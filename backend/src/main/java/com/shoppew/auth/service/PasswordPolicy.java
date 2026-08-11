package com.shoppew.auth.service;

import com.shoppew.common.api.ErrorDetail;
import com.shoppew.common.exception.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public void validate(String password) {
        boolean lower = password.codePoints().anyMatch(Character::isLowerCase);
        boolean upper = password.codePoints().anyMatch(Character::isUpperCase);
        boolean digit = password.codePoints().anyMatch(Character::isDigit);
        if (!lower || !upper || !digit) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "WEAK_PASSWORD",
                    "Mật khẩu cần có chữ thường, chữ hoa và chữ số",
                    List.of(new ErrorDetail(
                            "password",
                            "WEAK_PASSWORD",
                            "Dùng ít nhất 10 ký tự gồm chữ thường, chữ hoa và chữ số")));
        }
    }
}
