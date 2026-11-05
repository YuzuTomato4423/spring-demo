# SPR-W5-D3-01～05

EclipseでSPR-W5-D3.launchを実行する。Java 17、HTTP 8080。
Profileはspr-w5-d1,spr-w5-d2,spr-w5-d3を併用する。
H2 Console: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:sprw5d3 / user sa / password 空。
学習用のメモリDB・create-dropのため再起動で社員データは初期化される。
部署1=開発部、2=営業部は初期登録する。社員はPOSTで登録する。

GET /employees?name=田中 は氏名部分一致。
GET /employees?department=開発部 は部署名完全一致。
GET /employees?name=田中&department=開発部 は両条件のAND。
未指定・空白条件は無条件扱い、前後空白は除去、該当なしは200と[]。
JSONはid/name/departmentNameだけ。POST/PUT入力はname/departmentIdを維持する。
部署を別Entityへ分離し、社員→部署の単方向ManyToOneを設定した。
社員削除で部署を削除するCascadeは設定しない。
Query MethodとEntityGraphでDB検索と関連取得を行う。
公開DTOへの変換はService内のトランザクションで行い、open-in-view=falseを維持する。
EmployeeResponseのidはlong。JPAのLong IDをintへ切り詰めない。
過去D1/D2も同じEntityを使うため、部署関連追加後の構成へ発展している。
