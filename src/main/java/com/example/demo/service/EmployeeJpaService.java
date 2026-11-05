package com.example.demo.service;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.entity.DepartmentEntity;
import com.example.demo.entity.EmployeeEntity;
import com.example.demo.exception.EmployeeNotFoundException;
import com.example.demo.mapper.EmployeeJpaMapper;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.repository.DepartmentJpaRepository;
import com.example.demo.repository.EmployeeJpaRepository;

/**
 * SPR-W5-D5-01/03: 業務ルールとTransaction境界を所有し、Repositoryの結果を境界内でDTOへ変換する。
 * SPR-W5-D5-02: 社員未存在はEmployeeNotFoundException、部署不正はIllegalArgumentExceptionで表し、
 * HTTPへの変換はAdviceに任せる。検証失敗を成功扱いにせず、書込みTransactionをrollbackする。
 */
@Service
@Profile("spr-w5-d2")
@Transactional(readOnly = true)
public class EmployeeJpaService {
    private final EmployeeJpaRepository repository;
    private final DepartmentJpaRepository departments;
    private final EmployeeJpaMapper mapper;
    private final EmployeeRegistrationNotifier notifier;
    public EmployeeJpaService(EmployeeJpaRepository repository, DepartmentJpaRepository departments,
            EmployeeJpaMapper mapper, EmployeeRegistrationNotifier notifier) {
        this.repository=repository; this.departments=departments; this.mapper=mapper; this.notifier=notifier;
    }
    /** SPR-W5-D2-02 / SPR-W5-D3-04/05: 検索はRepositoryへ委譲し、全件のJava絞り込みを行わない。 */
    public List<EmployeeResponse> findAll(String name, String department) {
        return findAll(name,department,Sort.by("id"));
    }
    public List<EmployeeResponse> findAll(String name, String department, Sort sort) {
        String n=normalize(name), d=normalize(department);
        List<EmployeeEntity> result;
        if (n!=null && d!=null) result=repository.findByNameContainingAndDepartment_Name(n,d,sort);
        else if (n!=null) result=repository.findByNameContaining(n,sort);
        else if (d!=null) result=repository.findByDepartment_Name(d,sort);
        else result=repository.findAll(sort);
        // SPR-W5-D3-03: 関連を読めるトランザクション内で公開DTOへ変換する。
        return result.stream().map(mapper::toResponse).toList();
    }
    /** SPR-W5-D4-03: Pageableで検索・件数・並べ替えをDBへ渡し、全件取得後の切出しを避ける。 */
    public Page<EmployeeResponse> findPage(String name, String department, Pageable pageable) {
        String n=normalize(name), d=normalize(department);
        Page<EmployeeEntity> result;
        if(n!=null && d!=null) result=repository.findByNameContainingAndDepartment_Name(n,d,pageable);
        else if(n!=null) result=repository.findByNameContaining(n,pageable);
        else if(d!=null) result=repository.findByDepartment_Name(d,pageable);
        else result=repository.findAll(pageable);
        return result.map(mapper::toResponse);
    }
    public EmployeeResponse findById(long id) { return mapper.toResponse(requireEmployee(id)); }
    private EmployeeEntity requireEmployee(long id) {
        return repository.findById(id).orElseThrow(() ->
                new EmployeeNotFoundException("社員が見つかりません。 id="+id));
    }
    /** SPR-W5-D2-03: save後の採番IDを返し、コミットが確定した場合だけ模擬通知する。 */
    @Transactional public EmployeeResponse create(String name, int departmentId) {
        var saved=repository.save(new EmployeeEntity(name,validate(name,departmentId)));
        long id=saved.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { notifier.notifyRegistration(id); }
        });
        return mapper.toResponse(saved);
    }
    /**
     * SPR-W5-D4-01: 社員取得・入力確認・更新を一つの処理単位にするためServiceをTransaction境界に置く。
     * ControllerやRepository単独では複数操作をまとめられない。例外時はこの単位をrollbackする。
     */
    @Transactional public EmployeeResponse update(long id, String name, int departmentId) {
        var entity=requireEmployee(id);
        entity.update(name,validate(name,departmentId));
        return mapper.toResponse(repository.save(entity));
    }
    /** SPR-W5-D2-04: 未存在なら404。社員削除で共有部署は削除しない。 */
    @Transactional public void delete(long id) {
        requireEmployee(id); repository.deleteById(id);
    }
    private DepartmentEntity validate(String name, int departmentId) {
        if (name==null || name.isBlank()) throw new IllegalArgumentException("社員名を入力してください。");
        return departments.findById(departmentId).orElseThrow(() ->
                new IllegalArgumentException("存在しない部署です。"));
    }
    private static String normalize(String value) {
        return value==null || value.strip().isEmpty() ? null : value.strip();
    }
}
