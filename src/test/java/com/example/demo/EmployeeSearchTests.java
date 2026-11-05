package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.demo.config.AppProperties;
import com.example.demo.model.Department;
import com.example.demo.model.Employee;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.repository.InMemoryEmployeeRepository;
import com.example.demo.service.EmployeeService;

/** 独立した保存先で英字の大小文字と部分一致・完全一致の境界を確認する。 */
class EmployeeSearchTests {
    private EmployeeService fixture() {
        var repository = new InMemoryEmployeeRepository();
        repository.save(new Employee(1, "Tanaka", new Department(1, "開発部")));
        repository.save(new Employee(2, "Tanabe", new Department(2, "営業部")));
        var notifier = new EmployeeRegistrationNotifier(message -> message, new AppProperties());
        return new EmployeeService(repository, notifier);
    }

    @Test void partialNameIsCaseSensitive() {
        var service = fixture();
        assertThat(service.search("Tan", null)).hasSize(2);
        assertThat(service.search("tan", null)).isEmpty();
        assertThat(service.search("naka", null)).extracting(Employee::getId).containsExactly(1);
    }

    @Test void exactDepartmentAndAndConditionsDoNotBroadenSearch() {
        var service = fixture();
        assertThat(service.search("Tan", "開発部")).extracting(Employee::getId).containsExactly(1);
        assertThat(service.search(null, "開発")).isEmpty();
        assertThat(service.search("Tanaka", "営業部")).isEmpty();
    }

    @Test void whitespaceAndAbsentQueriesLeaveRepositoryUnchanged() {
        var service = fixture();
        assertThat(service.search(" 　", "")).hasSize(2);
        assertThat(service.search(" Tanaka　", " 開発部 ")).hasSize(1);
        assertThat(service.findAll()).hasSize(2);
    }
}
