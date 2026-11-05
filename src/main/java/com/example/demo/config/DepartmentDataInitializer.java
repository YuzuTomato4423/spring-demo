package com.example.demo.config;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.entity.DepartmentEntity;
import com.example.demo.repository.DepartmentJpaRepository;

/** SPR-W5-D3-01: 課題用の共有部署を登録する。社員はAPIで登録し、初期登録しない。 */
@Component
@Profile({"spr-w5-d1", "spr-w5-d2"})
public class DepartmentDataInitializer implements ApplicationRunner {
    private final DepartmentJpaRepository repository;
    public DepartmentDataInitializer(DepartmentJpaRepository repository) { this.repository=repository; }
    @Override @Transactional public void run(ApplicationArguments args) {
        for (var d : List.of(new DepartmentEntity(1,"開発部"),new DepartmentEntity(2,"営業部"))) {
            if (!repository.existsById(d.getId())) repository.save(d);
        }
    }
}
