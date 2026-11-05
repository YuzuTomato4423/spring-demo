package com.example.demo.week7.day1;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import com.example.demo.dto.BusinessEmployeeRequest;
import com.example.demo.dto.BusinessEmployeeResponse;
import tools.jackson.databind.ObjectMapper;

/** SPR-W7-D1-02: @Validが不正入力をService実行前に400へ変換し、項目別エラーを返すことを保証する。 */
@DisplayName("SPR-W7-D1-02 POST 201 / Validation 400")
class Week7Day1PostValidationTests extends Week7Day1WebSupport {
    @Test @DisplayName("SPR-W7-D1-02 正常POST 201・Location・応答JSON")
    void validPostReturnsCreatedWithLocation() throws Exception {
        var request = new BusinessEmployeeRequest("登録社員", "new@example.test", 1);
        when(business.create(request)).thenReturn(
                new BusinessEmployeeResponse(21, "登録社員", "new@example.test", "開発部"));
        mvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/employees/21"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.name").value("登録社員"))
                .andExpect(jsonPath("$.email").value("new@example.test"))
                .andExpect(jsonPath("$.departmentName").value("開発部"));
        verify(business).create(request);
        verifyNoMoreInteractions(business);
        verifyNoInteractions(existing);
    }

    @ParameterizedTest(name = "SPR-W7-D1-02 {0} -> 400")
    @MethodSource("invalidRequests")
    void invalidPostReturnsFieldErrorWithoutCallingService(String label, String name,
            String email, int departmentId, String field, String message) throws Exception {
        var request = new BusinessEmployeeRequest(name, email, departmentId);
        mvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("入力内容を確認してください。"))
                .andExpect(jsonPath("$.errors", aMapWithSize(1)))
                .andExpect(jsonPath("$.errors." + field).value(message));
        verifyNoInteractions(business, existing);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("氏名空白 @NotBlank", "   ", "valid@example.test", 1,
                        "name", "社員名を入力してください。"),
                Arguments.of("メール空白 @NotBlank", "入力社員", "   ", 1,
                        "email", "メールを入力してください。"),
                Arguments.of("メール形式 @Email", "入力社員", "invalid-address", 1,
                        "email", "メール形式を確認してください。"),
                // 有効な形式の255文字メールで@Sizeだけを検証し、複数制約の競合を避ける。
                Arguments.of("メール255文字 @Size", "入力社員",
                        "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(62), 1,
                        "email", "メールは254文字以内で入力してください。"),
                Arguments.of("部署ID 0 @Positive", "入力社員", "valid@example.test", 0,
                        "departmentId", "部署IDは正の整数で指定してください。"));
    }
}
