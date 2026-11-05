package com.example.demo.training.relations;

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.entity.DepartmentEntity;
import com.example.demo.entity.EmployeeEntity;
import com.example.demo.repository.DepartmentJpaRepository;
import jakarta.persistence.EntityManagerFactory;

@Service
@Profile("spr-w6-d2")
public class DepartmentRelationService {
    private static final Logger log=LoggerFactory.getLogger(DepartmentRelationService.class);
    private final DepartmentJpaRepository repository;
    private final EntityManagerFactory factory;
    private final String fetchMode;
    public DepartmentRelationService(DepartmentJpaRepository repository, EntityManagerFactory factory,
            @Value("${training.relations.fetch-mode}") String fetchMode) {
        this.repository=repository; this.factory=factory; this.fetchMode=fetchMode;
    }
    public record Observation(List<DepartmentResponse> body, int afterLoad, int total, List<Boolean> loaded) { }

    @Transactional(readOnly=true)
    public Observation findAll(boolean join) {
        // SPR-W6-D2-02: 同一API/データで取得直後とDTO変換後を分け、SQLの発行時期を比較する。
        try (var trace=RelationSqlTrace.begin()) {
            log.info("[SPR-W6-D2-02] BEGIN mode={} strategy={}",fetchMode,join?"join":"plain");
            List<DepartmentEntity> rows=join?repository.findAllWithEmployees():repository.findAll(Sort.by("id"));
            int afterLoad=trace.count();
            // isLoadedはコレクションへアクセスせず、取得済みかだけを確認する。
            var loaded=rows.stream().map(d->factory.getPersistenceUnitUtil().isLoaded(d,"employees")).toList();
            log.info("[SPR-W6-D2-02] AFTER_LOAD departments={} SELECT={} employeesLoaded={}",rows.size(),afterLoad,loaded);
            // SPR-W6-D2-03: 部署ごとの関連アクセスで増えるSQLを観察し、N+1の発生理由を学ぶ。
            var body=rows.stream().map(d->toResponse(d,trace)).toList();
            log.info("[SPR-W6-D2-{}] AFTER_DTO SELECT={} (load={}, relation={})",join?"05":"03",trace.count(),afterLoad,trace.count()-afterLoad);
            return new Observation(body,afterLoad,trace.count(),loaded);
        }
    }
    private DepartmentResponse toResponse(DepartmentEntity department, RelationSqlTrace.Scope trace) {
        // SPR-W6-D2-04: OSIVなしでも安全に返せるようTransaction内でDTO化を完了する。
        var employees=department.getEmployees().stream().sorted(Comparator.comparing(EmployeeEntity::getId))
            .map(e->new DepartmentResponse.EmployeeItem(e.getId(),e.getName())).toList();
        log.info("[SPR-W6-D2-03] ACCESS department={} employees={} SELECT={}",department.getId(),employees.size(),trace.count());
        return new DepartmentResponse(department.getId(),department.getName(),employees);
    }
}
