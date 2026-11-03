package com.homehub;

import com.homehub.model.Child;
import com.homehub.repository.ChildRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ClassroomHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClassroomHubApplication.class, args);
    }

    // 存檔後啟動專案時，自動寫入測試資料 child_01
    @Bean
    public CommandLineRunner initData(ChildRepository childRepository) {
        return args -> {
            if (!childRepository.existsById("child_01")) {
                Child child = new Child();
                child.setId("child_01"); // 若 ID 欄位叫 id 請確認型態
                child.setName("測試小孩"); // 若 Child 類別有 name 屬性
                childRepository.save(child);
                System.out.println(">>> 已成功寫入測試兒童資料：child_01 <<<");
            }
        };
    }
}