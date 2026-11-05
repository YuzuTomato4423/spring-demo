# SPR-1005-05：通知Beanの選択方法

| 方法 | 用途 | 記述例 | 注意点 |
|---|---|---|---|
| `@Qualifier` | 利用側がMail等の用途を明示し、型が一致する候補を絞る | constructor引数に `@Qualifier("mailNotificationService") NotificationService notificationService` | 候補を絞った後も一意性が必要。Bean名をQualifier値として使えるが、意味は型候補の絞り込み |
| `@Primary` | 特別な指定のない利用側に共通の既定実装を提供 | Slackの `@Bean` メソッドに `@Primary` | 単一値の注入向け。複数のPrimaryがあれば競合し得る。List等から他のBeanを除外するものではない |
| Bean名指定 | 特定の登録名を持つBeanを直接参照する | setterに `@Resource(name="mailNotificationService")` | 名前への依存が強い。Resourceはconstructor引数には使えない。引数名だけの暗黙的選択とは区別する |

この実習ではSlackを既定にし、Mailが必要な利用側にQualifierを付ける。QualifierによってMailへ候補が絞られるため、SlackのPrimaryがあってもMailが選ばれる。

Mailは個別宛先への案内、Slackはチームへの即時共有を想定する。両実装は模擬結果を返し、実際のメール送信やSlack接続は行わない。業務の利用側はSpringから注入された参照を使い、実装を手動で選ばない。

## Eclipseから再実行

Run Configurations → Java Applicationから `SPR-1005-01` ～ `SPR-1005-05` を選択してRunする。共通入口は `com.example.training.notification.NotificationDemoLauncher`、引数は課題番号 `01` ～ `05`。

01は実際のNoUniqueBeanDefinitionExceptionを期待結果として検証してPASSを表示する。02はMail、03はSlack、04はSlackにPrimaryがある状態でMail、05は既定Slack・明示Mail・名前指定Mailを表示する。各Contextは終了時にcloseする。

通常アプリは `com.example.demo` 配下を走査する。実習は走査範囲外の `com.example.training.notification` に配置し、専用入口が必要な設定と利用側だけを明示登録する。

## 公式資料

- [Qualifier：型候補の絞り込み、Bean名、Resourceとの違い](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html)
- [Primary：単一値注入の既定候補](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-primary.html)
