package com.example.demo.training.relations;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.entity.DepartmentEntity;
import com.example.demo.entity.EmployeeEntity;
import com.example.demo.repository.DepartmentJpaRepository;
import com.example.demo.repository.EmployeeJpaRepository;

/** SPR-W6-D2-02/03: 専用メモリDBの開発用サンプル。既存のDay1 DB・初期化処理は使わない。 */
@Component
@Profile("spr-w6-d2")
public class RelationSampleInitializer implements ApplicationRunner {
    private final DepartmentJpaRepository departments;
    private final EmployeeJpaRepository employees;
    public RelationSampleInitializer(DepartmentJpaRepository departments,EmployeeJpaRepository employees) {
        this.departments=departments; this.employees=employees;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if (employees.count()!=0) return;
        var development=departments.findById(1).orElseGet(()->departments.save(new DepartmentEntity(1,"開発部")));
        var sales=departments.findById(2).orElseGet(()->departments.save(new DepartmentEntity(2,"営業部")));
        if (!departments.existsById(3)) departments.save(new DepartmentEntity(3,"W6社員なし部署"));
        employees.save(new EmployeeEntity("W6比較社員A",development));
        employees.save(new EmployeeEntity("W6比較社員B",development));
        employees.save(new EmployeeEntity("W6比較社員C",sales));
        employees.save(new EmployeeEntity("W6比較社員D",sales));
    }
}
