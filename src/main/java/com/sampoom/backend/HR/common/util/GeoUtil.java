package com.sampoom.backend.HR.common.util;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class GeoUtil {

    private static final String KAKAO_ADDRESS_URL = "https://dapi.kakao.com/v2/local/search/address.json";
    private static final String KAKAO_KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    @Value("${kakao.api.key:}")
    private String kakaoApiKey;

    // 안전한 주소 입력 검증용 (SSRF 방지)
    private static final String SAFE_PATTERN = "^[가-힣a-zA-Z0-9\\-\\s\\.,()·]*$";
    private static final int ADDRESS_MAX_LENGTH = 100;

    private static boolean isSafe(String input) {
        return input != null &&
                !input.isBlank() &&
                input.length() <= ADDRESS_MAX_LENGTH &&
                input.matches(SAFE_PATTERN);
    }

    /**
     * 주소 문자열을 위도(lat), 경도(lon)로 변환
     */
    public double[] getLatLngFromAddress(String address) {
        if (!isSafe(address)) {
            log.warn("⚠️ 유효하지 않은 주소 입력: {}", address);
            return new double[]{0.0, 0.0};
        }

        if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
            log.error("❌ Kakao API Key 미설정");
            return new double[]{0.0, 0.0};
        }

        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, formatKey(kakaoApiKey));
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 기본 주소 검색
            double[] coords = requestAddress(rt, entity, address);
            if (isValid(coords)) return coords;

            // 괄호, 특수문자 정리 후 재시도
            String simplified = normalizeAddress(address);
            if (!simplified.equals(address)) {
                log.debug("주소 재시도 (정규화): {}", simplified);
                coords = requestAddress(rt, entity, simplified);
                if (isValid(coords)) return coords;
            }

            // 괄호 내부 키워드 재시도
            String inner = extractInnerText(address);
            if (inner != null) {
                log.debug("괄호 내부 재시도: {}", inner);
                coords = requestKeyword(rt, entity, inner);
                if (isValid(coords)) return coords;
            }

            // 키워드 검색 (전체 주소)
            coords = requestKeyword(rt, entity, address);
            if (isValid(coords)) return coords;

            // 정규화 주소 키워드 검색
            if (!simplified.equals(address)) {
                coords = requestKeyword(rt, entity, simplified);
                if (isValid(coords)) return coords;
            }

            log.warn("❗ 모든 변환 시도 실패: {}", address);

        } catch (Exception e) {
            log.error("❌ 주소 → 좌표 변환 중 예외 [{}]: {}", address, e.getMessage());
        }

        return new double[]{0.0, 0.0};
    }

    // -------------------- 내부 함수 --------------------

    private static boolean isValid(double[] c) {
        return c != null && c.length == 2 && !(c[0] == 0.0 && c[1] == 0.0);
    }

    private static String formatKey(String key) {
        return key.startsWith("KakaoAK ") ? key : "KakaoAK " + key;
    }

    /**
     * 괄호나 특수문자를 단순 제거하는 안전 버전 (정규식 없음)
     */
    private static String normalizeAddress(String input) {
        if (input == null || input.isBlank()) return "";

        StringBuilder sb = new StringBuilder();
        int depth = 0;

        for (char c : input.toCharArray()) {
            if (c == '(') {
                depth++;
                continue;
            } else if (c == ')') {
                if (depth > 0) depth--;
                continue;
            }

            // 괄호 안 문자는 모두 건너뜀
            if (depth == 0) sb.append(c);
        }

        // 나머지 단순 치환 (안전한 문자만)
        return sb.toString()
                .replace(",", " ")
                .replace("·", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }


    /**
     * 괄호 안의 키워드 추출 (예: "서울특별시(중구)" → "중구")
     */
    private static String extractInnerText(String input) {
        int start = input.indexOf('(');
        int end = input.indexOf(')');
        if (start >= 0 && end > start) {
            return input.substring(start + 1, end).trim();
        }
        return null;
    }

    private static double[] requestAddress(RestTemplate rt, HttpEntity<String> entity, String query) {
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(KAKAO_ADDRESS_URL)
                    .queryParam("query", query)
                    .queryParam("analyze_type", "similar")
                    .build(true)
                    .toUriString();

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) {
                log.warn("⚠️ 주소검색 응답 코드: {} (q={})", res.getStatusCode(), query);
                return new double[]{0.0, 0.0};
            }

            JSONObject json = new JSONObject(res.getBody());
            JSONArray docs = json.optJSONArray("documents");
            if (docs == null || docs.isEmpty()) {
                log.warn("⚠️ 주소 검색 결과 없음: {}", query);
                return new double[]{0.0, 0.0};
            }

            JSONObject first = docs.getJSONObject(0);
            double lat = first.getDouble("y");
            double lon = first.getDouble("x");
            log.info("📍 주소검색 성공: {} → 위도 {}, 경도 {}", query, lat, lon);
            return new double[]{lat, lon};

        } catch (Exception e) {
            log.error("❌ 주소검색 중 오류 ({}) : {}", query, e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }

    private static double[] requestKeyword(RestTemplate rt, HttpEntity<String> entity, String query) {
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(KAKAO_KEYWORD_URL)
                    .queryParam("query", query)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) {
                log.warn("⚠️ 키워드검색 응답 코드: {} (q={})", res.getStatusCode(), query);
                return new double[]{0.0, 0.0};
            }

            JSONObject json = new JSONObject(res.getBody());
            JSONArray docs = json.optJSONArray("documents");
            if (docs == null || docs.isEmpty()) {
                log.warn("⚠️ 키워드 검색 결과 없음: {}", query);
                return new double[]{0.0, 0.0};
            }

            JSONObject first = docs.getJSONObject(0);
            double lat = first.getDouble("y");
            double lon = first.getDouble("x");
            log.info("📍 키워드검색 성공: {} → 위도 {}, 경도 {}", query, lat, lon);
            return new double[]{lat, lon};

        } catch (Exception e) {
            log.error("❌ 키워드검색 중 오류 ({}) : {}", query, e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }
}
