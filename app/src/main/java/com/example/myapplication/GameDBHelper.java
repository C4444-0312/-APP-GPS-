package com.example.myapplication;

import android.content.Context; // 用於獲取應用程式的環境資訊
import android.database.sqlite.SQLiteDatabase; // SQLite 資料庫的核心類別
import android.database.sqlite.SQLiteOpenHelper; // 輔助類別，用於管理資料庫的創建和版本控制

public class GameDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "GameData"; // 資料庫檔案的名稱
    private static final int DATABASE_VERSION = 1; // 資料庫的版本號，用於控制 onUpgrade 方法的觸發

    // 構造函數：
    // 初始化 GameDBHelper。
    // context: 應用程式的環境。
    // DATABASE_NAME: 資料庫的名稱。
    // null: CursorFactory，通常設為 null，使用預設的。
    // DATABASE_VERSION: 資料庫的版本號。
    public GameDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override // 覆寫 SQLiteOpenHelper 的 onCreate 方法，當資料庫第一次被創建時調用
    public void onCreate(SQLiteDatabase db) {
        // 創建 player 資料表：
        // 儲存玩家的遊戲數據，例如金幣、關卡、攻擊力等。
        // id: INTEGER PRIMARY KEY (玩家唯一識別碼，自動增長)
        // coin: BIGINT (金幣數量，使用長整數以支持大數值)
        // stage: INTEGER (遊戲關卡)
        // atk: BIGINT (攻擊力)
        // atkLevel: INTEGER (攻擊力等級)
        // atkUpCoin: BIGINT (升級攻擊力所需金幣)
        // totalDistance: REAL (玩家累積的總移動距離，使用浮點數)
        db.execSQL("CREATE TABLE player(" +
                "id INTEGER PRIMARY KEY," +
                "coin BIGINT," +
                "stage INTEGER," +
                "atk BIGINT," +
                "atkLevel INTEGER," +
                "atkUpCoin BIGINT," +
                "totalDistance REAL)");

        // 創建 monster 資料表：
        // 儲存當前怪物及相關的數據，例如血量、上次擊敗的怪物ID等。
        // id: INTEGER PRIMARY KEY (怪物數據唯一識別碼，通常只會有一條記錄)
        // monsterHpLeft: BIGINT (怪物剩餘血量)
        // preMonsterHp: BIGINT (上一個怪物的初始血量，用於計算下一個怪物的血量)
        // lastMonsterId: INTEGER (上次擊敗的怪物在資源陣列中的ID)
        db.execSQL("CREATE TABLE monster(" +
                "id INTEGER PRIMARY KEY," +
                "monsterHpLeft BIGINT," +
                "preMonsterHp BIGINT," +
                "lastMonsterId INTEGER)");
    }

    @Override // 覆寫 SQLiteOpenHelper 的 onUpgrade 方法，當資料庫版本號升級時調用
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 資料庫升級策略：簡單地刪除舊表格並重新創建
        // 注意：這種方法會丟失所有現有數據。在實際發布的應用中，通常需要更精細的數據遷移策略。
        db.execSQL("DROP TABLE IF EXISTS player"); // 如果 player 表格存在，則刪除
        db.execSQL("DROP TABLE IF EXISTS monster"); // 如果 monster 表格存在，則刪除
        onCreate(db); // 重新調用 onCreate 方法，創建新的表格結構
    }
}