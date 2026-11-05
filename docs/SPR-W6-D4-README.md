# SPR-W6-D4-01～04 Repository / Service自動テスト

Java 17 / Spring Boot 4.1.1。API・DTO・業務ルールは変更せず、既存のEmployeeJpaRepositoryとEmployeeBusinessServiceを検証する。

| 課題 | テストクラス | 件数 | 確認内容 |
|---|---|---:|---|
| SPR-W6-D4-01 | Week6Day4RepositoryTests | 4 | 保存再読取、未存在、更新、削除と部署維持 |
| SPR-W6-D4-02 | Week6Day4QueryTests | 12 | 氏名部分一致、部署完全一致、AND、該当なし。各条件を通常版とSort版で実行 |
| SPR-W6-D4-03 | Week6Day4ServiceNormalTests | 3 | 正規化と正常登録、本人メール維持更新、commit前後の通知 |
| SPR-W6-D4-04 | Week6Day4ServiceErrorTests | 4 | 登録・更新の重複/部署未存在の期待例外、DBと通知の不変 |

## 実行

EclipseでMaven Update Projectを行い、SPR-W6-D4-01-JUnit.launch～SPR-W6-D4-04-JUnit.launchをJUnitとして実行する。JUnit 6を選択する。

CLIはプロジェクトルートで`mvnw.cmd -Dtest=Week6Day4*Tests test`、全件確認は`mvnw.cmd package`。

Repositoryは@DataJpaTestで各テストをrollbackし、flush/clear後の実DB読取を検証する。テスト用部署はテスト自身が用意する。
ServiceはHTTPを起動せずSpringのTransaction Proxyを経由する。テスト全体に@Transactionalを付けず、Service復帰後にJDBCでcommit結果を読む。初期化前に実接続がjdbc:h2:mem:sprw6d4-で始まることを検証する。

## 検索データの意図

田中太郎(開発部)、田中花子(営業部)、佐藤花子(開発部)を用意する。「田中」は2名、「開発部」は2名、「田中 AND 開発部」は田中太郎1名。「開発」という部分部署名、「佐藤 AND 営業部」は0名。Sort版はID降順を検証する。

## 失敗→修正後成功の学習証跡

作業時だけ正常登録テストの氏名期待値を意図的に誤らせ、Assertionで1件失敗することを確認する。正しい期待値「正常登録」へ戻して再成功を記録する。業務コードは変更しない。最終ソースに誤った期待値を残さない。
