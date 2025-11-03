package com.sampoom.backend.HR.common.util;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GeoUtil {

    private static final String KAKAO_ADDRESS_URL = "https://dapi.kakao.com/v2/local/search/address.json";
    private static final String KAKAO_KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    // 안전한 주소 입력 검증용 (SSRF 방지)
    private static final Pattern ADDRESS_SAFE_PATTERN =
            Pattern.compile("^[가-힣a-zA-Z0-9\\-\\s\\.,()·]*$");
    private static final int ADDRESS_MAX_LENGTH = 100;

    // 정규식 미리 컴파일 (CodeQL의 polynomial regex 경고 방지)
    private static final Pattern SAFE_PAREN_PATTERN = Pattern.compile("\\(([^()]{0,50})\\)");

    private static boolean isSafeAddressInput(String address) {
        if (address == null || address.isBlank()) return false;
        if (address.length() > ADDRESS_MAX_LENGTH) return false;
        return ADDRESS_SAFE_PATTERN.matcher(address).matches();
    }

    /**
     * 주소 문자열을 위도(lat), 경도(lon)로 변환
     */
    public double[] getLatLngFromAddress(String address) {
        if (!isSafeAddressInput(address)) {
            log.warn(" 주소가 비어 있어 좌표 변환 불가");
            return new double[]{0.0, 0.0};
        }

        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            String authHeader = kakaoApiKey.startsWith("KakaoAK ") ? kakaoApiKey : "KakaoAK " + kakaoApiKey;
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            headers.set("Content-Type", "application/json;charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 도로명 주소 검색 (유사 매칭)
            double[] coords = tryAddress(rt, entity, address, true);
            if (isValid(coords)) return coords;

            // 괄호 내용 제거 후 다시 시도
            String simplified = address
                    .replaceAll("\\([^)]*\\)", " ")
                    .replaceAll("[,·]", " ")
                    .replaceAll("\\s{2,}", " ")
                    .trim();
            if (!simplified.equals(address)) {
                log.warn("⚠️ 주소 재시도(정규화): {}", simplified);
                coords = tryAddress(rt, entity, simplified, true);
                if (isValid(coords)) return coords;
            }

            // 괄호 안 키워드 추출
            Matcher matcher = SAFE_PAREN_PATTERN.matcher(address);
            if (matcher.find()) {
                String inside = matcher.group(1).split(",")[0].trim();
                if (!inside.isEmpty()) {
                    log.warn("⚠️ 괄호 내부 키워드 재시도: {}", inside);
                    coords = tryKeyword(rt, entity, inside);
                    if (isValid(coords)) return coords;
                }
            }

            // 전체 주소를 키워드 검색으로 재시도
            coords = tryKeyword(rt, entity, address);
            if (isValid(coords)) return coords;

            // 정규화된 주소를 키워드 검색으로 재시도
            if (!simplified.equals(address)) {
                coords = tryKeyword(rt, entity, simplified);
                if (isValid(coords)) return coords;
            }

            log.warn("⚠️ 모든 시도 실패: {}", address);
        } catch (Exception e) {
            log.error("❌ 주소 → 좌표 변환 중 예외 ({}): {}", address, e.getMessage());
        }

        return new double[]{0.0, 0.0};
    }

    // -------------------- 내부 함수 --------------------

    private static boolean isValid(double[] c) {
        return c != null && c.length == 2 && !(c[0] == 0.0 && c[1] == 0.0);
    }

    private static double[] tryAddress(RestTemplate rt, HttpEntity<String> entity, String q, boolean similar) {
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(KAKAO_ADDRESS_URL)
                    .queryParam("query", q)
                    .queryParam("analyze_type", similar ? "similar" : "exact")
                    .build(true)
                    .toUriString();

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) {
                log.warn("⚠️ 주소검색 응답 코드: {} (q={})", res.getStatusCode(), q);
                return new double[]{0.0, 0.0};
            }

            JSONObject json = new JSONObject(res.getBody());
            JSONArray docs = json.getJSONArray("documents");
            if (docs.isEmpty()) {
                log.warn("⚠️ 주소 검색 결과 없음: {}", q);
                return new double[]{0.0, 0.0};
            }

            JSONObject first = docs.getJSONObject(0);
            double lon = first.getDouble("x");
            double lat = first.getDouble("y");
            log.info("📍 주소검색 성공: {} → 위도 {}, 경도 {}", q, lat, lon);
            return new double[]{lat, lon};

        } catch (Exception e) {
            log.error("❌ 주소검색 중 오류 ({}) : {}", q, e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }

    private static double[] tryKeyword(RestTemplate rt, HttpEntity<String> entity, String q) {
        try {
            String trimmed = q.length() > 90 ? q.substring(0, 90) : q; // ⚙️ 길이 제한 회피
            String uri = UriComponentsBuilder.fromHttpUrl(KAKAO_KEYWORD_URL)
                    .queryParam("query", trimmed)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) {
                log.warn("⚠️ 키워드검색 응답 코드: {} (q={})", res.getStatusCode(), trimmed);
                return new double[]{0.0, 0.0};
            }

            JSONObject json = new JSONObject(res.getBody());
            JSONArray docs = json.getJSONArray("documents");
            if (docs.isEmpty()) {
                log.warn("⚠️ 키워드 검색 결과 없음: {}", trimmed);
                return new double[]{0.0, 0.0};
            }

            JSONObject first = docs.getJSONObject(0);
            double lon = first.getDouble("x");
            double lat = first.getDouble("y");
            log.info("📍 키워드검색 성공: {} → 위도 {}, 경도 {}", trimmed, lat, lon);
            return new double[]{lat, lon};

        } catch (Exception e) {
            log.error("❌ 키워드검색 중 오류 ({}) : {}", q, e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }
}
