package com.homehub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.homehub.model.GoogleToken;
import com.homehub.repository.GoogleTokenRepository;

@Service
public class GoogleClassroomService {

    @Autowired
    private GoogleTokenRepository tokenRepository;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    // 輔助方法：自動檢查 Access Token 是否過期，若過期則用 refresh_token 自動換新
    private String getValidAccessToken(String childId) {
        GoogleToken token = tokenRepository.findByChildId(childId)
                .orElseThrow(() -> new RuntimeException("該小孩尚未綁定 Google Classroom 帳號！"));

        // 檢查是否已經過期（或距離過期剩不到 60 秒）
        if (token.getExpirationTimeMillis() != null &&
                System.currentTimeMillis() > (token.getExpirationTimeMillis() - 60000)) {

            if (token.getRefreshToken() == null || token.getRefreshToken().isEmpty()) {
                throw new RuntimeException("Access Token 已過期且無 Refresh Token，請點擊右上角重新綁定授權！");
            }

            try {
                GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                        token.getRefreshToken(),
                        clientId,
                        clientSecret
                ).execute();

                token.setAccessToken(response.getAccessToken());
                token.setExpirationTimeMillis(System.currentTimeMillis() + response.getExpiresInSeconds() * 1000);
                tokenRepository.save(token);
                System.out.println("✅ 已自動為 " + childId + " 刷新 Google Access Token！");
            } catch (Exception e) {
                throw new RuntimeException("自動刷新 Token 失敗，請重新綁定授權: " + e.getMessage());
            }
        }

        return token.getAccessToken();
    }

    // 1. 抓取指定課程底下的作業清單 (CourseWork)
    public String fetchCourseWork(String childId, String courseId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getValidAccessToken(childId));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 帶入課程 ID 查詢該課程的作業
        String courseWorkUrl = "https://classroom.googleapis.com/v1/courses/" + courseId + "/courseWork";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    courseWorkUrl, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("呼叫 Google Classroom 作業 API 失敗: " + e.getMessage());
        }
    }

    // 2. 抓取學生的課程列表 (Courses)
    public String fetchStudentCourses(String childId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getValidAccessToken(childId));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String coursesUrl = "https://classroom.googleapis.com/v1/courses";
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    coursesUrl, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("呼叫 Google Classroom 課程 API 失敗: " + e.getMessage());
        }
    }

    // 3. 抓取指定課程與作業的學生繳交狀態 (StudentSubmissions)
    public String fetchStudentSubmissions(String childId, String courseId, String courseWorkId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getValidAccessToken(childId));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 明確指定 userId=me，代表只查詢該學生自己的繳交狀態
        // (courseWorkId 傳入 "892222427988" 或傳入 "-" 查整門課都可以！)
        String submissionUrl = "https://classroom.googleapis.com/v1/courses/" + courseId
                + "/courseWork/" + courseWorkId
                + "/studentSubmissions?userId=me";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    submissionUrl, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("呼叫 Google Classroom 繳交狀態 API 失敗: " + e.getMessage());
        }
    }
}