package com.example.demo.week7.day1;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import com.example.demo.dto.BusinessEmployeeResponse;
import com.example.demo.dto.EmployeeResponse;

/** SPR-W7-D1-01: Web層のURL・検索引数・HTTP 200・公開JSONを確認し、業務処理と分けて原因を特定する。 */
@DisplayName("SPR-W7-D1-01 GET MockMvc")
class Week7Day1GetTests extends Week7Day1WebSupport {
    @Test @DisplayName("SPR-W7-D1-01 一覧GET 200・検索引数・JSON")
    void listReturnsEmployeesAndDelegatesSearch() throws Exception {
        when(existing.findAll("田中", "開発部", Sort.by("id"))).thenReturn(List.of(
                new EmployeeResponse(11, "田中一郎", "開発部"),
                new EmployeeResponse(12, "田中次郎", "開発部")));
        mvc.perform(get("/employees").param("name", "田中").param("department", "開発部"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", aMapWithSize(3)))
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].name").value("田中一郎"))
                .andExpect(jsonPath("$[0].departmentName").value("開発部"))
                .andExpect(jsonPath("$[1].id").value(12))
                .andExpect(jsonPath("$[1].name").value("田中次郎"))
                .andExpect(jsonPath("$[1].departmentName").value("開発部"));
        verify(existing).findAll("田中", "開発部", Sort.by("id"));
        verifyNoMoreInteractions(existing);
        verifyNoInteractions(business);
    }

    @Test @DisplayName("SPR-W7-D1-01 詳細GET 200・ID・公開DTO")
    void detailReturnsEmployeeAndDelegatesId() throws Exception {
        when(business.findById(11L)).thenReturn(
                new BusinessEmployeeResponse(11, "田中一郎", "tanaka@example.test", "開発部"));
        mvc.perform(get("/employees/{id}", 11))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.name").value("田中一郎"))
                .andExpect(jsonPath("$.email").value("tanaka@example.test"))
                .andExpect(jsonPath("$.departmentName").value("開発部"));
        verify(business).findById(11L);
        verifyNoMoreInteractions(business);
        verifyNoInteractions(existing);
    }
}
