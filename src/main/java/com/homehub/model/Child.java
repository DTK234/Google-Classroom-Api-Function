package com.homehub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "children")
public class Child {
    @Id
    private String id; // 例如: "child_01"
    private String name; // 子女名字
    private String status; // LOCKED / HOMEWORK_DONE

    @OneToOne(mappedBy = "child", cascade = CascadeType.ALL)
    private GoogleToken googleToken;

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public GoogleToken getGoogleToken() { return googleToken; }
    public void setGoogleToken(GoogleToken googleToken) { this.googleToken = googleToken; }
}