# SPR-W7-D2-01～04 統合テスト

既存のspring-demoに、実HTTP→Controller→Validation/Advice→Service→Repository→H2を通すテストを追加する。公開API・DTO・既存Profileの変更はない。

| 課題 | テスト | 確認内容 |
| --- | --- | --- |
| SPR-W7-D2-01 | Week7Day2ContextTests | Context、実Bean、Transaction Proxy、GET 200 |
| SPR-W7-D2-02 | Week7Day2ProfileTests | test Profile、実JDBC接続先、create-drop、file DBとの分離 |
| SPR-W7-D2-03 | Week7Day2IsolationTests | 同じメールの登録を2回繰り返し、各回0→1→0件 |
| SPR-W7-D2-04 | Week7Day2RestDbTests | POST201→GET200→PUT200→DELETE204→GET404とcommit後のDB照合 |

## 実行

Spring ToolsでプロジェクトをRefresh後、SPR-W7-D2-01～04-JUnit.launchをRun As > JUnit Testで個別実行できる。SPR-W7-D2-all-JUnit.launchはspr-w7-d2タグの全5件を実行する。

Maven: `mvnw.cmd -o "-Dtest=Week7Day2*Tests" test`。既存テストを含む確認: `mvnw.cmd -o test`。

## DBと初期化

有効Profileはspr-w6-d3,test,spr-w7-d2-test。最後の専用Profileはsrc/test/resourcesに置き、過去課題のapplication-test.propertiesは保持する。各ContextはUUID付きH2メモリDBを使用し、実JDBC URLのprefixをDB操作前に検証する。開発用data/*.mv.dbへ接続しない。

実HTTPのService Transactionはテストとは別スレッドでcommitする。このためテストに@Transactionalを付けず、BeforeEach/AfterEachの社員DELETEと独立JDBC参照で分離する。部署初期データ2件は保持する。主キーの連番リセットには依存しない。JUnitの既定どおり並列実行は使用しない。

## 証跡

JUnit成功画面とConsoleの実ログをWindows標準のAlt+Print Screenで撮影する。Computer Use観測画像は証跡に転用しない。PNGは指定のscreenshots/G-W7-D2-SPR配下に保存する。
