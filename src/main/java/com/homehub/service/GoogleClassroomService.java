package com.homehub.service;

import com.homehub.model.GoogleToken;
import com.homehub.repository.GoogleTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleClassroomService {

    @Autowired
    private GoogleTokenRepository tokenRepository;

    // 1. 抓取指定課程底下的作業清單 (CourseWork)
    public String fetchCourseWork(String childId, String courseId) {
        GoogleToken token = tokenRepository.findByChildId(childId)
                .orElseThrow(() -> new RuntimeException("該小孩尚未綁定 Google Classroom 帳號！"));

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 帶入課程 ID 查詢該課程的作業
        String courseworkUrl = "https://classroom.googleapis.com/v1/courses/" + courseId + "/courseWork";
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    courseworkUrl, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("呼叫 Google Classroom 作業 API 失敗: " + e.getMessage());
        }
    }

    // 2. 原本的抓取課程列表維持不變
    public String fetchStudentCourses(String childId) {
        GoogleToken token = tokenRepository.findByChildId(childId)
                .orElseThrow(() -> new RuntimeException("該小孩尚未綁定 Google Classroom 帳號！"));

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
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
        GoogleToken token = tokenRepository.findByChildId(childId)
                .orElseThrow(() -> new RuntimeException("該小孩尚未綁定 Google Classroom 帳號！"));

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 明確指定 userId=me，代表只查詢該學生自己的繳交狀態
        // (courseWorkId 傳入 "89222427988" 或傳入 "-" 查整門課都可以！)
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