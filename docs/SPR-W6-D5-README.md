# SPR-W6-D5-01～03 Week6統合確認

Java 17 / Spring Boot 4.1.1。既存API・業務ルールを再利用する。

## SPR-W6-D5-01 実行手順

1. 他の8080サーバーを停止してRefresh後、SPR-W6-D5.launchをRunする。
2. 起動ログの実接続先がjdbc:h2:file:./data/spr-w6-d5であることを確認する。DDLはupdate。
3. SPR-W6-D5-HTTP-prepare.launchをRunする。空のD5 DBだけへ社員3名を登録し、社員Aを更新する。
4. api-search、restart-beforeを順にRunする。氏名2件・部署2件・AND1件・該当なし0件を照合する。
5. サーバーを停止して同じSPR-W6-D5.launchを再起動し、restart-afterをRunする。同じID・値・DB行を照合する。
6. business-errors、transaction、db-stateをRunする。期待HTTP StatusとDBの不変・変更を照合する。

各シナリオは実HTTPを送信し、H2 Consoleの独立JDBC接続でSELECTを実行する。SQL結果HTML・実行ログ・再起動前スナップショットはtargetへ保存する。
prepareは社員が存在すると中断する。既存データを自動削除しない。再実施時はサーバー停止後にD5 DBとmarkerを別の場所へ退避してから開始する。
他のProfile・Day1 DBは維持する。通知は既存の模擬通知で、外部へ送信しない。

## SPR-W6-D5-02 自動テスト

- SPR-W6-D5-02-repository-service.launch: タグでDay4の4クラス・23件をまとめて実行。
- SPR-W6-D5-02-all-tests.launch: プロジェクト全体をJUnit 6で実行。
- CLI: mvnw.cmd -Dtest=Week6Day4*Tests test / mvnw.cmd package
- Week6Day5IntegrationTestsは@TempDir内のfile DBを使用。再起動保持、400/404/409とDB不変、一括異動commit/rollbackの3件。
- 既存テスト144件に統合3件を追加。最終件数は実行結果を参照する。

## SPR-W6-D5-03 中間説明（393字）

社員管理では、Week6Day3EmployeeControllerがHTTP要求と入力検証を受け持ち、EmployeeBusinessServiceへ処理を渡す。Serviceは部署の存在やメールの重複を判定し、トランザクション内でRepositoryを呼び出す。EmployeeJpaRepositoryはSpring Data JPAにより保存や氏名・部署の関連検索をDBへ委譲する。H2のファイルDBは社員と部署の関連を保持し、再起動後も同じIDと更新内容を読み出せる。一括異動は全件成功時だけ確定し、途中の例外では更新を戻す。通知は確定後だけ行う。Repositoryテストは保存と検索、Serviceテストは業務ルールと例外を確認する。統合テストでは実HTTPと独立したDB照会を使い、再起動、共通エラー応答、commitとrollbackまで検証し、各層の連携を保証する。

対応する課題IDコメントはController・Service・Repository・統合Testに記載。構成図はSPR-W6-D5-architecture.htmlをブラウザーで開く。
