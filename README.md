# Google Classroom API Function (HomeGuardian IoT)

這是一個基於 **Java Spring Boot**、**SQLite** 與 **Tailwind CSS** 開發的 Google Classroom 作業監控後端系統，提供 Web 家長監控面板與 Android App 專用 REST API，用於即時追蹤多位兒童的課程清單、作業題目與功課繳交狀態。

## ✨ 核心功能
* **多兒童表格化管理面板 (`index.html`)**：內建 Web Dashboard（`http://localhost:8080`），透過表格列出所有已綁定兒童，支援點擊切換檢視、一鍵新增綁定（自動編號）、修改顯示名稱、重新授權與刪除紀錄。
* **Android App 專用匯總 API**：提供 `/api/classroom/summary/{childId}`，自動合併所有課程、作業與繳交狀態並計算完成總數，供手機端一次讀取。
* **Access Token 自動續期**：後端自動檢查憑證效期，過期前透過 `Refresh Token` 自動向 Google 換發新憑證並更新至 SQLite。
* **功課完成狀態精準追蹤**：串接 `courses`、`courseWork` 與 `studentSubmissions`，自動略過無學生權限的課程（防止 403 中斷）。

## 🔐 必備 OAuth 2.0 Scopes
* `[https://www.googleapis.com/auth/classroom.courses.readonly](https://www.googleapis.com/auth/classroom.courses.readonly)`
* `[https://www.googleapis.com/auth/classroom.coursework.me.readonly](https://www.googleapis.com/auth/classroom.coursework.me.readonly)`
* `[https://www.googleapis.com/auth/classroom.student-submissions.me.readonly](https://www.googleapis.com/auth/classroom.student-submissions.me.readonly)`

## 📡 API 端點說明

### 1. 授權與兒童管理 (`GoogleAuthController`)
* **取得已綁定兒童清單**：`GET /api/auth/children`
  ```json
  [
    { "id": "child_01", "name": "大B" },
    { "id": "child_02", "name": "細B" }
  ]
  ```
* **取得 OAuth 授權網址**：`GET /api/auth/url?childId={childId}`
* **OAuth 授權回調接口**：`GET /api/auth/callback?code={code}&state={childId}`
* **修改兒童顯示名稱**：`PUT /api/auth/children/{childId}/name?name={newName}`
* **刪除兒童及綁定紀錄**：`DELETE /api/auth/children/{childId}`

### 2. 課程與作業查詢 (`ClassroomController`)
* **📱 手機 App 專用功課總覽（推薦）**：`GET /api/classroom/summary/{childId}`
  ```json
  {
    "childId": "child_02",
    "totalCompleted": 1,
    "totalPending": 2,
    "allCompleted": false,
    "courses": [
      {
        "courseId": "892222575388",
        "courseName": "Game",
        "assignments": [
          {
            "id": "892222427988",
            "title": "Returned",
            "state": "RETURNED",
            "completed": true,
            "grade": 100.0,
            "maxPoints": 100.0,
            "updateTime": "2026-11-26T12:30:00Z"
          }
        ]
      }
    ]
  }
  ```
* **查詢兒童的修讀課程**：`GET /api/classroom/courses/{childId}`
* **查詢指定課程的作業清單**：`GET /api/classroom/work/{childId}/{courseId}`
* **查詢指定課程「所有作業」的繳交狀態**：`GET /api/classroom/submissions/all/{childId}/{courseId}`
* **查詢「單一作業」的繳交狀態**：`GET /api/classroom/submissions/{childId}/{courseId}/{courseWorkId}`

## 📱 Android Studio 串接須知
* **Base URL 設定**：
  * 同機 Android 模擬器：`[http://10.0.2.2:8080/](http://10.0.2.2:8080/)`
  * 同 Wi-Fi 實體手機：`http://<主機內網IP>:8080/`（需於 `AndroidManifest.xml` 開啟 `android:usesCleartextTraffic="true"`）
  * 遠端 ngrok 測試：`https://<你的網址>.ngrok-free.dev/`（免費版需於 Request Header 加入 `ngrok-skip-browser-warning: true`）

## 📊 作業繳交狀態 (`state`) 判斷標準
* **`TURNED_IN`**：✅ 學生已繳交（`completed: true`）
* **`RETURNED`**：🏆 老師已批改並發還（`completed: true`，含 `grade` 得分）
* **`NEW` / `CREATED`**：⏳ 尚未繳交（`completed: false`）
* **`RECLAIMED_BY_STUDENT`**：↩️ 學生收回重做（`completed: false`）

## 🚀 快速啟動與操作流程
1. 在 `src/main/resources/application.yml` 設定 Google OAuth 憑證（`google.client.id`、`google.client.secret`、`google.client.redirect-uri`）。
2. 啟動 Spring Boot，瀏覽器開啟 **`http://localhost:8080`**。
3. 點擊右上角 **「➕ 新增兒童綁定」** 完成 Google 授權，之後即可直接在兒童表格點擊切換、改名、重綁或刪除。