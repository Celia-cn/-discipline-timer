package com.example.myapplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户资料模型：与后端 UserProfileDto 字段一一对应。
 * 由 ApiClient 解析 JSON 后供 UserManager 内存缓存使用。
 */
public class UserProfile {

    public String username;
    public int points;
    public long totalSeconds;
    public int focusCount;
    public int abandonCount;
    public long totalAbandonSeconds;
    public int fishCount;
    public String selectedBg;
    public List<String> unlockedBgs = new ArrayList<>();

    /** 将接口返回的 data 对象转为 Java 对象 */
    public static UserProfile fromJson(JSONObject data) {
        UserProfile profile = new UserProfile();
        profile.username = data.optString("username");
        profile.points = data.optInt("points");
        profile.totalSeconds = data.optLong("totalSeconds");
        profile.focusCount = data.optInt("focusCount");
        profile.abandonCount = data.optInt("abandonCount");
        profile.totalAbandonSeconds = data.optLong("totalAbandonSeconds");
        profile.fishCount = data.optInt("fishCount");
        profile.selectedBg = data.optString("selectedBg", UserManager.BG_GREEN);
        JSONArray arr = data.optJSONArray("unlockedBgs");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                profile.unlockedBgs.add(arr.optString(i));
            }
        }
        return profile;
    }
}