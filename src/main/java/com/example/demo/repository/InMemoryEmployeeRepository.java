package com.example.demo.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Employee;
import com.example.demo.model.Department;

/** 学習用の保存先。singletonで同じ社員一覧を共有し、プロセス終了後は保持しない。 */
@Repository
public class InMemoryEmployeeRepository implements  EmployeeRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEmployeeRepository.class);
	private final List<Employee> employees = new ArrayList<>();
    private long nextId = 1;

    public InMemoryEmployeeRepository() {
        log.info("[BEAN] InMemoryEmployeeRepository@{} created",
                Integer.toHexString(System.identityHashCode(this)));
    }
	
	@Override
	public synchronized void save(Employee employee) {
		if (findById(employee.getId()).isPresent()) {
            throw new IllegalArgumentException("社員IDが重複しています。 id=" + employee.getId());
        }
		employees.add(employee);
        nextId = Math.max(nextId, (long) employee.getId() + 1);
	}

    /** SPR-W4-D3-01: 採番と保存を一緒に行い、既存IDや並行登録との衝突を防ぐ。 */
    @Override
    public synchronized Employee insert(String name, Department department) {
        if (nextId > Integer.MAX_VALUE) {
            throw new IllegalStateException("社員IDの採番上限です。");
        }
        Employee employee = new Employee((int) nextId, name, department);
        save(employee);
        return employee;
    }

    /** SPR-W4-D3-02: 同じ位置を置換するので、更新で件数が増えない。 */
    @Override
    public synchronized boolean replace(Employee employee) {
        for (int index = 0; index < employees.size(); index++) {
            if (employees.get(index).getId() == employee.getId()) {
                employees.set(index, employee);
                return true;
            }
        }
        return false;
    }

    /** SPR-W4-D3-03: 削除有無を返す。採番値は巻き戻さず、削除したIDを再利用しない。 */
    @Override
    public synchronized boolean deleteById(int id) {
        return employees.removeIf(employee -> employee.getId() == id);
    }
	
	@Override
	public synchronized Optional<Employee> findById(int id) {
		for (Employee employee: employees) {
			if(employee.getId()==id) {
				return Optional.of(employee);
			}
		}
		
		return Optional.empty();
	}
	
	/** 呼出側の一覧操作によって保存先のコレクションが変更されることを防ぐ。 */
	@Override
	public synchronized List<Employee>findAll(){
		return new ArrayList<>(employees);
	}
}
