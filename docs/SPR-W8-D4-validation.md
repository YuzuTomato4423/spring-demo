# SPR-W8-D4 検証記録

## 実施結果

- SPR-W8-D4-01：公開説明用に、認証・認可・保護対象・401／403・CSRFの説明をREADMEへ追加。
- SPR-W8-D4-02：第三者が再現できるよう、Security／JPAの起動方法・代表API・保存経路を分けて記載。構成図、既存のJPA画像、今回の検証結果を整理。
- SPR-W8-D4-03：不要／機密ファイルを点検し、Public化直前の公開候補commitを作成する。リポジトリの公開設定は変更しない。

## テストと実HTTP

2026-11-12 13:22:52 JST、Java 17、Maven Wrapperの `clean package` が成功。全173件（Security 7件を含む）、失敗0・エラー0・スキップ0。

[テスト結果抜粋](validation/w8d4-test-results.txt)は実際のMavenログの抜粋とSurefire XMLのケース名。[API結果](validation/w8d4-api-results.txt)は今回のパッケージをループバック・一時ポートで起動した実HTTPの結果。

| モード・条件 | 実測 |
| --- | --- |
| JPA 社員登録 | 201、LocationとJSON |
| JPA 登録社員取得 | 200、登録時と同じJSON |
| JPA 名前の入力不正 | 400、errors.name |
| Security 公開GET /hello | 200、Hello Spring! |
| Security 未認証GET /employees | 401 |
| Security USER／ADMINのGET /employees | それぞれ200、初期状態[] |
| Security Basicヘッダー付き・CSRF／セッションなしPOST | 401（CSRFがBasic認証より先に拒否） |

ロール別の更新401／403／200、ADMINのCSRF不足403、管理者だけの作成・削除などは `Week8Day3SecurityTests` で確認した。実HTTPの応答とMockMvcの条件を混同しない。

## スクリーンショット取得状況

README内の `employee-api.png` は既存のWeek7画像。今回の結果は上記のテキスト記録を参照する。

今回追加予定だったREADMEプレビュー・README／画像表示・GitHub差分の画面証跡は未取得。ユーザーの指示によりブラウザー操作は行わない。Computer Useの観測画像を証跡へ流用しない。

実装・テスト・GitHub APIによる差分確認と、画面証跡の提出状況は別に記録する。
