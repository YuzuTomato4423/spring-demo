package com.example.demo.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.exception.EmployeeNotFoundException;
import com.example.demo.model.Employee;
import com.example.demo.model.Department;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.repository.EmployeeRepository;

/** 登録ルールと保存後の通知をまとめ、受付側を保存先・通知方式から独立させる。 */
@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
	
	private int counter = 0;
	
	private final EmployeeRepository repository;
	private final EmployeeRegistrationNotifier notifier;
	
	public EmployeeService(EmployeeRepository repository, EmployeeRegistrationNotifier notifier) {
        this.repository = repository;
        this.notifier = notifier;
        log.info("[DI] EmployeeService@{} -> {}@{}",
                Integer.toHexString(System.identityHashCode(this)),
                repository.getClass().getSimpleName(),
                Integer.toHexString(System.identityHashCode(repository)));
    }

    public void incrementCounter() {
        counter++;
    }

    public int getCounter() {
        return counter;
    }
	
	/** 保存に成功した社員だけを通知対象とし、入力不正を登録成功として伝えない。 */
	public void register(Employee employee) {

	    if (employee.getName() == null || employee.getName().isBlank()) {
	        throw new IllegalArgumentException("社員名を入力してください。");
	    }

	    repository.save(employee);
	    notifier.notifyRegistration(employee);
	}
	
	public List<Employee> findAll() {
	    return repository.findAll();
	}

    /**
     * SPR-W4-D2-03: 検索ルールはServiceに集約し、HTTP受付に依存させない。
     * 名前は大文字小文字を区別する部分一致、部署名は完全一致。指定条件はAND。
     * 前後の空白を除き、null・空文字は条件なし。該当なしは空の一覧を返す。
     */
    public List<Employee> search(String name, String department) {
        String nameCondition = normalizeCondition(name);
        String departmentCondition = normalizeCondition(department);
        return findAll().stream()
                .filter(employee -> nameCondition == null
                        || employee.getName().contains(nameCondition))
                .filter(employee -> departmentCondition == null
                        || employee.getDepartment().getName().equals(departmentCondition))
                .toList();
    }

    /**
     * SPR-W4-D3-01: Serviceは入力形式に依存せず、名前・部署の業務ルールを扱う。
     * 保存が成功した場合だけ既存通知を呼ぶ。IDは保存先で一意に採番する。
     */
    public Employee create(String name, int departmentId) {
        validateName(name);
        Department department = resolveDepartment(departmentId);
        Employee employee = repository.insert(name, department);
        notifier.notifyRegistration(employee);
        return employee;
    }

    /** SPR-W4-D3-02: 存在する社員だけを更新する。更新を新規登録として通知しない。 */
    public Employee update(int id, String name, int departmentId) {
        findById(id);
        validateName(name);
        Employee employee = new Employee(id, name, resolveDepartment(departmentId));
        if (!repository.replace(employee)) {
            throw new EmployeeNotFoundException("社員が見つかりません。 id=" + id);
        }
        return employee;
    }

    /** SPR-W4-D3-03: 未存在は業務例外にし、HTTP StatusはControllerで選択する。 */
    public void delete(int id) {
        if (!repository.deleteById(id)) {
            throw new EmployeeNotFoundException("社員が見つかりません。 id=" + id);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("社員名を入力してください。");
        }
    }

    private static Department resolveDepartment(int departmentId) {
        return switch (departmentId) {
            case 1 -> new Department(1, "開発部");
            case 2 -> new Department(2, "営業部");
            default -> throw new IllegalArgumentException("存在しない部署です。");
        };
    }

    private static String normalizeCondition(String value) {
        if (value == null) {
            return null;
        }
        String condition = value.strip();
        return condition.isEmpty() ? null : condition;
    }
	
	/**
	 * SPR-W4-D4-02: Repositoryの未存在をServiceで業務例外へ変換する。
	 * GET/PUTなどの受付で同じ判定を再利用し、HTTPに依存しない業務処理を保つ。
	 * 削除で未存在だった場合も同じ例外とし、HTTP 404への変換はAdviceへ委ねる。
	 */
	public Employee findById(int id) {
		return repository.findById(id)
				.orElseThrow(() ->
					new EmployeeNotFoundException(
							"社員が見つかりません。 id="+id
							)
					);
	}
	
	public List<Employee> findByDepartmentId(int departmentId){
		return repository.findAll()
				.stream()
				.filter(employee ->
						employee.getDepartment().getId()==departmentId)
				.toList();
	}
}
