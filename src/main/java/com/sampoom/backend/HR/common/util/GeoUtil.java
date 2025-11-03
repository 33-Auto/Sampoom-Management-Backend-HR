package com.sampoom.backend.HR.common.util;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class GeoUtil {

    private static final String KAKAO_ADDRESS_URL = "https://dapi.kakao.com/v2/local/search/address.json";
    private static final String KAKAO_KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    @Value("${kakao.api.key:}")
    private String kakaoApiKey;

    private static final String SAFE_PATTERN = "^[가-힣a-zA-Z0-9\\-\\s\\.,()·]*$";
    private static final int ADDRESS_MAX_LENGTH = 100;

    private static boolean isSafe(String input) {
        return input != null &&
                !input.isBlank() &&
                input.length() <= ADDRESS_MAX_LENGTH &&
                input.matches(SAFE_PATTERN);
    }

    /** 주소 → 위도/경도 변환 */
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

            // 주소 변환 시 정규식 대신 단순 문자 처리
            String sanitized = removeParentheses(address);

            // 안전하게 인코딩 (직접 URL 문자열 조합 방지)
            String encoded = URLEncoder.encode(sanitized, StandardCharsets.UTF_8);
            URI uri = new URI(KAKAO_ADDRESS_URL + "?query=" + encoded + "&analyze_type=similar");

            double[] coords = request(rt, entity, uri);
            if (isValid(coords)) return coords;

            // 키워드 검색 fallback
            uri = new URI(KAKAO_KEYWORD_URL + "?query=" + encoded);
            coords = request(rt, entity, uri);
            if (isValid(coords)) return coords;

            log.warn("❗ 변환 실패: {}", sanitized);
        } catch (Exception e) {
            log.error("❌ 주소 변환 중 오류 [{}]: {}", address, e.getMessage());
        }

        return new double[]{0.0, 0.0};
    }

    // ---------------- 내부 헬퍼 ----------------

    private static String removeParentheses(String input) {
        int start = input.indexOf('(');
        int end = input.indexOf(')');
        if (start >= 0 && end > start) {
            return (input.substring(0, start) + input.substring(end + 1)).trim();
        }
        return input;
    }

    private static boolean isValid(double[] c) {
        return c != null && c.length == 2 && !(c[0] == 0.0 && c[1] == 0.0);
    }

    private static String formatKey(String key) {
        return key.startsWith("KakaoAK ") ? key : "KakaoAK " + key;
    }

    private static double[] request(RestTemplate rt, HttpEntity<String> entity, URI uri) {
        try {
            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) {
                log.warn("⚠️ API 응답 코드: {}", res.getStatusCode());
                return new double[]{0.0, 0.0};
            }

            JSONObject json = new JSONObject(res.getBody());
            JSONArray docs = json.optJSONArray("documents");
            if (docs == null || docs.isEmpty()) return new double[]{0.0, 0.0};

            JSONObject first = docs.getJSONObject(0);
            double lat = first.getDouble("y");
            double lon = first.getDouble("x");
            return new double[]{lat, lon};

        } catch (Exception e) {
            log.error("❌ 요청 중 오류: {}", e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }
}
