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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

            // 1) 도로명/주소 검색 (address API) — analyze_type=similar 로 시도
            double[] coords = requestAddress(rt, entity, address);
            if (isValid(coords)) return coords;

            // 2) 괄호 및 일부 특수문자 제거 후 재시도
            String simplified = normalizeAddress(address);
            if (!simplified.equals(address)) {
                coords = requestAddress(rt, entity, simplified);
                if (isValid(coords)) return coords;
            }

            // 3) 키워드 검색으로 시도 (keyword API) — 길이 제한 고려
            double[] keywordCoords = requestKeyword(rt, entity, address);
            if (isValid(keywordCoords)) return keywordCoords;

            if (!simplified.equals(address)) {
                keywordCoords = requestKeyword(rt, entity, simplified);
                if (isValid(keywordCoords)) return keywordCoords;
            }

            log.warn("❌ 모든 시도 실패: {}", address);
        } catch (Exception e) {
            log.error("❌ 주소 → 좌표 변환 중 오류 ({}): {}", address, e.getMessage());
        }

        return new double[]{0.0, 0.0};
    }

    // ---------------- helper ----------------

    private static boolean isValid(double[] coords) {
        return coords != null && coords.length == 2 && !(coords[0] == 0.0 && coords[1] == 0.0);
    }

    private static boolean isSafeQuery(String query) {
        if (query == null || query.isBlank()) return false;
        // 허용 문자만 통과 (한글, 영문, 숫자, 공백, 일부 문장부호)
        return query.matches("^[가-힣a-zA-Z0-9\\s.,()\\-·]*$");
    }

    /**
     * Kakao가 허용하는 최대 쿼리(원문 기준)를 맞추기 위해 UTF-8 바이트 단위로 잘라낸다.
     * (Kakao 메세지는 'Max (query) length 100' 를 반환하므로 안전하게 100 바이트 이하로 자름)
     */
    private static String truncateQuery(String query) {
        if (query == null) return "";
        try {
            byte[] bytes = query.getBytes(StandardCharsets.UTF_8);
            if (bytes.length <= 100) return query;

            int byteCount = 0;
            StringBuilder sb = new StringBuilder();
            for (char c : query.toCharArray()) {
                int charBytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8).length;
                if (byteCount + charBytes > 100) break;
                sb.append(c);
                byteCount += charBytes;
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("⚠️ Query truncate 중 오류: {}", e.getMessage());
            return query.length() > 30 ? query.substring(0, 30) : query;
        }
    }

    /**
     * 괄호 및 일부 특수문자를 제거 (정규식 과도 사용을 피한 안전한 구현)
     */
    private static String normalizeAddress(String input) {
        if (input == null || input.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (char c : input.toCharArray()) {
            if (c == '(') { depth++; continue; }
            if (c == ')') { if (depth > 0) depth--; continue; }
            if (depth == 0) sb.append(c);
        }
        String result = sb.toString().replace(",", " ").replace("·", " ").trim();
        while (result.contains("  ")) result = result.replace("  ", " ");
        return result;
    }

    /**
     * 안전한 UriComponents 생성 (SSRF 방지 + 한글 인코딩 포함)
     * - query: 먼저 안전성 검사 및 truncate 한 뒤 URLEncoder로 인코딩하고 build(true) 사용
     */
    private static UriComponents buildSafeUri(String path, String rawQuery) {
        if (rawQuery == null) rawQuery = "";
        if (!isSafeQuery(rawQuery)) {
            throw new IllegalArgumentException("Unsafe query string detected");
        }

        String truncated = truncateQuery(rawQuery);
        log.debug("GeoUtil: truncated query='{}' ({} bytes)",
                truncated, truncated.getBytes(StandardCharsets.UTF_8).length);

        return UriComponentsBuilder
                .fromHttpUrl(KAKAO_DOMAIN)
                .path(path)
                .queryParam("query", truncated)  // 원본(잘린) 문자열 그대로 전달
                .build(false); // true = 이미 안전한 문자열로 간주
    }

    /**
     * address (도로명/주소) 검색 — analyze_type=similar 추가
     */
    private static double[] requestAddress(RestTemplate rt, HttpEntity<String> entity, String query) {
        try {
            UriComponents uc = buildSafeUri(KAKAO_ADDRESS_PATH, query);
            // analyze_type 파라미터는 원문(잘린 뒤)에 대해 추가 — encode 처리 동일하게 하려면 param 자체를 encoding해 넣음
            String encodedAnalyze = URLEncoder.encode("similar", StandardCharsets.UTF_8);
            String uri = uc.toUriString() + "&analyze_type=" + encodedAnalyze;

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) {
                log.warn("⚠️ Kakao 주소검색 응답 오류: {} (query={})", res.getStatusCode(), query);
                return new double[]{0.0, 0.0};
            }

            JSONObject json = new JSONObject(res.getBody());
            JSONArray docs = json.optJSONArray("documents");
            if (docs == null || docs.length() == 0) return new double[]{0.0, 0.0};

            JSONObject first = docs.getJSONObject(0);
            // Kakao returns x=lon, y=lat
            double lat = DistanceUtil.roundToTwoDecimalPlaces(first.getDouble("y"));
            double lon = DistanceUtil.roundToTwoDecimalPlaces(first.getDouble("x"));
            return new double[]{lat, lon};
        } catch (Exception e) {
            log.error("❌ Kakao 주소검색 중 예외 ({}): {}", query, e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }

    /**
     * keyword 검색 (키워드 검색 시에도 쿼리 잘라서 전달)
     */
    private static double[] requestKeyword(RestTemplate rt, HttpEntity<String> entity, String query) {
        try {
            UriComponents uc = buildSafeUri(KAKAO_KEYWORD_PATH, query);
            String uri = uc.toUriString();

            ResponseEntity<String> res = rt.exchange(uri, HttpMethod.GET, entity, String.class);
            if (res.getStatusCode() != HttpStatus.OK) {
                log.warn("⚠️ Kakao 키워드검색 응답 오류: {} (query={})", res.getStatusCode(), query);
                return new double[]{0.0, 0.0};
            }

            JSONObject json = new JSONObject(res.getBody());
            JSONArray docs = json.optJSONArray("documents");
            if (docs == null || docs.length() == 0) return new double[]{0.0, 0.0};

            JSONObject first = docs.getJSONObject(0);
            double lat = DistanceUtil.roundToTwoDecimalPlaces(first.getDouble("y"));
            double lon = DistanceUtil.roundToTwoDecimalPlaces(first.getDouble("x"));
            return new double[]{lat, lon};
        } catch (Exception e) {
            log.error("❌ Kakao 키워드검색 중 예외 ({}): {}", query, e.getMessage());
            return new double[]{0.0, 0.0};
        }
    }
}
