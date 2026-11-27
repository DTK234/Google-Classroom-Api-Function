package com.homehub.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homehub.service.GoogleClassroomService;

@RestController
@RequestMapping("/api/classroom")
public class ClassroomController {

    @Autowired
    private GoogleClassroomService classroomService;

    // 1. 檢視指定小孩的「課程列表」
    @GetMapping("/courses/{childId}")
    public String getChildCourses(@PathVariable String childId) {
        return classroomService.fetchStudentCourses(childId);
    }

    // 2. 檢視指定小孩、指定課程 ID 的「作業清單」
    @GetMapping("/work/{childId}/{courseId}")
    public String getChildCourseWork(@PathVariable String childId, @PathVariable String courseId) {
        return classroomService.fetchCourseWork(childId, courseId);
    }

    // 3. 查詢指定作業 (或傳入 "-" 查詢整門課所有作業) 的完成情況
    @GetMapping("/submissions/{childId}/{courseId}/{courseWorkId}")
    public String getChildSubmissions(@PathVariable String childId,
                                      @PathVariable String courseId,
                                      @PathVariable String courseWorkId) {
        return classroomService.fetchStudentSubmissions(childId, courseId, courseWorkId);
    }

    // 4. 快捷路由：直接查詢指定課程「所有作業」的完成情況 (自動幫您帶入 "-")
    @GetMapping("/submissions/all/{childId}/{courseId}")
    public String getAllChildSubmissions(@PathVariable String childId, @PathVariable String courseId) {
        return classroomService.fetchStudentSubmissions(childId, courseId, "-");
    }

    // 5. 手機 App 專用：一次匯總該小孩所有課程與作業完成狀態
    @GetMapping("/summary/{childId}")
    public Map<String, Object> getHomeworkSummary(@PathVariable String childId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode coursesRoot = mapper.readTree(classroomService.fetchStudentCourses(childId));
        JsonNode coursesNode = coursesRoot.path("courses");

        List<Map<String, Object>> courseResults = new ArrayList<>();
        int totalCompleted = 0;
        int totalPending = 0;

        if (coursesNode.isArray()) {
            for (JsonNode course : coursesNode) {
                String courseId = course.path("id").asText();
                String courseName = course.path("name").asText("未命名課程");

                try {
                    JsonNode workRoot = mapper.readTree(classroomService.fetchCourseWork(childId, courseId));
                    JsonNode subRoot = mapper.readTree(classroomService.fetchStudentSubmissions(childId, courseId, "-"));

                    Map<String, JsonNode> subMap = new HashMap<>();
                    for (JsonNode sub : subRoot.path("studentSubmissions")) {
                        subMap.put(sub.path("courseWorkId").asText(), sub);
                    }

                    List<Map<String, Object>> assignments = new ArrayList<>();
                    for (JsonNode work : workRoot.path("courseWork")) {
                        String workId = work.path("id").asText();
                        JsonNode sub = subMap.get(workId);
                        String state = (sub != null) ? sub.path("state").asText("UNKNOWN") : "NEW";
                        boolean isDone = "TURNED_IN".equals(state) || "RETURNED".equals(state);

                        if (isDone) totalCompleted++; else totalPending++;

                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", workId);
                        item.put("title", work.path("title").asText("未命名作業"));
                        item.put("state", state);
                        item.put("completed", isDone);
                        item.put("grade", (sub != null && sub.has("assignedGrade")) ? sub.path("assignedGrade").asDouble() : null);
                        item.put("maxPoints", work.has("maxPoints") ? work.path("maxPoints").asDouble() : null);
                        item.put("updateTime", (sub != null) ? sub.path("updateTime").asText("") : "");
                        assignments.add(item);
                    }

                    Map<String, Object> courseItem = new LinkedHashMap<>();
                    courseItem.put("courseId", courseId);
                    courseItem.put("courseName", courseName);
                    courseItem.put("assignments", assignments);
                    courseResults.add(courseItem);
                } catch (Exception e) {
                    // 若某門課無權限（如老師身分課程），略過不影響其他課程
                    System.out.println("略過無權限課程: " + courseId);
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("childId", childId);
        response.put("totalCompleted", totalCompleted);
        response.put("totalPending", totalPending);
        response.put("allCompleted", totalPending == 0);
        response.put("courses", courseResults);
        return response;
    }
}