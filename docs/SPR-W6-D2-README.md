# SPR-W6-D2-01～05：OneToMany・Fetch・N+1・DTO・fetch join

この課題は現行spring-demoで動作する。通常の完成設定はLAZY。依存ライブラリの追加はない。

## Eclipseから再実行

1. プロジェクトをRefreshし、ビルド完了を待つ。
2. ポート8080の既存アプリを正常終了する。
3. `SPR-W6-D2-lazy.launch`をJava Applicationとして起動する。
4. `SPR-W6-D2-HTTP-plain.launch`を起動する。実HTTPの200・JSON・SQL計数ヘッダーをクライアントConsoleで確認し、サーバーConsoleでSQLと取得タイミングを確認する。
5. `SPR-W6-D2-HTTP-join.launch`を起動し、同じJSONが1 SELECTで返ることを確認する。
6. サーバーを終了し、`SPR-W6-D2-eager.launch`で再起動する。同じplain APIを実行し、取得直後に関連が初期化済みであることを確認する。
7. 終了後はLAZY起動へ戻す。

作業ディレクトリは`${workspace_loc:/spring-demo}`。専用Profile `spr-w6-d2`は既存JPA/APIを再利用するProfileと、最後に専用設定を適用する`spr-w6-d2-config`を有効化する。EAGER比較では末尾に`spr-w6-d2-eager`を追加し、`mappings/spr-w6-d2-eager.xml`だけで取得方式を上書きする。

## データとAPI

専用H2メモリDBのURLは起動ごとに異なる`jdbc:h2:mem:sprw6d2-<UUID>;DB_CLOSE_ON_EXIT=FALSE`。Day1のfile DBと接続しない。部署3件、社員4件（開発部2・営業部2・社員なし部署0）を初期化し、終了時に破棄する。

- `GET /training/departments`または`?fetch=plain`：部署を通常取得してから社員へアクセス。
- `GET /training/departments?fetch=join`：LEFT JOIN FETCHで社員なしの部署も含め一括取得。
- `fetch`のそれ以外の値は400。
- 既存の`/employees` CRUD・検索・ページングAPIは継続利用可能。

部署DTOは`id/name/employees`、社員DTOは`id/name`のみ。ID順で返す。Entityを直接JSON化せず、ServiceのreadOnly Transaction内でDTO化を完了する。OSIVはfalse。

## SQLログの読み方

| モード | 部署取得直後 | DTO変換完了 | 関連取得タイミング |
|---|---:|---:|---|
| LAZY / plain | 1 SELECT、employeesLoaded=false | 4 SELECT | 部署ごとの社員アクセスで追加3回 |
| EAGER / plain | 4 SELECT、employeesLoaded=true | 4 SELECT | 部署取得完了までに関連取得 |
| LAZYまたはEAGER / join | 1 SELECT、employeesLoaded=true | 1 SELECT | 部署と社員を一括取得 |

`RelationSqlTrace`は学習APIの現在スレッド・処理区間だけを数え、起動SQLや別リクエストを除外する。リクエストごとに新しい永続化コンテキストを使い、2次キャッシュ・クエリキャッシュ・バッチ取得を無効にして比較する。

EAGERは取得時期の指定であり、JOINや1 SQLを保証する設定ではない。通常は必要時だけ読むLAZYを選び、部署と社員の一覧が必要なこのAPIではfetch joinで取得する。コレクションfetch joinにページングは適用しない。

## 関連と確認

`DEPARTMENT(ID)` ← `EMPLOYEE(DEPARTMENT_ID)`。社員の`department`が所有側で、部署の`employees`は`mappedBy="department"`の逆側。中間表やcascade削除は追加しない。

外部キーは`FK_EMPLOYEE_DEPARTMENT`。H2 Consoleを使う場合は起動ログに出た現在の専用メモリDBのJDBC URLに接続する。`INFORMATION_SCHEMA.TABLE_CONSTRAINTS`、`KEY_COLUMN_USAGE`、`REFERENTIAL_CONSTRAINTS`で所有側と参照先を確認できる。

`Week6Day2LazyTests`と`Week6Day2EagerTests`は、実HTTPのSQL本数・JSON一致・空部署・双方向関連・外部キー・既存CRUDを検証する。既存97件と合わせて実行する。

証跡はWindows標準Alt+Print Screenのクリップボード画像を無加工PNG保存する。Computer Useの観測画像は使用しない。
