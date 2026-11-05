package com.example.demo.service;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.exception.*;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.repository.*;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

/**
 * SPR-W6-D5-01/SPR-W6-D5-03: ControllerからTransaction Proxyを経由し、部署存在・メール一意性を検証する。
 * Repositoryで保存し、commit後だけ模擬通知する。例外はAdviceへ伝わり、HTTPとDBの整合性を保つ。
 */
@Service
@Profile("spr-w6-d3")
@Transactional(readOnly=true)
public class EmployeeBusinessService {
    private static final Logger log=LoggerFactory.getLogger(EmployeeBusinessService.class);
    private final EmployeeJpaRepository employees;
    private final DepartmentJpaRepository departments;
    private final EmployeeRegistrationNotifier notifier;
    private final Validator validator;
    public EmployeeBusinessService(EmployeeJpaRepository employees,DepartmentJpaRepository departments,
            EmployeeRegistrationNotifier notifier,Validator validator) {
        this.employees=employees;this.departments=departments;this.notifier=notifier;this.validator=validator;
    }
    public BusinessEmployeeResponse findById(long id) { return BusinessEmployeeResponse.from(requireEmployee(id)); }

    // SPR-W6-D3-01: 一意性はHTTP以外の呼出しでも守る業務ルールなのでServiceに置く。
    // 事前照会に加え、DB制約で同時登録を防ぐ。自身のメールを維持する更新は許可する。
    @Transactional
    public BusinessEmployeeResponse create(BusinessEmployeeRequest request) {
        validate(request);
        var department=requireDepartment(request.departmentId());
        if(employees.existsByEmail(request.email())) throw new DuplicateEmployeeEmailException();
        var saved=save(new EmployeeEntity(request.name(),request.email(),department));
        long id=saved.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { notifier.notifyRegistration(id); }
        });
        return BusinessEmployeeResponse.from(saved);
    }
    @Transactional
    public BusinessEmployeeResponse update(long id,BusinessEmployeeRequest request) {
        validate(request);
        var employee=requireEmployee(id);
        var department=requireDepartment(request.departmentId());
        if(employees.existsByEmailAndIdNot(request.email(),id)) throw new DuplicateEmployeeEmailException();
        employee.update(request.name(),request.email(),department);
        return BusinessEmployeeResponse.from(save(employee));
    }

    // SPR-W6-D3-03: 複数UPDATE全体を1つのTransaction境界にし、全件成功時だけcommitする。
    // Controllerから別Beanの公開メソッドへ入り、例外を握り潰さずProxyの外へ伝えてrollbackする。
    @Transactional
    public EmployeeTransferResponse transfer(EmployeeTransferRequest request) {
        validate(request);
        if(new HashSet<>(request.employeeIds()).size()!=request.employeeIds().size())
            throw new IllegalArgumentException("社員IDを重複指定できません。");
        var department=requireDepartment(request.departmentId());
        List<BusinessEmployeeResponse> result=new ArrayList<>();
        for(long id:request.employeeIds()) {
            var employee=requireEmployee(id);
            employee.update(employee.getName(),employee.getEmail(),department);
            employees.flush(); // SQL発行済みでも後続の失敗なら同じTransaction全体を戻す。
            log.info("[SPR-W6-D3-03] UPDATE flushed: employeeId={}, departmentId={}",id,department.getId());
            result.add(BusinessEmployeeResponse.from(employee));
        }
        return new EmployeeTransferResponse(result.size(),List.copyOf(result));
    }
    private EmployeeEntity requireEmployee(long id) {
        return employees.findById(id).orElseThrow(()->new EmployeeNotFoundException("社員が見つかりません。 id="+id));
    }
    // SPR-W6-D3-02: Repositoryで参照先を確認し、FK違反より前に説明可能な業務例外にする。
    // 外部キーも維持し、照会後に参照先が変化した場合のDB整合性を守る。
    private DepartmentEntity requireDepartment(int id) {
        return departments.findById(id).orElseThrow(()->new DepartmentNotFoundException(id));
    }
    private <T> void validate(T request) {
        var errors=validator.validate(Objects.requireNonNull(request,"request"));
        if(!errors.isEmpty()) throw new ConstraintViolationException(errors);
    }
    private EmployeeEntity save(EmployeeEntity employee) {
        try { return employees.saveAndFlush(employee); }
        catch(DataIntegrityViolationException exception) {
            // SPR-W6-D3-01/04: メールの名前付き一意制約だけを409へ変換。別のDB障害は隠さない。
            for(Throwable cause=exception;cause!=null;cause=cause.getCause()) {
                if(cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                        && constraint.getConstraintName()!=null
                        && constraint.getConstraintName().toUpperCase(Locale.ROOT).contains("UK_EMPLOYEE_EMAIL"))
                    throw new DuplicateEmployeeEmailException(exception);
            }
            throw exception;
        }
    }
}
