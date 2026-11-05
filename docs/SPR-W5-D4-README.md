# SPR-W5-D4-01～04

Java 17。EclipseでSPR-W5-D4.launchを実行する。ProfileはD1/D2/D3/D4を併用。
HTTP http://localhost:8080/employees / H2 Console http://localhost:8080/h2-console
JDBC jdbc:h2:mem:sprw5d4 / sa / password空。学習用メモリDBは再起動で初期化される。
部署1=開発部、2=営業部。SPR-W5-D4-HTTP-setup.launchで空DBへ社員4件を登録する。
setupは空DBのときだけ実行可能。putで最小IDの社員をDelta/営業部へ更新する。

SPR-W5-D4-01: 更新はEmployeeJpaService.updateの@Transactionalで一つの処理単位。
SPR-W5-D4-02: 実験にはSPR-W5-D4-rollback.launchを使用（通常起動から切替時は停止する）。
このProfileだけPOST /training/rollback/employees/{id}が有効。name/departmentIdのJSONを渡す。
通常のPUTに検証用フラグはない。別Beanで通常Service.update→flush→検証用RuntimeException。
HTTP-rollback.launchは最小IDの社員で実行。HTTP 500は予定した検証用例外。
トランザクションの外で例外を記録し再取得を比較する。H2 SELECTでも更新前と同じことを確認。
実験前後を撮影するときは同じ起動・DBのまま実行し、途中で再起動しない。
通常のSPR-W5-D4.launchには実験用Profileを含めず、検証用URLは404になる。

SPR-W5-D4-03: page/size未指定なら従来の配列。片方でも指定するとページDTO。
DTOはcontent/number/size/totalElements/totalPages/first/last。page=0、size=20が既定値。
GET /employees?page=0&size=2、page=1&size=2を比較する。検索と総件数は同じ条件。
範囲外ページは200と空content。pageは0以上、sizeは1以上の整数。不正値は共通形式400。
JPAの取得位置上限を超えるpage×sizeも400。検索の空白除去・LIKE特殊文字の扱いは従来通り。

SPR-W5-D4-04: sort=id,asc / id,desc / name,asc / name,desc。
sort=name,asc&sort=id,descのように複数指定可能。id/name以外、不正方向、同項目重複は400。
ID指定がなければid,ascを末尾に追加し、同名社員のページ順を安定化。既定はid,asc。
sortだけなら配列。RepositoryのPageable/SortでDB上で処理し、全件取得後の切出しはしない。
公開社員DTOはid/name/departmentName、入力はname/departmentId。OSIV=falseを維持。

H2確認SQL:
SELECT E.ID,E.NAME,E.DEPARTMENT_ID,D.NAME AS DEPARTMENT_NAME
FROM EMPLOYEE E JOIN DEPARTMENT D ON E.DEPARTMENT_ID=D.ID ORDER BY E.ID;
HTTPクライアントの各launchは実要求と応答をEclipse Consoleへ表示する。
