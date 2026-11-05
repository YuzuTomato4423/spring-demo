package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import com.example.demo.model.Department;
import com.example.demo.model.Employee;
import com.example.demo.repository.InMemoryEmployeeRepository;

class EmployeeMutationRepositoryTests {
    @Test void concurrentInsertAllocatesUniqueIdsAboveExistingData() throws Exception {
        var repository = new InMemoryEmployeeRepository();
        var department = new Department(1, "開発部");
        repository.save(new Employee(100, "既存社員", department));
        var pool = Executors.newFixedThreadPool(8);
        try {
            var tasks = new ArrayList<Callable<Integer>>();
            for (int i = 0; i < 100; i++) {
                tasks.add(() -> repository.insert("並行登録社員", department).getId());
            }
            var ids = new ArrayList<Integer>();
            for (var result : pool.invokeAll(tasks)) ids.add(result.get());
            assertThat(ids).doesNotHaveDuplicates().allMatch(id -> id > 100);
            assertThat(repository.findAll()).hasSize(101);
            assertThat(repository.replace(new Employee(100, "更新社員", department))).isTrue();
            assertThat(repository.findAll()).hasSize(101);
            assertThat(repository.deleteById(200)).isTrue();
            assertThat(repository.insert("削除後社員", department).getId()).isEqualTo(201);
            assertThat(repository.replace(new Employee(999, "未存在", department))).isFalse();
            assertThat(repository.deleteById(999)).isFalse();
            assertThatThrownBy(() -> repository.save(new Employee(100, "重複", department)))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            pool.shutdownNow();
        }
    }
}
