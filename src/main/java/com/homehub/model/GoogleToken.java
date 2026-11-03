package com.homehub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "google_tokens")
public class GoogleToken {

    @Id
    // ❌ 刪除原本的 @GeneratedValue(strategy = GenerationType.IDENTITY)
    
    // ✅ 改為手動賦值，或者使用 UUID 產生器
    private String id; // 也可以改用 UUID 當作主鍵

    // --- 請補上 id 的 Getter 與 Setter ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @OneToOne
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(length = 1000)
    private String accessToken;

    @Column(length = 1000)
    private String refreshToken;

    private Long expirationTimeMillis;

    // --- 確保有以下 Getter / Setter ---
    public Child getChild() { return child; }
    public void setChild(Child child) { this.child = child; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public Long getExpirationTimeMillis() { return expirationTimeMillis; }
    public void setExpirationTimeMillis(Long expirationTimeMillis) { this.expirationTimeMillis = expirationTimeMillis; }
}