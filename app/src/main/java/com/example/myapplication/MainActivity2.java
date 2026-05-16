package com.example.myapplication;

import android.Manifest; // 引入 Android 權限相關的類別
import android.animation.Animator; // 動畫介面
import android.animation.AnimatorListenerAdapter; // 動畫監聽器適配器，用於簡化動畫事件處理
import android.animation.AnimatorSet; // 用於組合多個動畫
import android.animation.ObjectAnimator; // 用於物件屬性動畫
import android.app.AlertDialog; // 警示對話框
import android.content.ContentValues; // 用於資料庫插入或更新操作，類似於 HashMap
import android.content.DialogInterface; // 對話框介面，用於處理對話框按鈕點擊
import android.content.pm.PackageManager; // 用於檢查權限狀態

import android.database.Cursor; // 資料庫查詢結果集
import android.database.sqlite.SQLiteDatabase; // SQLite 資料庫核心類別
import android.graphics.Color; // 顏色定義
import android.graphics.Typeface; // 字體樣式
import android.location.Location; // 位置數據物件
import android.media.MediaPlayer; // 媒體播放器
import android.os.Build; // 判斷 Android 版本
import android.os.Bundle; // Bundle 物件
import android.os.Looper; // 用於 Looper 類別，通常與處理程序和執行緒相關
import android.util.Log; // 日誌輸出
import android.util.TypedValue; // 轉換尺寸單位
import android.view.View; // UI 視圖
import android.view.ViewGroup; // 視圖組，用於佈局
import android.view.animation.AccelerateDecelerateInterpolator; // 動畫插值器，控制動畫速度變化
import android.widget.FrameLayout; // 佈局容器
import android.widget.ImageButton; // 圖片按鈕
import android.widget.ImageView; // 圖片視圖
import android.widget.TextView; // 文字視圖
import android.widget.Toast; // 輕量級提示訊息


import androidx.activity.EdgeToEdge; // 處理全螢幕顯示
import androidx.annotation.NonNull; // 註解，表示參數或返回值不能為 null
import androidx.appcompat.app.AppCompatActivity; // Activity 基類
import androidx.core.app.ActivityCompat; // AndroidX 權限兼容性助手
import androidx.core.content.ContextCompat; // AndroidX 上下文兼容性助手
import androidx.core.graphics.Insets; // 視窗邊緣安全區域
import androidx.core.view.ViewCompat; // 視圖兼容性助手
import androidx.core.view.WindowInsetsCompat; // 視窗邊緣安全區域

import com.google.android.gms.location.*;//引入 Google Play 服務 (Google Play Services) 中與定位 (Location) 相關的所有類別

// 遊戲畫面 Activity
public class MainActivity2 extends AppCompatActivity {
    // 常數定義
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1; // 請求位置權限的唯一識別碼
    private static final String TABLE_M = "monster"; // 怪物資料表名稱
    private static final String TABLE_P = "player"; // 玩家資料表名稱
    private static final long HIT_EFFECT_DURATION = 100; // 怪物受擊特效的持續時間 (毫秒)
    private Runnable onDeadEffectFinished = null; // 在死亡動畫結束後需要執行的回調邏輯
    private boolean onDeadEffect = false; // 標誌，指示是否正在播放怪物死亡動畫
    private static final long DEAD_EFFECT_DURATION = 1000; // 怪物死亡特效的持續時間 (毫秒)

    // 資料庫相關物件
    private SQLiteDatabase db; // SQLite 資料庫實例
    private GameDBHelper dbHelper; // 資料庫助手物件，用於管理資料庫創建和升級

    // 定位服務相關物件
    private FusedLocationProviderClient fusedLocationClient; // Google Play 服務的融合定位客戶端
    private LocationRequest locationRequest; // 定位請求的配置（頻率、精度等）
    private LocationCallback locationCallback; // 接收位置更新的回調
    private Location lastLocation; // 儲存上一個位置數據，用於計算距離
    private boolean isLocationUpdatesStarted = false; // 標誌，指示位置更新是否已經啟動
    private double totalDistance = 0; // 玩家累積的總移動距離 (公尺)
    private float distance = 0; // 上一個位置與當前位置之間的距離差 (公尺)

    private MediaPlayer mp; // 媒體播放器，用於遊戲內背景音樂

    // UI 元件：
    // TextViews：用於顯示遊戲數據
    private TextView txtCoin, txtAtk, txtStage, txtMileage, txtMonsterHp, txtAtkUpCoin, txtMonsterName;
    private ImageView imgMonster; // 怪物圖片
    private ImageButton btnAtkUp; // 攻擊力升級按鈕
    private ImageButton btnAtk; // 手動攻擊按鈕
    private FrameLayout damageTextContainer; // 用於顯示浮動傷害數字的容器

    // 遊戲數據與邏輯物件
    private byte monsterId; // 當前怪物的 ID (對應資源陣列中的索引)
    private byte playerId = 1; // 玩家的唯一 ID (預設為 1)
    public Game game; // 遊戲邏輯物件，包含所有遊戲數據和規則

    @Override // 覆寫 Activity 的 onCreate 方法，遊戲 Activity 的初始化入口
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // 啟用全螢幕顯示
        setContentView(R.layout.activity_main2); // 設定佈局文件為 activity_main2.xml

        // 設定視窗邊緣安全區域監聽器（與 MainActivity 類似）
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.start_btn), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Log.d("MainActivity", "onCreate() called"); // 日誌記錄 onCreate 調用

        // 初始化並啟動背景音樂
        mp = MediaPlayer.create(this, R.raw.in_game_bgm); // 載入遊戲背景音樂
        mp.setLooping(true); // 設定為循環播放
        mp.start(); // 開始播放音樂

        // 初始化資料庫
        dbHelper = new GameDBHelper(this); // 創建資料庫助手實例
        db = dbHelper.getWritableDatabase(); // 獲取可寫入的資料庫連接
        dataRead(); // 從資料庫讀取遊戲數據，恢復遊戲狀態

        // 初始化 Google Play 服務的融合定位客戶端
        // 用於獲取高精準度且優化電池使用的設備位置資訊
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 綁定所有 UI 元件到 Java 物件，以便在程式碼中操作它們
        txtCoin = findViewById(R.id.coin);
        txtAtk = findViewById(R.id.atk);
        txtAtkUpCoin = findViewById(R.id.up_coin);
        txtStage = findViewById(R.id.stage);
        txtMileage = findViewById(R.id.mileage);
        txtMonsterHp = findViewById(R.id.monster_hp);
        txtMonsterName = findViewById(R.id.monster_name);
        imgMonster = (ImageView) findViewById(R.id.monster);
        btnAtkUp = (ImageButton) findViewById(R.id.atkUpBtn);
        btnAtk = (ImageButton) findViewById(R.id.atkBtn);
        damageTextContainer = findViewById(R.id.demageFrame); // 受傷特效容器

        updateDisplay(); // 初始刷新 UI 顯示，根據讀取到的數據更新介面
        createLocationRequest(); // 配置位置請求的參數

        // 初始化 LocationCallback 物件：
        // 這個回調介面用於接收來自 Fused Location Provider 的位置更新。
        // 當設備位置發生變化時，系統會調用其 onLocationResult 方法。
        locationCallback = new LocationCallback() {
            @Override // 當 Fused Location Provider 傳回新的位置數據時調用此方法，@NonNull 放在參數前面，參數傳遞的值不能為空
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Log.d("LocationTracker", "Location result received.");
                super.onLocationResult(locationResult);

                // 遍歷所有收到的位置更新：
                // Fused Location Provider 可能會一次性提供多個位置數據點（批處理），
                // 這裡逐一處理每個 Location 物件，確保即使在批處理模式下也能處理所有數據。
                for (Location location : locationResult.getLocations()) {

                    // 檢查每個 Location 物件是否為空，以防止潛在的 NullPointerException。
                    if (location != null) {

                        // 使用當前 Location 數據計算玩家的移動距離：
                        // 這會更新遊戲內部累積的移動距離（例如，玩家在現實世界中行走的距離）。
                        calculateDistance(location);

                        // 根據累積的移動距離觸發遊戲內的 GPS 攻擊或其他相關事件：
                        // 這裡的 'distance' 變數應該是 calculateDistance 更新後，
                        // 代表玩家已移動的總距離或達到觸發攻擊所需的特定距離閾值。
                        atk(distance);
                    } else {
                        // 如果接收到的 Location 物件為空，記錄一個錯誤，以便調試。
                        Log.e("LocationTracker", "Location is null in the result.");
                    }
                }
            }
        };
        // 檢查並請求位置權限，並在獲得權限後開始位置更新
        checkLocationPermission();
    }

    @Override // 覆寫 Activity 的 onPause 方法，當 Activity 暫時失去焦點時調用
    protected void onPause() {
        super.onPause();
        mp.pause(); // 暫停背景音樂播放
        dataWrite(); // 保存遊戲數據到資料庫
    }

    @Override // 覆寫 Activity 的 onResume 方法，當 Activity 從暫停狀態恢復時調用
    protected void onResume() {
        super.onResume();
        mp.start(); // 恢復背景音樂播放
        db = dbHelper.getWritableDatabase(); // 重新獲取資料庫連接，確保可用
        checkLocationPermission(); // 檢查位置權限並嘗試啟動位置更新
        updateDisplay(); // 刷新 UI 顯示
    }

    @Override // 覆寫 Activity 的 onStop 方法，當 Activity 完全不可見時調用
    protected void onStop() {
        super.onStop();
        dataWrite(); // 保存遊戲數據到資料庫
        stopLocationUpdates(); // 停止位置更新，節省電池
        mp.stop(); // 停止音樂播放
    }

    @Override // 覆寫 Activity 的 onDestroy 方法，當 Activity 即將被銷毀時調用
    protected void onDestroy() {
        super.onDestroy();
        dataWrite(); // 最後一次保存遊戲數據
        stopLocationUpdates(); // 停止位置更新
        mp.stop(); // 停止音樂播放
        mp.release(); // 釋放 MediaPlayer 資源
        mp = null; // 將 MediaPlayer 設為 null
        db.close(); // 關閉資料庫連接，釋放資源
    }

    // 當用戶按下返回鍵時調用，防止誤觸直接就離開遊戲
    @Override
    public void onBackPressed() {
        // 建立一個 AlertDialog.Builder 實例
        new AlertDialog.Builder(this)
                // 設定提示框標題，從多國語言資源中獲取
                .setTitle(getResources().getStringArray(R.array.exit_prompt)[0])
                // 設定提示訊息，從多國語言資源中獲取
                .setMessage(getResources().getStringArray(R.array.exit_prompt)[1])
                // 設定「是」（Positive）按鈕及其點擊事件監聽器
                .setPositiveButton(getResources().getStringArray(R.array.exit_prompt)[2], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 用戶點擊「是」按鈕時，呼叫父類別的 onBackPressed 方法，執行返回操作（即退出當前 Activity）
                        MainActivity2.super.onBackPressed();
                    }
                })
                // 設定「否」（Negative）按鈕及其點擊事件監聽器
                .setNegativeButton(getResources().getStringArray(R.array.exit_prompt)[3], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 用戶點擊「否」按鈕時，不做任何操作，關閉對話框
                        dialog.dismiss();
                    }
                })
                .setCancelable(false) // 設定為不可取消（點擊對話框外部或返回鍵不會關閉對話框），強制用戶選擇
                .show(); // 顯示對話框
    }

    // 從資料庫讀取遊戲數據
    private void dataRead() {
        int stage;
        long monsterHp, preMonsterHp, atk, atkLevel, atkUpCoin, coin;
        Cursor c = null; // 宣告一個 Cursor 物件並初始化為 null，用於儲存資料庫查詢結果
        try {
            // 查詢 player 資料表，獲取指定玩家 ID 的數據
            c = db.query(TABLE_P, null, "id = ?", new String[]{String.valueOf(playerId)}, null, null, null);
            if (!c.moveToFirst()) { // 如果 Cursor 為空或無法移動到第一行（表示沒有玩家數據）
                game = new Game(); // 創建一個新的 Game 物件，初始化遊戲狀態
                dataWrite(); // 將新的預設遊戲數據寫入資料庫
                c.close(); // 關閉 Cursor
                try {
                    // 嘗試向 player 表格插入新的玩家記錄
                    ContentValues cv = new ContentValues();
                    cv.put("id", playerId);
                    db.insert(TABLE_P, null, cv);
                    Log.d("dataRead", "玩家資料建立成功");
                } catch (Exception e) {
                    Log.e("dataRead", "玩家資料建立失敗", e); // 記錄錯誤
                }
                try {
                    // 嘗試向 monster 表格插入新的怪物記錄
                    ContentValues cv = new ContentValues();
                    cv.put("id", playerId); // 這裡使用 playerId 作為 monster 表的 id，專屬playerId的怪物資料
                    db.insert(TABLE_M, null, cv);
                    Log.d("dataRead", "怪物資料建立成功");
                } catch (Exception e) {
                    Log.e("dataRead", "怪物資料建立失敗", e); // 記錄錯誤
                }
                return; // 數據讀取失敗或新數據建立完成，退出方法
            }
            // 已經有玩家數據，從 Cursor 中讀取玩家數據
            stage = c.getInt((int) c.getColumnIndex("stage"));
            atk = c.getLong((int) c.getColumnIndex("atk"));
            atkLevel = c.getLong((int) c.getColumnIndex("atkLevel"));
            atkUpCoin = c.getLong((int) c.getColumnIndex("atkUpCoin"));
            coin = c.getLong((int) c.getColumnIndex("coin"));
            totalDistance = c.getDouble((int) c.getColumnIndex("totalDistance"));
            Log.d("dataRead", "玩家資料讀取成功");

            try {
                // 查詢 monster 資料表，讀取怪物相關數據
                c = db.query(TABLE_M, null, null, null, null, null, null);
                c.moveToFirst(); // 移動到第一行（假設只有一條怪物記錄）
                monsterId = (byte) c.getInt((int) c.getColumnIndex("lastMonsterId"));
                monsterHp = c.getLong((int) c.getColumnIndex("monsterHpLeft"));
                preMonsterHp = c.getLong((int) c.getColumnIndex("preMonsterHp"));
                // 使用讀取到的數據恢復 Game 物件的狀態
                game = new Game(stage, monsterHp, preMonsterHp, atk, atkLevel, atkUpCoin, coin);
                Log.d("dataRead", "怪物資料讀取成功");
            } catch (Exception e) {
                c.close(); // 發生錯誤時關閉 Cursor
                Log.e("dataRead", "怪物資料讀取失敗", e);
                return;
            }
        } catch (Exception e) {
            // 處理玩家資料讀取失敗的情況
            c.close(); // 發生錯誤時關閉 Cursor
            Log.e("dataRead", "玩家資料讀取失敗", e);
            return;
        }
        c.close(); // 無論成功與否，最終確保 Cursor 被關閉
    }

    // 將遊戲數據寫入資料庫
    private void dataWrite() {
        ContentValues cv = new ContentValues(); // 創建 ContentValues 物件用於數據更新

        // 準備玩家數據
        cv.put("coin", game.getRealCoin());
        cv.put("stage", game.getRealStage());
        cv.put("atk", game.getRealAtk());
        cv.put("atkLevel", game.getRealAtkLevel());
        cv.put("atkUpCoin", game.getRealAtkUpCoin());
        cv.put("totalDistance", totalDistance);
        try {
            // 更新 player 資料表，根據 id = 1
            db.update(TABLE_P, cv, "id = ?", new String[]{"1"});
            Log.d("dataWrite", "玩家資料寫入成功");
        } catch (Exception e) {
            Log.e("dataWrite", "玩家資料寫入失敗", e);
        }

        cv.clear(); // 清空 ContentValues 以用於下一個表格

        // 準備怪物數據
        cv.put("monsterHpLeft", game.getRealMonsterHp());
        cv.put("preMonsterHp", game.getRealPreMonsterHp());
        cv.put("lastMonsterId", monsterId);
        try {
            // 更新 monster 資料表，根據 id = 1
            db.update(TABLE_M, cv, "id = ?", new String[]{"1"});
            Log.d("dataWrite", "怪物資料寫入成功");
        } catch (Exception e) {
            Log.e("dataWrite", "怪物資料寫入失敗", e);
        }
    }

    // 檢查位置權限並請求
    private void checkLocationPermission() {
        // 檢查是否已授予 ACCESS_FINE_LOCATION 權限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 如果沒有權限，則向用戶請求權限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, // 請求精確位置權限
                    LOCATION_PERMISSION_REQUEST_CODE); // 使用定義的識別碼
        } else {
            // 權限已被授予
            if (isLocationUpdatesStarted) { // 檢查位置更新是否已啟動
                Log.d("LocationTracker", "位置開始更新，不須再註冊");
                return; // 如果已啟動，直接返回
            }
            startLocationUpdates(); // 啟動位置更新
            isLocationUpdatesStarted = true; // 設定標誌為已啟動
        }
    }

    // 建立位置請求配置
    private void createLocationRequest() {
        // 檢查 Android 版本是否為 API 31 (Android 12) 或更高，因為 LocationRequest 構造方式有變化
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 使用 LocationRequest.Builder 創建高精度、每 5 秒更新一次的請求
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                    .setMinUpdateIntervalMillis(5000L) // 最小更新間隔也是 5 秒
                    .build();
            Log.d("LocationTracker", "Location request created.");
        } else {
            // 對於舊版本 Android，可能需要不同的 LocationRequest 構造方式，這裡簡單記錄錯誤
            Log.e("LocationTracker", "Location request failed or not implemented for this Android version.");
        }
    }


    @Override // 覆寫 Activity 的 onRequestPermissionsResult 方法，處理權限請求的結果
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 檢查是否是我們之前發出的位置權限請求
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // 檢查授權結果：grantResults 陣列長度大於 0 且第一個元素為 PERMISSION_GRANTED
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 權限已授予，開始請求位置更新
                startLocationUpdates();
                isLocationUpdatesStarted = true;
            } else {
                // 權限被拒絕，顯示警告訊息並記錄日誌
                Toast.makeText(this, getResources().getString(R.string.Permission_warn), Toast.LENGTH_LONG).show();
                Log.e("LocationTracker", "Location permission denied.");
            }
        }
    }

    // 啟動位置更新
    private void startLocationUpdates() {
        try {
            // 在請求位置更新前再次檢查權限，因為用戶可能在運行時撤銷權限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("LocationTracker", "No location permission");
                return; // 如果沒有權限，則退出方法
            }
            // 向 FusedLocationProviderClient 請求位置更新
            // locationRequest: 定義更新頻率和精度
            // locationCallback: 接收位置數據的回調
            // Looper.getMainLooper(): 在主執行緒（UI 執行緒）上接收位置更新
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d("LocationTracker", "Location updates started.");
        } catch (SecurityException e) {
            // 捕獲 SecurityException，通常發生在缺少權限但程式碼嘗試請求時
            Log.e("LocationTracker", "SecurityException: " + e.getMessage(), e);
        }
    }

    // 停止位置更新
    private void stopLocationUpdates() {
        // 從 FusedLocationProviderClient 移除位置更新回調，停止接收位置數據
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isLocationUpdatesStarted = false; // 重設標誌
        Log.d("LocationTracker", "位置更新關閉");
    }

    // 計算玩家移動距離
    private void calculateDistance(Location newLocation) {
        if (lastLocation != null) { // 確保有上一個位置數據才能計算距離
            distance = lastLocation.distanceTo(newLocation); // 計算兩個位置點之間的距離 (公尺)
            totalDistance += distance; // 將當前距離累加到總距離
            Log.d("LocationTracker", "距離上一個點: " + distance + "m, 總距離: " + totalDistance + "m");
        }
        lastLocation = newLocation; // 更新 lastLocation 為當前新位置，為下一次計算做準備
    }

    // 播放怪物受擊特效
    private void showHitEffect() {
        // 如果怪物死亡動畫正在播放，或者怪物圖片物件為空，則不播放受擊特效
        if (onDeadEffect || imgMonster == null) {
            Log.d("HitEffect", "跳過受擊特效，死亡動畫播放中或圖片未初始化。");
            return;
        }
        // 透過 ViewPropertyAnimator 播放兩階段透明度動畫
        imgMonster.animate()
                // 第一階段：快速變暗
                .alpha(0.3f) // 透明度降到 0.3 (30% 不透明)
                .setDuration(HIT_EFFECT_DURATION / 2) // 持續時間為總時長的一半
                // 當第一階段動畫結束後，執行第二階段動畫
                .withEndAction(() -> imgMonster.animate() // 恢復透明度
                        .alpha(1.0f) // 透明度回到 1.0 (完全不透明)
                        .setDuration(HIT_EFFECT_DURATION / 2) // 持續時間為總時長的一半
                        .start()) // 啟動恢復動畫
                .start(); // 啟動變暗動畫
    }

    // 播放怪物死亡特效
    private void showDeadEffect(final Runnable onFinished) {
        // 如果死亡動畫已在播放中，避免重複觸發
        if (onDeadEffect) {
            Log.d("showDeadEffect", "死亡動畫已在播放中，避免重複觸發。");
            return;
        }

        onDeadEffect = true; // 設定標誌為死亡動畫正在播放
        if (btnAtk != null) {
            btnAtk.setEnabled(false); // 禁用攻擊按鈕
        }

        this.onDeadEffectFinished = onFinished; // 保存死亡動畫結束後的回調邏輯
        Log.d("showDeadEffect", "怪物死亡動畫開始");
        txtMonsterHp.setText("0"); // 立即將怪物血量顯示為 0
        imgMonster.setAlpha(1.0f); // 確保怪物圖片在動畫開始時是完全可見的

        // 使用 AnimatorSet 組合多個 ObjectAnimator 實現三階段漸變透明動畫
        // Animator 1: 從 1.0f (完全不透明) 變到 0.6f (微透明)
        ObjectAnimator alpha1 = ObjectAnimator.ofFloat(imgMonster, "alpha", 1.0f, 0.6f);
        alpha1.setDuration(DEAD_EFFECT_DURATION / 3); // 總時長的三分之一

        // Animator 2: 從 0.6f 變到 0.3f (更透明)
        ObjectAnimator alpha2 = ObjectAnimator.ofFloat(imgMonster, "alpha", 0.6f, 0.3f);
        alpha2.setDuration(DEAD_EFFECT_DURATION / 3);

        // Animator 3: 從 0.3f 變到 0.0f (完全透明，即消失)
        ObjectAnimator alpha3 = ObjectAnimator.ofFloat(imgMonster, "alpha", 0.3f, 0.0f);
        alpha3.setDuration(DEAD_EFFECT_DURATION / 3);

        AnimatorSet animatorSet = new AnimatorSet(); // 創建 AnimatorSet 實例
        animatorSet.playSequentially(alpha1, alpha2, alpha3); // 設定動畫按順序播放

        // 為整個 AnimatorSet 設定一個監聽器，確保只有所有子動畫都完成後才執行一次回調
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation); // 調用父類別方法
                Log.d("showDeadEffect", "怪物死亡動畫結束，準備執行回調。");
                if (onDeadEffectFinished != null) {
                    onDeadEffectFinished.run(); // 執行死亡動畫結束後的回調邏輯 (例如：換怪物、重啟按鈕)
                    onDeadEffectFinished = null; // 清除回調引用
                }
                onDeadEffect = false; // 動畫完全結束後，將標誌設為 false，允許再次攻擊
                Log.d("showDeadEffect", "onDeadEffect 設為 false。");
            }
        });

        animatorSet.start(); // 啟動整個動畫序列
    }

    // 顯示浮動傷害數字
    private void showFloatingDamage(String damage, int mode) {
        // 如果傷害文本容器為空，則無法顯示浮動傷害
        if (damageTextContainer == null) {
            Log.e("FloatingDamage", "damageTextContainer is null, cannot show floating damage.");
            return;
        }

        final TextView damageText = new TextView(this); // 創建一個新的 TextView 用於顯示傷害數字
        damageText.setText("-" + String.valueOf(damage)); // 設定傷害文字，前面加上 "-"

        // 根據不同的 mode 設定文字顏色和大小
        switch (mode) {
            case 1: // 模式 1：手動點擊傷害 (紅色，較小)
                damageText.setTextColor(Color.RED);
                damageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                break;
            case 2: // 模式 2：GPS 攻擊傷害 (橘色，較大)
                damageText.setTextColor(Color.rgb(255, 165, 0)); // 橘色
                damageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                break;
            default: // 預設值 (紅色，較小)
                damageText.setTextColor(Color.RED);
                damageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        }

        damageText.setTypeface(Typeface.DEFAULT_BOLD); // 設定字體為粗體

        // 建立 FrameLayout.LayoutParams，設定 TextView 的佈局參數
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, // 寬度包裹內容
                ViewGroup.LayoutParams.WRAP_CONTENT // 高度包裹內容
        );
        // 設定水平居中，並將其底部對齊到 FrameLayout 的底部
        params.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.BOTTOM;

        damageText.setLayoutParams(params); // 將佈局參數應用到 TextView

        // 將 TextView 添加到 damageTextContainer 容器中
        damageTextContainer.addView(damageText);

        // 確保 TextView 已經佈局完成，才能正確獲取其高度並執行動畫
        damageText.post(() -> {
            // 計算向上移動的距離：讓文字從底部向上移動 damageTextContainer 高度的約 80%
            // 負值表示向上移動
            float translateYDistance = -(damageTextContainer.getHeight() * 0.8f);

            // 啟動浮動傷害數字的動畫
            damageText.animate()
                    .translationY(translateYDistance) // 向上移動到目標位置
                    .alpha(0f) // 同時淡出到完全透明
                    .setDuration(1200) // 動畫持續時間 1.2 秒
                    .setInterpolator(new AccelerateDecelerateInterpolator()) // 設定動畫插值器，使其先加速後減速
                    .setListener(new AnimatorListenerAdapter() { // 動畫結束監聽器
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            // 動畫結束後，從父佈局中移除 TextView，釋放資源
                            damageTextContainer.removeView(damageText);
                        }
                    })
                    .start(); // 啟動動畫
        });
    }

    // 處理攻擊邏輯的通用方法
    private void handleAttackLogic(String damage, int mode) {
        // 在怪物死亡動畫播放期間，不執行任何攻擊相關的邏輯，避免衝突
        if (onDeadEffect) {
            Log.d("AttackLogic", "死亡動畫播放中，攻擊操作被忽略。");
            return;
        }

        // 顯示浮動傷害數字，根據 mode 區分手動攻擊和 GPS 攻擊
        showFloatingDamage(damage, mode);

        // 檢查怪物是否死亡
        if (game.isMonsterDead()) {
            Log.d("AttackLogic", "怪物被擊敗！觸發死亡動畫。");
            // 觸發死亡動畫，並傳入一個 Lambda 表達式作為回調，該回調會在死亡動畫播放完畢後執行
            showDeadEffect(() -> {
                // 這個 Lambda 表達式會在死亡動畫播放完畢後執行：
                monsterId++; // 怪物 ID 增加，準備顯示下一個怪物
                // 怪物 ID 取模，實現怪物循環出現的效果 (例如，如果只有 3 種怪物，ID 會在 0, 1, 2 之間循環)
                monsterId %= (byte) (getResources().getStringArray(R.array.monster_name).length);
                imgMonster.setAlpha(1.0f); // 確保新的怪物圖片是完全可見的
                updateDisplay(); // 更新 UI 顯示新怪物的圖片和資訊
                if (btnAtk != null) {
                    btnAtk.setEnabled(true); // 重新啟用攻擊按鈕功能
                }
                Log.d("AttackLogic", "變換新怪物並重啟按鈕： " + monsterId);
                dataWrite(); // 換怪物後保存數據
            });
        } else {
            // 怪物未死亡，播放受擊特效並更新顯示
            showHitEffect();
            updateDisplay(); // 更新 UI，例如怪物血量
            dataWrite(); // 沒死也保存進度（例如 HP 減少），確保數據持久化
        }
    }

    // 刷新介面顯示
    public void updateDisplay() {
        // 從 Game 物件獲取格式化後的數據，並設定到對應的 TextView
        txtAtk.setText(game.getAtk());
        txtAtkUpCoin.setText(game.getAtkUpCoin());
        txtCoin.setText(game.getCoin());
        txtStage.setText(getResources().getString(R.string.Stage) + " " + game.getStage()); // 組合關卡文字和數字
        txtMonsterHp.setText(game.getMonsterHp());
        txtMileage.setText(String.format("%.2f Km", totalDistance / 1000)); // 總距離轉換為公里並格式化
        // 設定怪物名稱，從資源陣列中獲取對應 ID 的名稱
        txtMonsterName.setText(getResources().getStringArray(R.array.monster_name)[monsterId]);
        // 設定怪物圖片，從 TypedArray 資源中獲取對應 ID 的圖片
        imgMonster.setImageResource(getResources().obtainTypedArray(R.array.monster_img).getResourceId(monsterId, 0));

        // 根據金幣是否足夠升級攻擊力，改變升級按鈕的圖片
        if (game.isAtkUpAble()) {
            btnAtkUp.setImageResource(R.drawable.up_btn_blue); // 可升級時顯示藍色按鈕
        } else {
            btnAtkUp.setImageResource(R.drawable.up_btn_red); // 金幣不足時顯示紅色按鈕
        }
    }

    // GPS 攻擊觸發方法
    // distance: 玩家移動的距離
    public void atk(float distance) {
        // 如果死亡特效還在持續，直接返回，不執行攻擊
        if (onDeadEffect) {
            return;
        }
        // 調用通用攻擊邏輯處理方法，傳遞 GPS 造成的傷害和模式 2 (GPS 攻擊)
        handleAttackLogic(game.atk(distance), 2);
    }

    // 手動點擊攻擊觸發方法，由 XML 佈局中的 android:onClick 屬性綁定
    public void btnAtkClick(View view) {
        // 如果死亡特效還在持續，直接返回
        if (onDeadEffect) {
            return;
        }
        // 調用通用攻擊邏輯處理方法，傳遞手動點擊造成的傷害和模式 1 (手動點擊攻擊)
        handleAttackLogic(game.atk(), 1);
    }

    // 攻擊力升級按鈕點擊方法，由 XML 佈局中的 android:onClick 屬性綁定
    public void btnAtkUpClick(View view) {
        game.atkUp(); // 調用 Game 物件的攻擊力升級方法
        updateDisplay(); // 刷新 UI 顯示更新後的攻擊力、金幣和升級按鈕狀態
    }
}