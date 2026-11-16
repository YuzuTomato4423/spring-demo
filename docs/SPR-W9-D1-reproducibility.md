# SPR-W9-D1-01～03 公開後の再現性確認

確認日：2026-11-16（JST）。対象：YuzuTomato4423/spring-demo、開始時main `d779ca4931edb43b68650613c6abfacb87888a5b`。

## SPR-W9-D1-01：Clean Clone

目的：現行IDEのローカル設定に依存せず、第三者がPublicのREADMEからclone/build/run/testを再現できることを確認する。

資格情報を使わないHTTPS cloneを別フォルダへ作成し、開始時のgit statusがcleanであることを確認した。既存プロジェクトのDB・IDE設定・未コミット起動設定はコピーしていない。JDK 17.0.15、PowerShell 7、Maven Wrapper 3.9.16を使用。Mavenの通常キャッシュは使用し、空の依存キャッシュでの検証ではない。

`mvnw.cmd -B clean package` はBUILD SUCCESS。42スイート、173件、失敗0・エラー0・スキップ0。完了：2026-11-16T14:11:46+09:00。`mvnw.cmd -Dtest=Week8Day3SecurityTests test` も7件成功した。[今回の実ログ抜粋](validation/w9d1-test-results.txt)を参照。

READMEのMaven起動コマンドで各モードを別々に127.0.0.1:8080で起動し、PowerShell 7のHTTP例を実行した。Security用の2つの異なるパスワードは実行時生成の一時値をプロセス環境に渡し、記録・公開・撮影していない。

| モード | 確認 | 結果 |
| --- | --- | --- |
| JPA | 社員登録・Location | 201 |
| JPA | 登録社員の取得・内容一致 | 200 |
| JPA | 空の名前・errors.name | 400 |
| Security | 公開GET /hello | 200、Hello Spring! |
| Security | 未認証GET /employees | 401 |
| Security | USER、ADMINのGET /employees | 200、[] |

[実HTTP結果](validation/w9d1-api-results.txt)を参照。各検証後、今回起動したサーバーを停止した。

## SPR-W9-D1-02：公開後監査

Public化後の監査として、README手順、相対リンク、PNG/SVG画像、sample設定、Git追跡ファイル、除外設定と全公開履歴を再確認した。

- 開始時の公開対象は318ファイル、8コミット、329種類のblob。README相対リンク切れ0件。
- 秘密鍵・既知トークン形式・資格情報付きURL・個人端末パスの検出0件。DB・ログ・ローカル秘密設定等の履歴混入0件。
- 資格情報に関する候補29件を確認。環境変数・実行時生成・学習用の空H2設定等で、実パスワードの埋込みなし。
- 公開用PNGは架空の社員API結果。2つのSVGはJPAとSecurityの構成図。メールはexample.testを使用。
- target、IDE設定、ログ、DB、.env、秘密設定・鍵の10例についてgitignoreが機能した。
- sample設定はループバック・8080・JPAプロファイルで秘密値なし。適用コマンドはREADMEへ補足した。

これは上記の検査範囲で問題を発見しなかったという結果であり、未知の秘密情報まで自動検査で保証するものではない。

## SPR-W9-D1-03：再現性改善

API・ビルド・テストの不具合は再現しなかった。手順をたどる際、Javaの実行環境確認と停止方法がまとまっておらず、設定例のリンク先をどう起動に適用するかはファイル内のコメントから組み立てる必要があったため、READMEを軽微修正した。

| 修正前 | 修正後・理由 |
| --- | --- |
| JDK 17/PowerShell 7の前提のみ | java、PowerShell、Wrapperのバージョン確認、Java 17不一致時の確認先、build/test→モード選択→停止の順序を明記 |
| 設定例はリンクとファイル内コメントのみ | ルートからadditional-locationで適用するコマンドを追加し、JPA専用であることを明記 |
| 過去Week8の検証記録のみ | 今回のclone・全テスト・HTTP結果と修正理由への導線を追加 |

実行コード・依存関係・API仕様・sample設定の値は変更しない。今回の文書だけを通常commit/pushする。開始時の既存JUnit起動設定12件は保持し、commitへ含めない。

追加コマンドは別cloneへ今回の文書だけを適用して再検証し、WrapperのJava 17確認と設定例による起動・JPA 201/200/400を確認した。公開後はmainのSHAと文書の内容を照合する。

証跡画像はWindows標準Alt+Print Screenによる未加工PNGで、ローカルの課題別エビデンスフォルダに保存する。Computer Useの観測画像は使用しない。
