package com.example.demo.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.entity.DepartmentEntity;
import com.example.demo.entity.EmployeeEntity;
import com.example.demo.repository.DepartmentJpaRepository;
import com.example.demo.repository.EmployeeJpaRepository;

/**
 * SPR-W6-D1-04: 本番データと混同しない「W6開発用」サンプル。Week6のdevだけで投入する。
 * 外部キー参照先の部署を先に確保する。社員が空の場合だけ投入し、再起動で重複させず既存値を上書きしない。
 * 社員を全削除した開発DBでは次回起動時に再投入される。test/prodにはこのサンプルを投入しない。
 */
@Component
@Profile("spr-w6-d1 & dev & !test & !prod")
public class Week6Day1DevDataInitializer implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(Week6Day1DevDataInitializer.class);
    private final EmployeeJpaRepository employees;
    private final DepartmentJpaRepository departments;
    public Week6Day1DevDataInitializer(EmployeeJpaRepository employees, DepartmentJpaRepository departments) {
        this.employees=employees; this.departments=departments;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        for(var d:List.of(new DepartmentEntity(1,"開発部"),new DepartmentEntity(2,"営業部"))) {
            if(!departments.existsById(d.getId())) departments.save(d);
        }
        if(employees.count()==0) {
            employees.save(new EmployeeEntity("W6開発用_初期社員A",departments.findById(1).orElseThrow()));
            employees.save(new EmployeeEntity("W6開発用_初期社員B",departments.findById(2).orElseThrow()));
            log.info("[SPR-W6-D1-04] dev samples inserted: 2 (development only)");
        } else {
            log.info("[SPR-W6-D1-04] existing employees preserved; dev samples skipped");
        }
    }
}
