# SPR-1006-01～05：Profileと遅延初期化

## 実習の構成と再実行

同じspring-demo内の `com.example.training.profilelazy` に専用実習を追加した。通常アプリのComponent Scan範囲は `com.example.demo` のまま。通知の契約は既存の `NotificationService.send(String)` を使用する。

Eclipseの **Run → Run Configurations → Java Application** から次の設定を選び、Runする。共通入口は `com.example.training.profilelazy.ProfileLazyDemoLauncher`。すべてUTF-8で出力する。

| 起動設定 | 引数 | VMのactive profile | 実行内容 |
|---|---|---|---|
| SPR-1006-01 | 01 | dev | 開発用通知Beanの選択とDI |
| SPR-1006-02 | 02 | prod | 本番想定の別通知Beanへの切替 |
| SPR-1006-03 | 03 | dev | @Lazyで起動時未生成、初回取得時に初期化 |
| SPR-1006-04 | 04 | dev | 別ContextでLazyなし→ありを一括比較 |
| SPR-1006-04-eager | 04 eager | dev | Lazyなしだけを実行し証跡を表示 |
| SPR-1006-04-lazy | 04 lazy | dev | Lazyありだけを実行し証跡を表示 |
| SPR-1006-05 | 05 | prod | 既存の社員管理Bean、通知、任意レポートを連携 |

VM引数の例は `-Dfile.encoding=UTF-8 -Dspring.profiles.active=dev`。active profileはdev/prodのどちらか1つに限定し、未指定、未対応値、同時指定を明確な例外で拒否する。01/03/04はdev、02/05はprodを期待する。これは本実習の再現条件であり、Spring自体が複数Profileを禁止するという意味ではない。

devは `DevNotificationService` が `DEV_CONSOLE` を返し、prodは `ProdNotificationService` が `PROD_MAIL` を返す。両方とも模擬通知。通知Beanが1つだけ存在し、利用側に注入された参照がContextのBeanと一致することを確認してからPASSを表示する。

## SPR-1006-03/04：初期化の比較

`HeavyEmployeeReport` の@PostConstructで約1秒の初期化を模擬する。Lazyあり・なしは同じクラス、同じ処理、同じ待機時間を使い、@Beanメソッドの@Lazy有無だけを変える。記録用BeanはContextごとに独立させる。

| 確認点 | @Lazyなし | @Lazyあり |
|---|---|---|
| 起動完了時 | 生成1回、初期化1回 | 生成0回、初期化0回 |
| ログ順序 | CONSTRUCTOR → INIT_BEGIN → INIT_DONE → STARTUP_COMPLETE | STARTUP_COMPLETE → FIRST_GET_REQUEST → CONSTRUCTOR → INIT_BEGIN → INIT_DONE |
| 初回取得後 | 生成1回、初期化1回 | 生成1回、初期化1回 |
| 再取得 | 同じsingleton、初期化は重複しない | 同じsingleton、初期化は重複しない |

2026-10-06のEclipse実行で、比較用設定の実測値は以下となった。

| 証跡対象 | 起動時間 | 初回取得時間 |
|---|---:|---:|
| SPR-1006-04-eager | 1507 ms | 0 ms |
| SPR-1006-04-lazy | 528 ms | 1010 ms |

時間は参考値。別JVMの起動、クラス読み込み、JIT、端末負荷も含むため、時間差を合否判定に使わない。合否は生成・初期化回数、ログ順序、参照の一致で決める。Lazyは初期化コストを消す仕組みではなく、初回利用へ移す仕組み。

未生成の観察には軽量な `InitializationProbe` と `containsSingleton("heavyEmployeeReport")` を使用する。検証のために重いBeanを取得したり、重いBeanの型一覧を取得して初期化を誘発したりしない。実際に使う段階で初めてgetBeanする。

Bean側だけの@Lazyは、eager singletonに直接注入すると起動時に生成される場合がある。今回は任意レポートを初回getBeanで取得する。実システムで依存を持たせる場合はObjectProviderや注入点の@Lazyを検討する。

## SPR-1006-05：社員管理での適用判断

最終実習は、既存のEmployeeController、EmployeeService、EmployeeMapper、InMemoryEmployeeRepositoryを専用Contextへ登録する。既存のコンストラクタDIで連携し、社員ID 1006を開発部へ登録してServiceから取得する。prod通知の結果を表示した後、必要になった時点で任意レポートを取得し、社員件数1、ID [1006]を出力する。通常の社員登録・通知までレポートが未生成であることも検証する。Contextは各実習の終了時にcloseする。

| 機能・状況 | 判断 | 理由 |
|---|---|---|
| 開発用確認通知と本番用通知 | Profileを使う | 環境によって異なる実装を、起動設定で選択できる |
| 環境に依存しない社員名検証・部署判定 | Profileを使わない | 共通の業務ルールを環境別に分岐すると差異と検証漏れを増やす |
| 頻度の低い、初期化の重いレポート | Lazyを検討する | 利用しない起動では準備コストを避けられる。初回の待ち時間を許容できる場合に適する |
| 基本の社員登録・検索、軽量なMapper | eagerを維持する | 日常的に使う処理であり、起動時に依存不備を検知したい |
| 必須の接続確認、初回応答時間が厳しい機能 | Lazyを避ける | 設定不備の検知が利用時まで遅れ、初回応答も遅くなる |

Profileを細かい業務条件に乱用したり、全Beanへ一律にLazyを設定したりしない。環境差と任意機能の準備コストに用途を限定する。

## 検証と証跡

変更前のMavenテストは16件成功。追加後は22件成功（失敗0、エラー0、スキップ0）。追加の6テストはProfile選択、不正Profile、Lazyの順序と生成回数、eagerの順序、社員管理との連携、dev/prodの通常アプリへ実習Beanが混入しないことを検証する。

再検証はプロジェクト直下で `mvnw.cmd -o test`。Java 17と既存のローカルMaven依存を使用する。

証跡はWindows標準のAlt+PrintScreenで対象ウィンドウ単体を取得し、Windowsクリップボードの画像を加工せずPNG保存する。Computer Useの観測画像は使用しない。保存後の画像を開き、ログの可読性、通常の色味、カーソル・クリック表示・操作オーバーレイがないことを確認する。

| 課題 | PNGファイル |
|---|---|
| SPR-1006-01 | 20261006_104442_SPR-1006-01_dev.png |
| SPR-1006-02 | 20261006_105114_SPR-1006-02_prod.png |
| SPR-1006-03 | 20261006_105233_SPR-1006-03_lazy.png |
| SPR-1006-04（なし） | 20261006_105351_SPR-1006-04_eager.png |
| SPR-1006-04（あり） | 20261006_105510_SPR-1006-04_lazy.png |
| SPR-1006-05 | 20261006_110845_SPR-1006-05_final.png |

保存先は `<学習作業フォルダ>\screenshots\G-1006-SPR`。6枚とも保存後に画像を開いて確認した。01～05と04の一括比較・個別比較の全7起動設定はEclipse上でPASSを確認済み。

## バックアップと復元

編集・実行前にプロジェクト全体119ファイルを `<学習作業フォルダ>\backups\20261006_102749_G-1006-SPR` へコピーし、全ファイルのSHA-256一致を確認した。未保存のエディターはなかった。バックアップ内の `manifest-sha256.json` に一覧とハッシュを記録している。

今回の変更は専用実習のJava 4ファイル、テスト1ファイル、起動設定7ファイル、このREADMEの追加。既存ソースと通常起動設定の書換えはない。

復元する場合は実習の実行を止め、今回追加した13ファイルを一覧と照合する。作業後に追加ファイルへ編集があれば先に退避する。追加ファイルを除去し、変更が見つかった既存ファイルのみバックアップのspring-demoから戻す。targetは再生成可能なビルド成果物なので、EclipseをRefreshして再ビルドし、Mavenテストと通常起動を確認する。ユーザーの追加作業や別のプロジェクトを一括削除しない。

## 公式資料

- [SpringのProfileとEnvironment](https://docs.spring.io/spring-framework/reference/core/beans/environment.html)
- [SpringのLazy初期化とeager依存の注意点](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-lazy-init.html)
