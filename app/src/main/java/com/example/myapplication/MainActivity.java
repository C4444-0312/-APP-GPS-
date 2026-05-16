package com.example.myapplication;

import android.content.Intent; // 用於啟動其他 Activity
import android.media.MediaPlayer; // 用於播放音訊
import android.os.Bundle; // Bundle 物件，用於在 Activity 之間傳遞數據，通常用於保存和恢復 Activity 狀態
import android.util.Log; // 用於輸出日誌
import android.view.View; // 用於處理 UI 視圖

import androidx.activity.EdgeToEdge; // 處理全螢幕顯示，使內容延伸到系統欄區域
import androidx.appcompat.app.AppCompatActivity; // AndroidX 庫提供的 Activity 基類，提供向後兼容性
import androidx.core.graphics.Insets; // 處理系統視窗邊緣的安全區域
import androidx.core.view.ViewCompat; // 處理視圖兼容性
import androidx.core.view.WindowInsetsCompat; // 處理視窗邊緣的安全區域
//遊戲標題頁面
public class MainActivity extends AppCompatActivity {

    MediaPlayer mp; // 宣告一個 MediaPlayer 物件，用於控制背景音樂

    @Override // 覆寫 AppCompatActivity 的 onCreate 方法，這是 Activity 生命周期中的第一個回調，用於初始化
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // 調用父類別的 onCreate 方法
        EdgeToEdge.enable(this); // 啟用全螢幕顯示，讓內容延伸到系統欄（例如狀態欄和導航欄）下方
        setContentView(R.layout.activity_main); // 設定 Activity 的佈局文件為 activity_main.xml

        // 設定視窗邊緣的安全區域監聽器：
        // 確保內容不會被系統UI（如狀態欄、導航欄）遮擋。
        // 當系統視窗邊緣安全區域發生變化時，會調整 start_btn 視圖的 padding。
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.start_btn), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化 MediaPlayer：
        // 從 res/raw 目錄載入 menu_bgm 音樂檔案，並創建 MediaPlayer 實例。
        mp = MediaPlayer.create(this, R.raw.menu_bgm);
        mp.setLooping(true); // 設定音樂無限循環播放，營造持續的菜單氛圍
        mp.start(); // 開始播放菜單背景音樂
    }

    @Override // 覆寫 Activity 的 onDestroy 方法，當 Activity 即將被銷毀時調用
    protected void onDestroy() {
        super.onDestroy(); // 調用父類別的 onDestroy 方法
        mp.stop(); // 停止音樂播放
        mp.release(); // 釋放 MediaPlayer 佔用的資源（例如記憶體、音頻解碼器），避免記憶體洩漏
        mp = null; // 將 mp 設為 null，幫助垃圾回收器回收對象
        Log.d("MainActivity", "onDestroy() called"); // 輸出日誌，表示 onDestroy 被調用
    }

    @Override // 覆寫 Activity 的 onResume 方法，當 Activity 從暫停狀態恢復或第一次啟動並準備與用戶互動時調用
    protected void onResume() {
        super.onResume(); // 調用父類別的 onResume 方法
        mp.start(); // 重新開始播放音樂（如果之前被暫停了）
        Log.d("MainActivity", "onResume() called"); // 輸出日誌，表示 onResume 被調用
    }

    // 當開始按鈕被點擊時調用，由 XML 佈局中的 android:onClick 屬性綁定
    public void startGame(View view) {
        // 建立一個 Intent：
        // 用於從當前 MainActivity 啟動 MainActivity2。
        Intent intent = new Intent(this, MainActivity2.class);
        mp.stop(); // 停止當前菜單的背景音樂，避免進入遊戲後兩種音樂同時播放
        startActivity(intent); // 啟動 MainActivity2
    }
}