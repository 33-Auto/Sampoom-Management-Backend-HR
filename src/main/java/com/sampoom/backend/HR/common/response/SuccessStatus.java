package com.sampoom.backend.HR.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum SuccessStatus {

    // 공통 성공 메시지
    OK(HttpStatus.OK, "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "리소스가 성공적으로 생성되었습니다."),

    // 거래처(Vendor)
    VENDOR_CREATED(HttpStatus.CREATED, "거래처가 성공적으로 등록되었습니다."),
    VENDOR_UPDATED(HttpStatus.OK, "거래처 정보가 성공적으로 수정되었습니다."),
    VENDOR_DELETED(HttpStatus.NO_CONTENT, "거래처가 비활성화되었습니다."),

    // 지점(Branch)
    BRANCH_CREATED(HttpStatus.CREATED, "지점이 성공적으로 등록되었습니다."),
    BRANCH_UPDATED(HttpStatus.OK, "지점 정보가 성공적으로 수정되었습니다."),
    BRANCH_DEACTIVATED(HttpStatus.NO_CONTENT, "지점이 비활성화되었습니다."),


    ;




    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}
