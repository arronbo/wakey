# 夢遊 Wakey - 社交鬧鐘 Android App

Kotlin + Jetpack Compose 製作的社交鬧鐘 App。

## 開發環境
- Android Studio Hedgehog 或以上
- JDK 17
- minSdk 26 / targetSdk 35

## 首次設定（每位開發者必做）

### 1. 取得 Firebase 設定檔
這個專案使用 Firebase Cloud Messaging。請依下列步驟取得 `google-services.json`：

1. 進入 [Firebase Console](https://console.firebase.google.com/) 並加入專案 `claude-final-3e35f`（或建立你自己的）
2. 註冊 Android App，package name 填 `com.wakey.app`
3. 下載 `google-services.json` 放到 `app/google-services.json`

> ⚠️ `google-services.json` 已加入 `.gitignore`，**不會** 被 push 到 GitHub。

### 2. 設定 FCM Service Account（推播功能）
打開 `app/src/main/java/com/wakey/app/data/remote/FcmSender.kt`，把下列三個變數填成你的 Firebase 專案資料：
- `projectId`
- `serviceAccountEmail`
- `privateKeyPem`

Service Account JSON 可在 Firebase Console → Project Settings → Service Accounts → Generate new private key 取得。

> ⚠️ 私鑰請勿 commit。

### 3. 同步 Gradle 並執行
在 Android Studio 開啟專案後按 **Sync Now**，然後選一台手機/模擬器執行。

## 必要權限
首次啟動會跳出：
- 通知權限（Android 13+）
- 鬧鐘與提醒權限（精確鬧鐘）
- 相機權限（加好友掃 QR 時）

## 主要功能
- 🏠 主頁：可走動的村莊世界，走到好友家門口可敲門喚醒
- ⏰ 鬧鐘：新增/編輯/共用，支援重複、自訂星期、震動、系統鈴聲
- 👥 好友：QR Code 配對，查看狀態
- 👨‍👩‍👧 群組：群組起床功能（FCM 推播）
- 👤 個人：頭像（含相片裁切）、留言、喚醒時段、深色模式、預設鬧鐘設定
