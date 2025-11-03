package com.sampoom.backend.HR.common.util;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;


@Slf4j
@Component
public class GeoUtil {

    private static final String KAKAO_DOMAIN = "https://dapi.kakao.com";
    private static final String KAKAO_ADDRESS_PATH = "/v2/local/search/address.json";
    private static final String KAKAO_KEYWORD_PATH = "/v2/local/search/keyword.json";

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    /**
     * 주소를 위도/경도로 변환
     */
    public double[] getLatLngFromAddress(String address) {
        if (address == null || address.isBlank()) {
            log.warn("⚠️ 주소가 비어 있음 — 변환 불가");
            return new double[]{0.0, 0.0};
        }

        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION,
                    kakaoApiKey.startsWith("KakaoAK ") ? kakaoApiKey : "KakaoAK " + kakaoApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 기본 주소로 시도
            double[] coords = requestAddress(rt, entity, address);
            if (isValid(coords)) return coords;

            // 괄호 및 특수문자 제거 후 재시도
            String simplified = normalizeAddress(address);
            if (!simplified.equals(address)) {
                coords = requestAddress(rt, entity, simplified);
                if (isValid(coords)) return coords;
            }

            // 키워드 검색으로 재시도
            coords = requestKeyword(rt, entity, address);
            if (isValid(coords)) return coords;

            if (!simplified.equals(address)) {
                coords = requestKeyword(rt, entity, simplified);
                if (isValid(coords)) return coords;
            }

            log.warn("❌ 모든 시도 실패: {}", address);
        } catch (Exception e) {
            log.error("❌ 주소 → 좌표 변환 중 오류 ({}): {}", address, e.getMessage());
        }

        return new double[]{0.0, 0.0};
    }

    // ---------------------------------------------------------
    // 🔒 내부 도우미 메서드 (모두 안전하게 작성)
    // ---------------------------------------------------------

    private static boolean isValid(double[] coords) {
        return coords != null && coords.length == 2 && !(coords[0] == 0.0 && coords[1] == 0.0);
    }

    /**
     * 괄호 및 특수문자를 제거
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
            if (depth == 0) sb.append(c);
        }

        String result = sb.toString()
                .replace(",", " ")
                .replace("·", " ")
                .trim();

        while (result.contains("  ")) {
            result = result.replace("  ", " ");
        }

        return result;
    }

    /**
     * SSRF 방지: Kakao 도메인만 허용
     */
    private static UriComponents buildSafeUri(String path, String query) {
        // 도메인과 path를 고정하여 SSRF 불가
        return UriComponentsBuilder
                .fromHttpUrl(KAKAO_DOMAIN)
                .path(path)
                .queryParam("query", query)
                .build(true);
    }

    /**
     * Kakao 주소검색 API 호출 (SSRF-safe)
     */
    private static double[] requestAddress(RestTemplate rt, HttpEntity<String> entity, String query) {
        try {
            UriComponents uriComponents = buildSafeUri(KAKAO_ADDRESS_PATH, query);
            String uri = uriComponents.toUriString();

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) {
                log.warn("⚠️ Kakao 주소검색 응답 오류: {}", res.getStatusCode());
                return new double[]{0.0, 0.0};
            }

            JSONObject json = new JSONObject(res.getBody());
            JSONArray docs = json.optJSONArray("documents");
            if (docs == null || docs.isEmpty()) return new double[]{0.0, 0.0};

            JSONObject first = docs.getJSONObject(0);
            return new double[]{first.getDouble("y"), first.getDouble("x")};

        } catch (Exception e) {
            log.error("❌ Kakao 주소검색 중 예외 ({}): {}", query, e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }

    /**
     * Kakao 키워드검색 API 호출 (SSRF-safe)
     */
    private static double[] requestKeyword(RestTemplate rt, HttpEntity<String> entity, String query) {
        try {
            UriComponents uriComponents = buildSafeUri(KAKAO_KEYWORD_PATH, query);
            String uri = uriComponents.toUriString();

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) {
                log.warn("⚠️ Kakao 키워드검색 응답 오류: {}", res.getStatusCode());
                return new double[]{0.0, 0.0};
            }

            JSONObject json = new JSONObject(res.getBody());
            JSONArray docs = json.optJSONArray("documents");
            if (docs == null || docs.isEmpty()) return new double[]{0.0, 0.0};

            JSONObject first = docs.getJSONObject(0);
            return new double[]{first.getDouble("y"), first.getDouble("x")};

        } catch (Exception e) {
            log.error("❌ Kakao 키워드검색 중 예외 ({}): {}", query, e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }
}
