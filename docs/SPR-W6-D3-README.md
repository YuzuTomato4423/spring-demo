# SPR-W6-D3-01～04：Service業務ルールとTransaction

このプロジェクトの`spr-w6-d3` Profileで実行する。過去課題のProfileでは従来の社員APIを使用する。
Day3は毎起動独立するH2メモリDBで、既存の`data/spr-w6-d1.mv.db`へ接続しない。

## 起動と再実行

1. Eclipseで他の8080サーバーを停止し、プロジェクトをRefreshする。
2. `SPR-W6-D3.launch`をRunする。部署1=開発部、2=営業部を用意する。社員は初期登録しない。
3. `SPR-W6-D3-HTTP-*.launch`を次の順にRunする。

| 実行名 | 実際の処理と期待結果 |
|---|---|
| register | メールの空白・大小文字を正規化して社員Aを登録。201とLocation。 |
| duplicate | 同じメールを拒否。409。 |
| department | 社員Aを営業部へ更新200。未存在部署でのPOST/PUTは404。 |
| prepare | 社員Bを営業部へ登録201。 |
| db-before | H2 Consoleの独立JDBC接続でSELECT。両社員の部署ID=2。 |
| transfer | 両社員を開発部へ一括異動200。 |
| db-after | 同じSELECTで両社員の部署ID=1を確認。 |
| rollback | 社員Aを部署2へUPDATE/flush後、後続の未存在社員で404。同じSELECTで両社員が部署1のままであることを確認。 |
| errors | メール形式不正400、重複409、社員未存在404の共通形式を表示。 |

再実行はサーバーを停止・起動し、空の専用DBから上記の順で行う。クライアントは要求・応答を捏造せず、実HTTP結果を表示し、想定Statusと共通エラー契約を検証する。
サーバーログ`target/SPR-W6-D3-server.log`のJDBC URLを使用してH2 Consoleへ接続する。
DB確認クライアントのSQLはSELECTのみ。H2から返された結果表の内容を表示し、HTML原本もtargetへ残す。

## API

- `POST /employees`：`{"name":"社員A","email":"alice@example.test","departmentId":1}` → 201。
- `PUT /employees/{id}`：同じ入力契約 → 200。
- `GET /employees/{id}`：Day3用の`id/name/email/departmentName` DTO。
- `POST /employees/transfers`：`{"employeeIds":[1,2],"departmentId":2}` → `updatedCount/employees`。
- `GET /employees`：従来の一覧・検索・Sort・ページングを再利用。従来どおり`id/name/departmentName`を返す。
- `DELETE /employees/{id}`：既存Serviceを再利用。204、未存在404。

メールは前後空白を除去し、Locale.ROOTで小文字化してから必須・形式・254文字以内を検証する。
社員名の重複は許可する。更新時は本人のメールを一意性判定から除外する。
DBの`UK_EMPLOYEE_EMAIL`制約で同時登録も防ぎ、この制約による競合だけを409へ変換する。
旧課題の既存社員を維持するためEMAIL列はNULL可。旧Profileの入力契約をメール必須にはしない。

## ServiceとHTTPの役割

- D3-01：ServiceがRepositoryでメール重複を確認。DB制約は競合への最終防御。通知はafterCommit。
- D3-02：ServiceがDepartment Repositoryで存在を確認し、専用業務例外を送出。
- D3-03：一括異動全体が公開Serviceメソッド1つのTransaction。SQL発行後でも後続のRuntimeExceptionを外へ伝えて全件rollbackする。異動では氏名・メールを変更しない。
- D3-04：Adviceが`status/message/errors`へ変換。入力不正400、保存状態との競合409、参照先未存在404。DBの別障害をメール重複として隠さない。

## 検証

`Week6Day3BusinessTests`は実HTTP完了後に独立JDBCでcommit/rollback状態を確認する14件のテスト。
テスト全体をTransactionで囲まず、同時登録・直接Service呼出し・DB一意制約・通知・旧API再利用も検証する。
`Week5Day1JpaTests`のDDL期待値には新しいEMAIL列とNULL許容の検証を追加している。
既存テストを含むMaven packageと、現行Eclipseプロジェクトからの実行の両方を確認する。
