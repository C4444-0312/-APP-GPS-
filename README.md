本專案在獨立開發過程中，積極導入前沿生成式 AI 工具建立高效工作流（AIGC Pipeline），以優化視覺、聽覺體驗與程式碼品質：
- **🎨 視覺與圖片素材**：建構提示詞由 **Microsoft Copilot** 輔助生成與視覺風格設計。
- **🎵 背景音樂與音效**：運用 **Suno AI** 進行音樂編排與音訊流創作，營造沉浸式遊戲氛圍。
- **🤖 程式重構與優化**：全程由 **Google Gemini** 協助軟體架構設計、邏輯審查與複雜 Bug 排除，大幅縮短開發週期。


# 🗺️ Android 原生 GPS 定位互動放置型遊戲 App (-APP-GPS-)

本專案是一款結合**真實世界地理位置服務 (LBS, Location-Based Services)** 與**放置點擊冒險 (Idle/Clicker RPG)** 的原生 Android 應用程式。核心玩法將使用者的現實行走距離轉換為遊戲中的巨大傷害輸出，藉此鼓勵戶外活動與健康生活。專案全面採用 **Java** 進行原生開發，深度整合硬體定位 API、本地關閉式資料庫持久化，並設計了流暢的 UI 屬性動畫與嚴謹的記憶體生命週期管理。


# 🛠️ 開發環境與使用套件 (Tech Stack)
開發語言：Java (JDK 17+)

開發框架：原生 Android SDK (Min SDK API 24+, Target SDK API 34)

定位服務 API：Google Play Services Location (com.google.android.gms.location)

本地資料庫：SQLite (Android 內建架構)

UI 佈局技術：XML Layouts, EdgeToEdge 全螢幕沉浸式視窗安全邊緣設定 (ViewCompat.setOnApplyWindowInsetsListener)

# 🚀 執行與測試說明
將本專案導入至 Android Studio。

確保手機或模擬器已安裝 Google Play 服務。

執行應用程式後，系統會跳出提示請求精確定位權限 (ACCESS_FINE_LOCATION)，請選擇允許。

測試 GPS 攻擊：

實機測試：開啟 App 後攜帶手機在室外行走，每走幾公尺畫面上便會跳出橘色的巨大浮動傷害數字。

模擬器測試：可在 Android 模擬器的 Extended Controls ➔ Location 頁面中，手動載入 GPS 路徑軌跡 (GPX/KML 檔) 或設定變更經緯度點擊 Set Location，即可模擬行走並觸發遊戲內的自動 GPS 攻擊機制。



