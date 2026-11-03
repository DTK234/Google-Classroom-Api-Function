# HomeHub - 家長監控系統 (Google Classroom 功課追蹤模組)

HomeHub 是一個專為家長設計的邊緣運算（IoT 主機）監控系統後端。本模組透過串接 **Google Classroom REST API** 與 **OAuth 2.0**，讓家長能夠即時查詢子女在學校的課程清單、作業題目以及功課繳交狀態（例如：是否已交齊功課、老師是否已批改發還），並將授權憑證安全地儲存於本地 SQLite 資料庫中。

---

## 技術架構 (Tech Stack)

* **語言環境**：Java 17
* **後端框架**：Spring Boot 3.2.0 (Spring Web, Spring Data JPA)
* **資料庫**：SQLite (`homehub.db`) + Hibernate ORM
* **外部整合**：Google API Client (`google-api-client`)、Google Classroom REST API v1

---

## 核心功能

1. **Google OAuth 2.0 帳號綁定**：產生專屬授權網址，支援 `offline` 模式取得 `refresh_token`，並自動綁定或更新指定子女（如 `child_01`）的憑證資料。
2. **課程列表查詢 (Courses)**：讀取學生帳號所選修的所有 Google Classroom 課程。
3. **作業清單查詢 (CourseWork)**：讀取指定課程底下發布的所有作業題目、配分上限（`maxPoints`）與狀態。
4. **功課繳交狀態追蹤 (StudentSubmissions)**：支援查詢單一作業或一次性查詢整門課程所有作業的完成進度（`TURNED_IN`、`RETURNED`、`CREATED` 等）與獲得分數。

---

## 前置設定 (Google Cloud Console)

在啟動專案前，請確認已在 [Google Cloud Console](https://console.cloud.google.com/) 完成以下設定：

1. **啟用 API**：於「API 和服務」中啟用 **Google Classroom API**。
2. **設定 OAuth 同意畫面**：新增以下 3 個必要的權限範圍（Scopes）：
   * `https://www.googleapis.com/auth/classroom.courses.readonly`（讀取課程列表）
   * `https://www.googleapis.com/auth/classroom.coursework.me.readonly`（讀取學生自己的作業）
   * `https://www.googleapis.com/auth/classroom.student-submissions.me.readonly`（讀取學生自己的繳交狀態）
3. **建立憑證 (OAuth 2.0 用戶端 ID)**：
   * 應用程式類型選擇「網頁應用程式」。
   * 在「已授權的重新導向 URI (Authorized redirect URIs)」中加入：
     `http://localhost:8080/api/auth/callback`（或您的 ngrok 測試網址）。

---

## 環境變數與設定檔 (`application.yml`)

請確認 `src/main/resources/application.yml` 包含以下設定（請替換為您自己的憑證資訊）：

```yaml
spring:
  datasource:
    url: jdbc:sqlite:homehub.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: update
    show-sql: true

google:
  client:
    id: YOUR_GOOGLE_CLIENT_ID
    secret: YOUR_GOOGLE_CLIENT_SECRET
    redirect-uri: http://localhost:8080/api/auth/callback
```

---

## API 端點說明與使用流程

### 1. 帳號授權與綁定 (`GoogleAuthController`)

| 方法 | 路徑 | 說明 |
| :--- | :--- | :--- |
| `GET` | `/api/auth/url?childId={childId}` | 取得 Google OAuth 2.0 授權登入網址 |
| `GET` | `/api/auth/callback` | Google 授權成功後的自動回調接口（自動寫入/更新 SQLite） |

* **測試範例**：
  1. 瀏覽器訪問：`http://localhost:8080/api/auth/url?childId=child_01`
  2. 複製回傳的網址至瀏覽器並使用**學生 Google 帳號**登入同意授權。
  3. 畫面顯示 `授權成功！已成功綁定 Google Classroom。` 即完成綁定。

---

### 2. 課程與作業查詢 (`ClassroomController`)

| 方法 | 路徑 | 說明 |
| :--- | :--- | :--- |
| `GET` | `/api/classroom/courses/{childId}` | 取得指定子女的所有課程列表（可從中取得 12 位數的 `courseId`） |
| `GET` | `/api/classroom/work/{childId}/{courseId}` | 取得指定課程底下的所有作業題目與配分（可從中取得 `courseWorkId`） |
| `GET` | `/api/classroom/submissions/all/{childId}/{courseId}` | **（推薦）** 一次取得指定課程中「所有作業」的繳交與完成情況 |
| `GET` | `/api/classroom/submissions/{childId}/{courseId}/{courseWorkId}` | 取得指定單一作業的繳交與完成情況 |

* **實際呼叫範例**：
  * **查課程**：`http://localhost:8080/api/classroom/courses/child_01`
  * **查作業題目**：`http://localhost:8080/api/classroom/work/child_01/892222575388`
  * **查整門課所有作業完成狀態**：`http://localhost:8080/api/classroom/submissions/all/child_01/892222575388`
  * **查單一作業完成狀態**：`http://localhost:8080/api/classroom/submissions/child_01/892222575388/892222427988`

---

## 作業繳交狀態 (`state`) 判斷指南

當呼叫 `/submissions/...` 端點時，可根據回傳 JSON 中 `studentSubmissions` 陣列內的 `state` 欄位來判斷子女是否已完成功課：

```json
{
  "studentSubmissions": [
    {
      "courseId": "892222575388",
      "courseWorkId": "896645660792",
      "state": "TURNED_IN",
      "courseWorkType": "ASSIGNMENT"
    },
    {
      "courseId": "892222575388",
      "courseWorkId": "892222427988",
      "state": "RETURNED",
      "assignedGrade": 100,
      "courseWorkType": "ASSIGNMENT"
    }
  ]
}
```

| `state` 狀態值 | 代表意義 | 功課完成判定 |
| :--- | :--- | :--- |
| **`TURNED_IN`** | 學生已點擊「繳交」作業 | ✅ **已完成** |
| **`RETURNED`** | 老師已批改並發還作業（可搭配 `assignedGrade` 查看分數） | ✅ **已完成** |
| **`NEW`** | 作業剛發布，學生尚未點開過 | ❌ **未完成** |
| **`CREATED`** | 學生已點開或建立作業附件，但**尚未點擊繳交** | ❌ **未完成** |
| **`RECLAIMED_BY_STUDENT`** | 學生取消繳交並收回作業重新修改中 | ❌ **未完成** |

---

## 常見問題排查 (Troubleshooting)

* **遇到 `404 Not Found: Requested entity was not found.`**：
  1. 請檢查網址中的 `courseId` 與 `courseWorkId` 位數是否完全正確（Google Classroom ID 通常為 12 位數，請直接從 `/courses` 或 `/work` 回傳的 JSON 中複製 `id` 欄位）。
  2. 確認綁定授權的 Google 帳號確實為選修該門課程的**學生**，且查詢網址結尾帶有 `?userId=me`。
  3. 若曾修改過 OAuth Scope，請先刪除舊的 `homehub.db` 或重新執行 `/api/auth/url` 授權流程以更新 Token。
* **遇到 `ERR_NGROK_3200`**：
  * 若在本地電腦測試，無須開啟 ngrok，請將 `application.yml` 中的 `redirect-uri` 改回 `http://localhost:8080/api/auth/callback`，或在跳轉失敗時手動將瀏覽器網址列開頭替換為 `http://localhost:8080`。