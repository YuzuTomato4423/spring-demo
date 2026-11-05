# SPR-W6-D1-01～04

Eclipseの既存spring-demoでSPR-W6-D1-dev.launchを起動する。Java 17。作業ディレクトリはプロジェクト直下。
dev/testのどちらか一方をspr-w6-d1の後に指定する。Week5起動構成は従来のメモリDBのまま。

| 環境 | DB | DDL | 開発サンプル |
|---|---|---|---|
| dev | jdbc:h2:file:./data/spr-w6-d1;DB_CLOSE_ON_EXIT=FALSE | update | 社員が空の時のみ2件 |
| test | jdbc:h2:mem:sprw6d1-test-${random.uuid};DB_CLOSE_ON_EXIT=FALSE | create-drop | なし |

SPR-W6-D1-01: メモリDBはプロセス終了で失われるためfileへ変更。DB実体はdata/spr-w6-d1.mv.db。
H2 Consoleはhttp://localhost:8080/h2-console。JDBC URLはdevと同じ値、ユーザーsa、パスワード空（ローカル学習用）。
同じDBを複数のアプリから同時に開かない。テストには一時領域のfile DBまたは独立したmem DBを使用する。

SPR-W6-D1-02: SPR-W6-D1-HTTP-post-before.launchで実POSTの201とIDを確認。
devアプリを正常終了し、終了を確認して同じdev.launchを再起動。
SPR-W6-D1-HTTP-get-after.launchで同じID・氏名・部署のGET 200を照合。
照合値はdata/spr-w6-d1-restart-marker.json。再POSTは別IDになるため再起動確認前に実行し直さない。

SPR-W6-D1-03: SPR-W6-D1-test.launchは8081で起動し、UUID付きメモリDBを使用する。
テストを止めて起動し直すと空になる。devのファイルには接続しない。
application-dev.propertiesのWeek6設定は区切り文書のspr-w6-d1条件で限定し、過去のdev表示設定を維持する。

SPR-W6-D1-04: W6開発用_初期社員A/Bは本番データではない。
Week6のdevだけで部署を確保した後、社員表が空の場合だけ投入。既存社員があれば何も上書きしない。
開発中に社員を全削除すると次回起動で再投入される。test/prodにはサンプル社員を投入しない。
SPR-W6-D1-HTTP-initial-data.launchで初期社員2件をGETできる。

公開APIは既存の/employeesを継続利用し、URL・DTO・HTTP statusを変更しない。
自動テストWeek6Day1PersistenceTestsは一時領域で再起動・既存値保持・重複防止・test分離を確認する。
