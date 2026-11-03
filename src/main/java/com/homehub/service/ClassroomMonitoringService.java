package com.homehub.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.homehub.model.Child;
import com.homehub.model.GoogleToken;
import com.homehub.repository.ChildRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassroomMonitoringService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Autowired
    private ChildRepository childRepository;

    @Scheduled(fixedRate = 900000)
    public void checkAllChildrenHomework() {
        List<Child> children = childRepository.findAll();

        for (Child child : children) {
            GoogleToken token = child.getGoogleToken();
            if (token == null || token.getRefreshToken() == null) {
                continue;
            }

            try {
                GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        token.getRefreshToken(),
                        clientId,
                        clientSecret)
                        .execute();

                String accessToken = tokenResponse.getAccessToken();
                // 接下來可繼續呼叫 Classroom API...

            } catch (Exception e) {
                System.err.println("檢查子女 [" + child.getName() + "] 功課失敗: " + e.getMessage());
            }
        }
    }
}