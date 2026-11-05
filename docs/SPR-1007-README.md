# SPR-1007-01～04

Java 17 / Spring Boot 4.1.1。Spring ToolsのSPR-1007-01～04.launchをJava Applicationとして実行する。
通常起動のComponent Scan対象外であるcom.example.training.beanpostprocessorに実習を隔離した。

01は初期化前後に全Beanを観測し、02は社員関連の型だけを観測する。
既存EmployeeController/EmployeeService/EmployeeMapper/EmployeeRepositoryと、
EmployeeFormatter/FormatterHolderも対象。inMemoryEmployeeRepositoryを名前のprefixで漏らさない。
HelloControllerは比較用の対象外Bean。DTO/modelはSpring管理Beanとして登録しない。
ログ観測は業務コードへ個別に埋め込まず、BeanPostProcessorの共通処理として実行する。

03はsrc/main/resources/spr1007-auto-configuration.txtの概念メモを表示する。
課題がメモ中心なのでCondition Evaluation Reportは必須としない。

04はsrc/main/resources/spr1007-lifecycle-diagram.txtの図と実ログを照合する。
EmployeeLifecycleProbeが生成/DI・@PostConstruct・afterPropertiesSet・利用・@PreDestroyを実測し、
独自BPPがその前後を観測する。callbackを手動呼び出ししてログを模擬する構成ではない。
独自BPPのPriorityOrdered.HIGHEST_PRECEDENCEにより@PostConstruct処理より先にbeforeを実行する。
@PostConstructはCommonAnnotationBeanPostProcessorのbeforeチェーン中の処理なので、
独自BPPの順序次第で@PostConstructとの前後関係は変わる。

公式参照:
- https://docs.spring.io/spring-framework/reference/core/beans/factory-extension.html
- https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html
- https://docs.spring.io/spring-boot/reference/using/auto-configuration.html
- https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html

検証: Maven test/verifyとEclipse実行。Beanの初期化前後、型による対象限定、
社員登録/DI、singleton再取得、Lifecycle順序、Context.close、通常起動への非干渉を確認する。
