package com.example.myapplication;

import android.util.Log; // 用於輸出日誌

public class Game {

    // 用於將大數值轉換為帶有單位的字串，例如 1000 -> "1k", 1000000 -> "1m",節省字串顯示的空間
    // 每個字元對應一個數量級的單位 (空白, 千, 百萬, 十億...等)
    public final char UNITS[] = {' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    // 遊戲常數定義：
    private final int BASE_MONSTER_HP = 300; // 基礎怪物血量
    private final int BASE_ATK = 10; // 基礎攻擊力
    private final int BASE_COIN_REWARD = 50; // 擊敗怪物後獲得的基礎金幣獎勵
    private final float ATK_UPGRADE_COIN_MULTIPLIER = 1.3f; // 攻擊力升級所需金幣的乘數
    private final float MONSTER_HP_MULTIPLIER = 1.3f; // 怪物血量在下一關卡的乘數
    private final float GPS_ATK_MULTIPLIER = 2.0f; // GPS 攻擊的額外傷害乘數

    // 遊戲狀態變數：
    private int stage; // 當前關卡
    private long monsterHp; // 當前怪物的血量
    private long preMonsterHp; // 上一個怪物的血量（用於計算下一隻怪物的初始血量）
    private long atk; // 玩家當前的攻擊力
    private long atkLevel; // 玩家攻擊力的等級
    private long atkUpCoin; // 升級攻擊力所需的金幣
    private long coin; // 玩家的金幣數量

    // 預設構造函數：用於初始化一個新的遊戲
    public Game() {
        this.stage = 1; // 從第一關開始
        this.monsterHp = BASE_MONSTER_HP; // 怪物血量設定為基礎值
        this.preMonsterHp = BASE_MONSTER_HP; // 上一個怪物血量也為基礎值
        this.atk = BASE_ATK; // 攻擊力設定為基礎值
        this.atkLevel = 1; // 攻擊力等級為1
        this.atkUpCoin = 50; // 初始升級金幣需求
        this.coin = 0; // 初始金幣為0
    }

    // 帶參數的構造函數：用於從資料庫載入數據，恢復遊戲狀態
    public Game(int stage, long monsterHp, long preMonsterHp, long atk, long atkLevel, long atkUpCoin, long coin) {
        this.stage = stage;
        this.monsterHp = monsterHp;
        this.preMonsterHp = preMonsterHp;
        this.atk = atk;
        this.atkLevel = atkLevel;
        this.atkUpCoin = atkUpCoin;
        this.coin = coin;
    }

    // 推進到下一關：
    public void nextStage() {
        stage++; // 關卡數加1
        monsterHp = (long) (MONSTER_HP_MULTIPLIER * preMonsterHp); // 新怪物的血量基於上一隻怪物的血量乘以乘數
        preMonsterHp = monsterHp; // 更新 preMonsterHp 為當前怪物的血量，以便計算下一隻
        Log.d("game.nextStage", "怪物血量: " + monsterHp); // 記錄新怪物血量
    }

    // 升級攻擊力：
    public void atkUp() {
        if (!this.isAtkUpAble()) { // 檢查金幣是否足夠升級
            return; // 金幣不足，不執行升級
        }
        atkLevel++; // 攻擊等級提升
        atk = atkLevel * BASE_ATK; // 攻擊力根據等級和基礎攻擊力重新計算
        coin -= atkUpCoin; // 扣除升級所需金幣
        atkUpCoin = (long) (atkUpCoin * ATK_UPGRADE_COIN_MULTIPLIER); // 下一次升級所需金幣增加
    }

    // 手動點擊攻擊：
    // 返回一個格式化後的傷害數字字串。
    public String atk() {
        monsterHp -= atk; // 怪物血量減少玩家攻擊力
        Log.d("game.atk", "怪物受到點擊傷害: " + atk); // 記錄傷害值
        Log.d("game.atk", "怪物剩餘血量: " + monsterHp); // 記錄剩餘血量
        return convertUnit(atk); // 返回格式化後的攻擊力數值
    }

    // GPS 自動攻擊：
    // distance: 玩家移動的距離（公尺）。
    // 返回一個格式化後的傷害數字字串。
    public String atk(float distance) {
        // 計算 GPS 造成的傷害：基礎攻擊力 * 移動距離 * GPS 乘數
        long damage = (long) (atk * distance * GPS_ATK_MULTIPLIER);
        damage = Math.max(damage, 1L); // 確保傷害至少為1，避免小距離移動無傷害
        monsterHp -= damage; // 怪物血量減少 GPS 傷害
        Log.d("game.atk", "怪物受到GPS傷害: " + damage); // 記錄 GPS 傷害值
        Log.d("game.atk", "怪物剩餘血量: " + monsterHp); // 記錄剩餘血量
        return convertUnit(damage); // 返回格式化後的傷害數值
    }

    // 檢查是否能夠升級攻擊力：
    public boolean isAtkUpAble() {
        return coin >= atkUpCoin; // 如果金幣大於或等於升級所需金幣，則可以升級
    }

    // 檢查怪物是否死亡：
    public boolean isMonsterDead() {
        if (monsterHp <= 0) { // 如果怪物血量小於或等於0
            coin += (long) (stage * BASE_COIN_REWARD); // 玩家獲得金幣獎勵，獎勵與關卡數相關
            Log.d("game.atk", "金幣數量: " + coin); // 記錄當前金幣數量
            nextStage(); // 推進到下一關
            return true; // 怪物死亡
        }
        return false; // 怪物未死亡
    }

    // 數值單位轉換：
    // 將大數值轉換為帶有單位（例如 1a, 1b, 1c 等）的字串，用於 UI 顯示。
    public String convertUnit(long value) {
        String s = "";
        int len = String.valueOf(value).length(); // 數值的字串長度
        if (len > 3) { // 如果數字超過三位數（例如 1000 以上）
            int showLen;
            if (len % 3 == 0) { // 如果長度是 3 的倍數（例如 1000, 1000000）
                showLen = 3; // 顯示 3 位數字作為前綴 (例如 1a)
            } else {
                showLen = len % 3; // 顯示餘數位數字作為前綴 (例如 10a)
            }
            s = String.valueOf(value).substring(0, showLen); // 取出前綴數字
            // 根據字串長度判斷單位（每 3 位數對應 UNITS 陣列中的一個單位）
            s += UNITS[(int) Math.floor(len / 3.0)];
        } else {
            s = String.valueOf(value); // 如果是三位數以下，直接轉換為字串
        }
        return s;
    }

    // 以下是各種 getter 方法，用於獲取格式化或原始的遊戲數據，供 UI 顯示。

    public String getAtk() {
        return convertUnit(atk); // 返回格式化後的攻擊力字串
    }

    public String getAtkUpCoin() {
        return convertUnit(atkUpCoin); // 返回格式化後的升級金幣需求字串
    }

    public String getCoin() {
        return convertUnit(coin); // 返回格式化後的金幣數量字串
    }

    public String getStage() {
        return String.valueOf(stage); // 返回關卡數的字串形式
    }

    public String getMonsterHp() {
        return convertUnit(monsterHp); // 返回格式化後的怪物血量字串
    }

    // 獲取原始（非格式化）的遊戲數據，主要供資料庫寫入時使用
    protected int getRealStage() {
        return stage;
    }

    protected long getRealMonsterHp() {
        return monsterHp;
    }

    protected long getRealPreMonsterHp() {
        return preMonsterHp;
    }

    protected long getRealAtk() {
        return atk;
    }

    protected long getRealAtkLevel() {
        return atkLevel;
    }

    protected long getRealAtkUpCoin() {
        return atkUpCoin;
    }

    protected long getRealCoin() {
        return coin;
    }
}