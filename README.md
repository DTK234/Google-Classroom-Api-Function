# Google Classroom API Function (HomeGuardian IoT)

這是一個基於 **Java Spring Boot**、**SQLite** 與 **Tailwind CSS** 開發的 Google Classroom 作業監控系統，專為家庭 IoT 家長監控主機設計，用於即時追蹤多位學生的課程清單、作業題目與功課繳交狀態。

## ✨ 核心功能
* **視覺化家長監控面板 (`index.html`)**：內建直覺的 Web Dashboard（`http://localhost:8080`），自動合併顯示課程列表、作業名稱、得分與完成狀態標籤。
* **多兒童獨立帳號管理**：透過 OAuth `state` 參數動態建立並綁定多位兒童（如 `child_01`、`child_02`），支援前端下拉選單一鍵切換檢視。
* **Access Token 自動續期**：後端自動檢查 Token 有效期限，過期前透過 `Refresh Token` 自動向 Google 換發新憑證並更新至 SQLite，免除每小時手動重登。
* **功課完成狀態精準追蹤**：串接 `courses`、`courseWork` 與 `studentSubmissions` 端點，即時統計「已完成」與「未完成」功課數量。

## 🔐 必備 OAuth 2.0 Scopes
* `https://www.googleapis.com/auth/classroom.courses.readonly`
* `https://www.googleapis.com/auth/classroom.coursework.me.readonly`
* `https://www.googleapis.com/auth/classroom.student-submissions.me.readonly`

## 📡 API 端點說明

### 1. 授權與兒童管理 (`GoogleAuthController`)
* **取得 OAuth 授權網址**：`GET /api/auth/url?childId={childId}`
* **OAuth 授權回調接口**：`GET /api/auth/callback?code={code}&state={childId}`
* **取得已綁定兒童清單**：`GET /api/auth/children`

### 2. 課程與作業查詢 (`ClassroomController`)
* **查詢兒童的修讀課程**：`GET /api/classroom/courses/{childId}`
* **查詢指定課程的作業清單**：`GET /api/classroom/work/{childId}/{courseId}`
* **查詢指定課程「所有作業」的繳交狀態**：`GET /api/classroom/submissions/all/{childId}/{courseId}`
* **查詢「單一作業」的繳交狀態**：`GET /api/classroom/submissions/{childId}/{courseId}/{courseWorkId}`

## 📊 作業繳交狀態 (`state`) 判斷標準
* **`TURNED_IN`**：✅ 學生已繳交（判定為已完成）
* **`RETURNED`**：🏆 老師已批改並發還（判定為已完成，支援讀取 `assignedGrade` 得分）
* **`NEW` / `CREATED`**：⏳ 尚未繳交（判定為未完成）
* **`RECLAIMED_BY_STUDENT`**：↩️ 學生取消繳交收回重做（判定為未完成）

## 🚀 快速啟動與操作流程
1. 在 `src/main/resources/application.yml` 設定 Google OAuth 憑證（`google.client.id`、`google.client.secret`、`google.client.redirect-uri`）。
2. 啟動 Spring Boot 應用程式，瀏覽器開啟 **`http://localhost:8080`**。
3. 在右上角輸入框輸入兒童 ID（如 `child_01` 或 `child_02`），點擊 **「🔗 綁定此 ID」** 完成 Google 授權。
4. 透過右上角 **「已綁定兒童」下拉選單** 切換不同子女，即時檢視各科作業完成進度。