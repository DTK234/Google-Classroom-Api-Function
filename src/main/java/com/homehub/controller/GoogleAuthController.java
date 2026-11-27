package com.homehub.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.homehub.model.Child;
import com.homehub.model.GoogleToken;
import com.homehub.repository.ChildRepository;
import com.homehub.repository.GoogleTokenRepository;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    // 從 application.yml 自動讀取憑證
    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.client.redirect-uri}")
    private String redirectUri;

    @Autowired
    private GoogleTokenRepository tokenRepository;

    @Autowired
    private ChildRepository childRepository;

    /**
     * 1. 取得 Google 授權網址
     * 呼叫範例: GET /api/auth/url?childId=child_02
     */
    @GetMapping("/url")
    public String getAuthUrl(@RequestParam String childId) throws Exception {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientId,
                clientSecret,
                Arrays.asList(
                        "https://www.googleapis.com/auth/classroom.courses.readonly",
                        "https://www.googleapis.com/auth/classroom.coursework.me.readonly",
                        "https://www.googleapis.com/auth/classroom.student-submissions.me.readonly"
                )
        )
        .setAccessType("offline")
        .setApprovalPrompt("force")
        .build();

        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(childId) // 將子女 ID 夾帶在 state 中，方便回調時辨識
                .build();
    }

    /**
     * 2. Google 授權成功後的回調接口 (Callback)
     * 當家長/子女在 Google 登入並同意後，Google 會自動跳轉到這裡
     */
    @GetMapping("/callback")
    public String callback(@RequestParam String code, @RequestParam String state) throws Exception {
        // 1. 取得 state 傳過來的 childId
        String childId = state;

        // 2. 建立 GoogleAuthorizationCodeFlow 物件
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientId,
                clientSecret,
                Arrays.asList(
                        "https://www.googleapis.com/auth/classroom.courses.readonly",
                        "https://www.googleapis.com/auth/classroom.coursework.me.readonly",
                        "https://www.googleapis.com/auth/classroom.student-submissions.me.readonly"
                )
        )
        .setAccessType("offline")
        .setApprovalPrompt("force")
        .build();

        // 3. 從資料庫查找該兒童實體，若不存在（例如新輸入 child_02）則自動建立並儲存！
        Child child = childRepository.findById(childId)
                .orElseGet(() -> {
                    Child newChild = new Child();
                    newChild.setId(childId);
                    newChild.setName(childId);
                    newChild.setStatus("LOCKED");
                    return childRepository.save(newChild);
                });

        // 4. 透過 Google Flow 用 code 交換 token
        GoogleTokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        // 5. 先找看看資料庫有沒有該兒童舊的 Token，有就更新，沒有才新建
        GoogleToken token = tokenRepository.findByChildId(childId)
                .orElseGet(() -> {
                    GoogleToken newToken = new GoogleToken();
                    newToken.setId(java.util.UUID.randomUUID().toString());
                    newToken.setChild(child);
                    return newToken;
                });

        token.setAccessToken(response.getAccessToken());
        if (response.getRefreshToken() != null) {
            token.setRefreshToken(response.getRefreshToken());
        }
        token.setExpirationTimeMillis(System.currentTimeMillis() + response.getExpiresInSeconds() * 1000);

        // 儲存（更新）至資料庫
        tokenRepository.save(token);

        return "授權成功！已成功將 Google Classroom 綁定至兒童 ID: [" + childId + "]。請回到監控面板點擊「載入課程」。";
    }

    /**
     * 3. 取得目前已綁定的所有兒童清單（僅回傳安全欄位，避免雙向關聯序列化中斷）
     */
    @GetMapping("/children")
    public List<Map<String, Object>> getBoundChildren() {
        Map<String, Map<String, Object>> uniqueChildren = new HashMap<>();

        for (GoogleToken token : tokenRepository.findAll()) {
            Child c = token.getChild();
            if (c != null && c.getId() != null && !uniqueChildren.containsKey(c.getId())) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", c.getId());
                item.put("name", c.getName() != null ? c.getName() : c.getId());
                uniqueChildren.put(c.getId(), item);
            }
        }

        return new ArrayList<>(uniqueChildren.values());
    }

    /**
     * 4. 讓家長修改兒童的顯示名稱
     */
    @PutMapping("/children/{childId}/name")
    public Map<String, Object> updateChildName(@PathVariable String childId, @RequestParam String name) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("找不到對應的兒童 ID: " + childId));
        child.setName(name.trim());
        childRepository.save(child);

        Map<String, Object> result = new HashMap<>();
        result.put("id", child.getId());
        result.put("name", child.getName());
        return result;
    }

    /**
     * 5. 刪除指定的兒童及其綁定憑證
     * 呼叫範例: DELETE /api/auth/children/child_02
     */
    @DeleteMapping("/children/{childId}")
    public void deleteChild(@PathVariable String childId) {
        tokenRepository.findByChildId(childId).ifPresent(tokenRepository::delete);
        childRepository.findById(childId).ifPresent(childRepository::delete);
    }   
}