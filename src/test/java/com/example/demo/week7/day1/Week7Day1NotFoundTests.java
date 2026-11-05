package com.example.demo.week7.day1;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import com.example.demo.exception.EmployeeNotFoundException;

/** SPR-W7-D1-03: 共通のstatus/message/errors形式により、クライアントが400と404を同じ構造で処理できることを確認する。 */
@DisplayName("SPR-W7-D1-03 NotFound 404 / 共通JSON")
class Week7Day1NotFoundTests extends Week7Day1WebSupport {
    @Test @DisplayName("SPR-W7-D1-03 未存在ID GET 404・status/message/errors")
    void missingEmployeeReturnsCommonErrorJson() throws Exception {
        String message = "社員が見つかりません。 id=999999";
        when(business.findById(999999L)).thenThrow(new EmployeeNotFoundException(message));
        mvc.perform(get("/employees/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.errors", anEmptyMap()));
        verify(business).findById(999999L);
        verifyNoMoreInteractions(business);
        verifyNoInteractions(existing);
    }
}
