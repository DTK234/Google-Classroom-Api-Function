package com.homehub.repository;

import com.homehub.model.GoogleToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GoogleTokenRepository extends JpaRepository<GoogleToken, String> { // 假設主鍵型態是 String，若為 Long 請依您的設定調整

    // 【請補上這一行】讓 Spring Data JPA 自動根據 Child 的 ID 尋找對應的 Token
    Optional<GoogleToken> findByChildId(String childId);
}