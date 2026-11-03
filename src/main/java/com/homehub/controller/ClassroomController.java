package com.homehub.controller;

import com.homehub.service.GoogleClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    // 3. 查詢指定作業（或傳入 "-" 查詢整門課所有作業）的完成情況
    @GetMapping("/submissions/{childId}/{courseId}/{courseWorkId}")
    public String getChildSubmissions(@PathVariable String childId, 
                                      @PathVariable String courseId, 
                                      @PathVariable String courseWorkId) {
        return classroomService.fetchStudentSubmissions(childId, courseId, courseWorkId);
    }

    // 4. 快捷路由：直接查詢指定課程「所有作業」的完成情況（自動幫您帶入 "-"）
    @GetMapping("/submissions/all/{childId}/{courseId}")
    public String getAllChildSubmissions(@PathVariable String childId, @PathVariable String courseId) {
        return classroomService.fetchStudentSubmissions(childId, courseId, "-");
    }
}