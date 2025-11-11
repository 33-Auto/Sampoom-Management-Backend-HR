package com.sampoom.backend.HR.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum ErrorStatus {

    // 400 BAD_REQUEST
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.",40001),
    MISSING_EMAIL_VERIFICATION_EXCEPTION(HttpStatus.BAD_REQUEST, "이메일 인증을 진행해주세요.",40002),



    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", 40101),

    // 403 FORBIDDEN
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.",40301),

    // 404 NOT_FOUND
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.",40401),
    VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다.",40402),
    BRANCH_NOT_FOUND(HttpStatus.NOT_FOUND, "지점을 찾을 수 없습니다.",40403),



    // 409 CONFLICT
    CONFLICT(HttpStatus.CONFLICT, "충돌이 발생했습니다.",40901),

    // 400 BAD_REQUEST
    SHORT_PUBLIC_KEY(HttpStatus.BAD_REQUEST, "서명용 공개키의 길이가 짧습니다. 적어도 2048비트 이상으로 설정하세요.", 12401),
    NULL_BLANK_TOKEN(HttpStatus.BAD_REQUEST, "토큰 값은 Null 또는 공백이면 안됩니다.", 12400),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력 값입니다.", 11402),
    INVALID_ROLE_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 권한(role) 타입입니다.", 11401),
    BLANK_TOKEN_ROLE(HttpStatus.BAD_REQUEST,"토큰 내 권한 정보가 공백입니다.",12404),
    NULL_TOKEN_ROLE(HttpStatus.BAD_REQUEST,"토큰 내 권한 정보가 NULL입니다.",12405),
    INVALID_REQUEST_ORGID(HttpStatus.BAD_REQUEST,"workspace 없이 organizationID로만 요청할 수 없습니다.",11403),
    INVALID_EMPSTATUS_TYPE(HttpStatus.BAD_REQUEST,"유효하지 않은 직원 상태(EmployeeStatus) 타입입니다.",11404),
    INVALID_PUBLIC_KEY(HttpStatus.BAD_REQUEST,"서명용 공개키가 유효하지 않거나 불러오는데 실패했습니다.",12406),
    INVALID_WORKSPACE_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 조직(workspace) 타입입니다.", 12408),

    // 401 UNAUTHORIZED
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", 12410),
    NOT_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED,"토큰의 타입이 액세스 토큰이 아닙니다.",12413),
    NOT_SERVICE_TOKEN(HttpStatus.UNAUTHORIZED,"토큰의 타입이 서비스 토큰(내부 통신용 토큰)이 아닙니다.",12414),
    INVALID_TOKEN_ROLE(HttpStatus.UNAUTHORIZED,"유효하지 않은 토큰 내 권한 정보입니다. (토큰 권한 불일치)",12415),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다.", 12411),

    // 403 FORBIDDEN
    ACCESS_DENIED(HttpStatus.FORBIDDEN,"접근 권한이 없어 접근이 거부되었습니다.",11430),

    // 404 NOT_FOUND
    NOT_FOUND_USER_BY_ID(HttpStatus.NOT_FOUND, "유저 고유 번호(userId)로 해당 유저를 찾을 수 없습니다.", 11440),
    NOT_FOUND_AGENCY_NAME(HttpStatus.NOT_FOUND, "지점명으로 대리점 이름을 찾을 수 없습니다.", 13440),
    NOT_FOUND_EMPLOYEE_AGENCY(HttpStatus.NOT_FOUND,"전체 대리점에서 해당 직원을 찾을 수 없습니다.",13441),
    NOT_FOUND_MEMBER_PRODUCTION(HttpStatus.NOT_FOUND,"생산 관리에서 해당 직원을 찾을 수 없습니다.",13442),
    NOT_FOUND_MEMBER_INVENTORY(HttpStatus.NOT_FOUND,"재고 관리에서 해당 직원을 찾을 수 없습니다.",13443),
    NOT_FOUND_MEMBER_PURCHASE(HttpStatus.NOT_FOUND,"구매 관리에서 해당 직원을 찾을 수 없습니다.",13444),
    NOT_FOUND_MEMBER_SALES(HttpStatus.NOT_FOUND,"판매 관리에서 해당 직원을 찾을 수 없습니다.",13445),
    NOT_FOUND_MEMBER_MD(HttpStatus.NOT_FOUND,"기준 정보 관리에서 해당 직원을 찾을 수 없습니다.",13446),
    NOT_FOUND_MEMBER_HR(HttpStatus.NOT_FOUND,"인사 관리에서 해당 직원을 찾을 수 없습니다.",13447),

    // 409 CONFLICT
    DUPLICATED_USER_ID(HttpStatus.CONFLICT, "이미 존재하는 유저의 ID입니다.", 11491),

    // 500 INTERNAL_SERVER_ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", 10500),
    INVALID_EVENT_FORMAT(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 형식이 유효하지 않습니다.", 10501),
    FAILED_CONNECTION_KAFKA(HttpStatus.INTERNAL_SERVER_ERROR,"Kafka 브로커 연결 또는 통신에 실패했습니다.",10503),
    EVENT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Kafka 이벤트 처리 중 예외가 발생했습니다.",10504),
    OUTBOX_SERIALIZATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"Outbox 직렬화에 실패했습니다.",10505),


    ;

    private final HttpStatus httpStatus;
    private final String message;
    private final int code;


    public int getStatusCode() {
        return this.httpStatus.value();
    }

}
