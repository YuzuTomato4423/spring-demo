# SPR-W5-D2-01～04

Java 17 / Spring Boot 4.1.1。SPR-W5-D2.launchをEclipseのJava Applicationとして起動する。
Profileはspr-w5-d1,spr-w5-d2、ポート8080。carry-demoはD2では無効で、初期社員は0名。

- EmployeeJpaRepositoryはJpaRepository<EmployeeEntity, Long>を継承する。
- D2の社員APIはWeek5Day2EmployeeController → EmployeeJpaService → EmployeeJpaRepository → H2。
- 従来の起動設定では過去課題のインメモリ社員APIを再実行できる。
- GET一覧・詳細、POST、PUT、DELETEはD2起動時に同じEMPLOYEE表を使用する。
- 入力はEmployeeRequest(name, departmentId)、出力はEmployeeJpaResponse(id, name, departmentName)。IDはLong。
- 部署1=開発部、2=営業部。名前・部署の既存検索条件と400/404共通エラー形式を維持する。
- POSTは201、Location、採番IDのDTOを返す。登録通知はコミット後の模擬Console通知。
- DELETEはトランザクション内で存在を確認し、deleteByIdを実行。成功204、未存在404。

## 実HTTPとH2の確認

1. SPR-W5-D2-HTTP-post.launchを実行し、応答のIDを記録する。確認社員は削除せず残す。
2. list、detailの各launchで200と登録済みデータを確認する。
3. H2 Console (http://localhost:8080/h2-console) へ接続する。
   JDBC URL=jdbc:h2:mem:sprw5d2、user=sa、password=空。
4. SELECT ID, NAME, DEPARTMENT_ID FROM EMPLOYEE ORDER BY ID; でAPIとの一致を確認する。
5. delete、missingの各launchで204と404を確認する。
   SELECT COUNT(*) AS REMAINING_ROWS FROM EMPLOYEE WHERE ID=1; で0件を確認する。

detail/delete/missingのlaunch初期値はID=1。再実行で別IDが採番された場合は、Run ConfigurationsのArgumentsを実IDに変更する。
H2はインメモリかつcreate-drop。プロセスの終了・再起動でデータは初期化され、永続ファイルは作成しない。

## テスト

Java 17でmvnw.cmd test。Week5Day2JpaHttpTestsはランダム名H2・ランダムHTTPポートで分離する。
実HTTPのCRUDとJDBC、400/404、検索、int上限超のID、ロールバック時の通知抑止を検証する。
既存の過去課題テストもあわせて実行する。
