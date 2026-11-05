package com.example.demo.week7.day1;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import com.example.demo.dto.BusinessEmployeeRequest;
import com.example.demo.dto.BusinessEmployeeResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * SPR-W7-D1-04: GET/POST/PUT/DELETEの代表ケースを同一パッケージで一括実行し、変更によるHTTP契約の回帰を検出する。
 * GET/POSTはDay1の01/02を再利用する。DBの更新・削除の保証は既存Repository/Serviceテストが担う。
 */
@DisplayName("SPR-W7-D1-04 代表CRUD回帰 PUT / DELETE")
class Week7Day1CrudTests extends Week7Day1WebSupport {
    @Test @DisplayName("SPR-W7-D1-04 PUT 200・IDと更新内容を委譲")
    void updateReturnsUpdatedEmployee() throws Exception {
        var request = new BusinessEmployeeRequest("更新社員", "updated@example.test", 2);
        when(business.update(21L, request)).thenReturn(
                new BusinessEmployeeResponse(21, "更新社員", "updated@example.test", "営業部"));
        mvc.perform(put("/employees/{id}", 21).contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.name").value("更新社員"))
                .andExpect(jsonPath("$.email").value("updated@example.test"))
                .andExpect(jsonPath("$.departmentName").value("営業部"));
        verify(business).update(21L, request);
        verifyNoMoreInteractions(business);
        verifyNoInteractions(existing);
    }

    @Test @DisplayName("SPR-W7-D1-04 DELETE 204・空本文・削除IDを委譲")
    void deleteReturnsNoContent() throws Exception {
        mvc.perform(delete("/employees/{id}", 21))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(existing).delete(21L);
        verifyNoMoreInteractions(existing);
        verifyNoInteractions(business);
    }
}
