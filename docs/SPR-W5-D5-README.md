# SPR-W5-D5-01～03 統合スモークテスト

## 構成説明（396字）

Week5Day2EmployeeControllerはHTTPリクエストを受け、入力DTOを@Validで検証し、社員操作をEmployeeJpaServiceへ渡す。Serviceは業務ルールとトランザクション境界を担当し、社員・部署の存在を確認してEmployeeJpaRepositoryを呼び出す。JpaRepositoryの実装はSpring Data JPAが生成し、Hibernateを通じてH2の表へSQLを実行する。取得結果はトランザクション内でMapperが公開DTOへ変換し、ControllerがJSONと適切なHTTPステータスを返す。入力不正の400と社員未存在の404はEmployeeApiExceptionHandlerが共通形式へ変換する。H2はメモリDBとcreate-dropを使用するため、再起動で社員は消え、部署マスタは初期化処理で再生成される。

## 起動・再実行

EclipseでSPR-W5-D5.launchを起動する。通常のD1～D4とD5のProfileを併用し、rollback実験Profileは有効にしない。
HTTP: http://localhost:8080/employees / H2: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:sprw5d5 / user: sa / password: 空（学習用）

HTTPクライアントのlaunchを次の順で実行する。Consoleは実HTTPの内容を表示し、不一致なら例外で停止する。

1. create-read: 空の社員DBへ登録し、採番ID・Location・GET結果を照合。
2. update-search: 同一IDを更新し、氏名部分一致＋部署名完全一致で検索。
3. delete: 削除204、再GET 404、社員一覧が空であることを照合。
4. errors-prepare: 異常系で保持すべき社員を登録。H2で操作前の状態を撮影。
5. validation: 不正POST・既存社員の不正PUT・存在しない部署へのPUTが400。社員不変を照合。
6. not-found: 未存在IDのGET・PUT・DELETEが共通形式404。H2で操作後を撮影。
7. restart-before: 再起動確認社員を追加し、社員2件をAPIとH2で確認。
8. アプリを終了してSPR-W5-D5.launchを再実行。
9. restart-after: APIが空配列。H2で社員0件・部署2件を確認。
10. representative: 最終確認社員を登録し、代表APIとH2を表示して撮影。

## H2照合SQL

```sql
SELECT E.ID,E.NAME,E.DEPARTMENT_ID,D.NAME AS DEPARTMENT_NAME
FROM EMPLOYEE E JOIN DEPARTMENT D ON D.ID=E.DEPARTMENT_ID ORDER BY E.ID;
SELECT * FROM DEPARTMENT ORDER BY ID;
SELECT COUNT(*) AS EMPLOYEE_COUNT FROM EMPLOYEE;
```

## コードとの対応

- SPR-W5-D5-01/03: Week5Day2EmployeeController、EmployeeJpaService、EmployeeJpaRepository。
- SPR-W5-D5-02/03: EmployeeApiExceptionHandler。入力制約は既存EmployeeRequestを維持。
- SPR-W5-D5-03: EmployeeJpaMapper、application-spr-w5-d5.properties。
- 自動検証: Week5Day5SmokeTests（実HTTP＋独立JDBC）、Week5Day5RestartTests（実Context再起動）。

APIのURL、入力name/departmentId、出力id/name/departmentName、400/404のstatus/message/errorsは既存仕様のまま。
page/size/sortと検索の既存動作も変更しない。H2ファイル永続化への切替は行わない。
