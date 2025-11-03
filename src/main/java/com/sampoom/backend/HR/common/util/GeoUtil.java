package com.sampoom.backend.HR.common.util;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    /** 주소를 위도/경도로 변환 */
    public double[] getLatLngFromAddress(String address) {
        if (!isSafe(address)) {
            log.warn("⚠️ 주소 입력이 유효하지 않음: {}", address);
            return new double[]{0.0, 0.0};
        }

        if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
            log.error("❌ Kakao API Key가 설정되지 않았습니다.");
            return new double[]{0.0, 0.0};
        }

        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION,
                    kakaoApiKey.startsWith("KakaoAK ") ? kakaoApiKey : "KakaoAK " + kakaoApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            double[] coords = requestAddress(rt, entity, encoded);
            if (valid(coords)) return coords;

            String simplified = normalizeAddress(address);
            if (!simplified.equals(address)) {
                log.debug("주소 재시도 (정규화): {}", simplified);
                coords = requestAddress(rt, entity, URLEncoder.encode(simplified, StandardCharsets.UTF_8));
                if (valid(coords)) return coords;
            }

            String inner = extractInnerText(address);
            if (inner != null) {
                log.debug("괄호 내부 재시도: {}", inner);
                coords = requestKeyword(rt, entity, URLEncoder.encode(inner, StandardCharsets.UTF_8));
                if (valid(coords)) return coords;
            }

            coords = requestKeyword(rt, entity, URLEncoder.encode(address, StandardCharsets.UTF_8));
            if (valid(coords)) return coords;

            if (!simplified.equals(address)) {
                coords = requestKeyword(rt, entity, URLEncoder.encode(simplified, StandardCharsets.UTF_8));
                if (valid(coords)) return coords;
            }

            log.warn("❗ 모든 변환 시도 실패: {}", address);
            return new double[]{0.0, 0.0};

        } catch (Exception e) {
            log.error("❌ 주소 → 좌표 변환 중 오류 [{}]: {}", address, e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }

    // ---------------- 내부 로직 ----------------

    private static String normalizeAddress(String address) {
        return address.replaceAll("\\([^)]*\\)", " ")
                .replaceAll("[,·]", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static String extractInnerText(String s) {
        int start = s.indexOf('(');
        int end = s.indexOf(')');
        if (start >= 0 && end > start) {
            String inner = s.substring(start + 1, end).trim();
            if (!inner.isEmpty()) return inner;
        }
        return null;
    }

    private static boolean valid(double[] c) {
        return c != null && c.length == 2 && !(c[0] == 0.0 && c[1] == 0.0);
    }

    private static double[] requestAddress(RestTemplate rt, HttpEntity<String> entity, String encoded) {
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(KAKAO_ADDRESS_URL)
                    .queryParam("query", encoded)
                    .queryParam("analyze_type", "similar")
                    .encode()
                    .toUriString();

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) return new double[]{0.0, 0.0};

            JSONObject body = new JSONObject(res.getBody());
            JSONArray docs = body.optJSONArray("documents");
            if (docs == null || docs.isEmpty()) return new double[]{0.0, 0.0};

            JSONObject first = docs.getJSONObject(0);
            return new double[]{first.getDouble("y"), first.getDouble("x")};

        } catch (Exception e) {
            log.error("주소 API 오류: {}", e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }

    private static double[] requestKeyword(RestTemplate rt, HttpEntity<String> entity, String encoded) {
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(KAKAO_KEYWORD_URL)
                    .queryParam("query", encoded)
                    .encode()
                    .toUriString();

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) return new double[]{0.0, 0.0};

            JSONObject body = new JSONObject(res.getBody());
            JSONArray docs = body.optJSONArray("documents");
            if (docs == null || docs.isEmpty()) return new double[]{0.0, 0.0};

            JSONObject first = docs.getJSONObject(0);
            return new double[]{first.getDouble("y"), first.getDouble("x")};

        } catch (Exception e) {
            log.error("키워드 API 오류: {}", e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }
}
