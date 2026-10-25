package com.example.myapplication;

/** 主题配置枚举：颜色、背景图、解锁积分，与 MySQL selected_bg / bg_id 对应 */
public class AppTheme {

    public final String id;
    public final String name;
    public final int primary;
    public final int primaryDark;
    public final int primaryLight;
    public final int btn;
    public final int bgDrawable;
    public final int unlockCost;

    public AppTheme(String id, String name, int primary, int primaryDark, int primaryLight,
                    int btn, int bgDrawable, int unlockCost) {
        this.id = id;
        this.name = name;
        this.primary = primary;
        this.primaryDark = primaryDark;
        this.primaryLight = primaryLight;
        this.btn = btn;
        this.bgDrawable = bgDrawable;
        this.unlockCost = unlockCost;
    }

    public static final AppTheme[] ALL = {
            new AppTheme(UserManager.THEME_GREEN, "清新绿",
                    0xFF43A047, 0xFF2E7D32, 0xFF66BB6A, 0xFF388E3C,
                    R.drawable.green, 0),
            new AppTheme(UserManager.THEME_BLUE, "天空蓝",
                    0xFF1E88E5, 0xFF1565C0, 0xFF42A5F5, 0xFF1976D2,
                    R.drawable.blue, UserManager.UNLOCK_COST),
            new AppTheme(UserManager.THEME_YELLOW, "暖阳黄",
                    0xFFF9A825, 0xFFF57F17, 0xFFFBC02D, 0xFFF9A825,
                    R.drawable.yellow, UserManager.UNLOCK_COST),
            new AppTheme(UserManager.THEME_ORANGE, "活力橙",
                    0xFFFB8C00, 0xFFEF6C00, 0xFFFFA726, 0xFFF57C00,
                    R.drawable.orange, UserManager.UNLOCK_COST),
            new AppTheme(UserManager.THEME_RED, "热情红",
                    0xFFE53935, 0xFFC62828, 0xFFEF5350, 0xFFD32F2F,
                    R.drawable.red, UserManager.UNLOCK_COST),
    };

    public static AppTheme byId(String id) {
        for (AppTheme t : ALL) {
            if (t.id.equals(id)) return t;
        }
        return ALL[0];
    }
}