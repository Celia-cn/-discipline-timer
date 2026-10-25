# 自律时钟（Discipline Timer）

专注计时 Android 客户端：登录注册、倒计时专注、积分奖励与主题商店。用户资料由 Spring Boot 后端（默认端口 `8081`）写入 MySQL。

> GitHub 仓库名建议：`discipline-timer`  
> 简介建议：`专注计时 Android 客户端，支持账号、积分与主题商店，对接 Spring Boot 后端。`

## 技术栈

- Android（Java）、OkHttp
- 后端：Spring Boot + MySQL（本仓库不含后端工程）

## 功能

- **账号**：登录、注册、忘记密码、修改密码、退出登录
- **首页**：累计自律时长、积分余额、当前主题背景
- **专注**：10 / 30 / 60 / 120 分钟或自定义时长；完成后约每 10 分钟获得 1 积分
- **专注锁定**：计时中限制返回与切出；可主动放弃并上报
- **个人中心**：专注次数、平均时长、放弃次数、积分等统计
- **主题商店**：清新绿（免费）；天空蓝、暖阳黄、活力橙、热情红（默认 100 积分解锁）

## 页面流程

```
LoginActivity（启动页）
  ├─ 已登录 → HomeActivity
  ├─ 注册 → RegisterActivity
  └─ 忘记密码 → ForgotPasswordActivity
HomeActivity
  ├─ 开始自律 → FocusSetupActivity → FocusTimerActivity
  └─ 个人中心 → ProfileActivity
        ├─ 主题商店 → ThemeStoreActivity
        └─ 修改密码 → ChangePasswordActivity
```

`MainActivity` 为模板遗留页，未接入主流程。

## 仓库内容说明

本仓库是 Android Studio 工程中的 **`app/src/main` 源码快照**（清单、Java、布局、部分资源），**不是**可直接 `Open` 后编译的完整工程。

| 已包含 | 未包含 |
|--------|--------|
| `main/AndroidManifest.xml`、Java、layout、values | Gradle（`settings.gradle`、`build.gradle`、`gradlew`） |
| 登录 / 专注 / 积分 / 主题相关业务代码 | `BuildConfig.API_BASE_URL` |
| | OkHttp 等依赖声明 |
| | 主题图 `green` / `blue` / `yellow` / `orange` / `red` |

运行方式：将 `main/` 对应放回完整工程的 `app/src/main`，补上 Gradle 与主题图片后，用 Android Studio 打开原工程。

## 目录结构

```
.
├── README.md
├── .gitignore
└── main/                            # 对应完整工程的 app/src/main
    ├── AndroidManifest.xml
    ├── java/com/example/myapplication/
    └── res/
```

主要类：

| 文件 | 作用 |
|------|------|
| `LoginActivity.java` | 启动页 |
| `HomeActivity.java` | 首页 |
| `FocusSetupActivity.java` / `FocusTimerActivity.java` | 时长选择与倒计时 |
| `ProfileActivity.java` / `ThemeStoreActivity.java` | 个人中心与主题商店 |
| `UserManager.java` | 业务门面 |
| `ApiClient.java` | HTTP 调用后端 |
| `UserProfile.java` | 对应用户资料 DTO |

## 后端接口

统一响应：`success` / `message` / `data`。

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/api/health` | 探测后端 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/register` | 注册 |
| GET | `/api/auth/exists` | 账号是否存在 |
| POST | `/api/auth/reset-password` | 重置密码 |
| GET | `/api/users/{username}` | 拉取资料 |
| POST | `/api/users/{username}/change-password` | 修改密码 |
| POST | `/api/users/{username}/focus/complete` | 完成专注 |
| POST | `/api/users/{username}/focus/abandon` | 放弃专注 |
| PUT | `/api/users/{username}/theme/select` | 切换主题 |
| POST | `/api/users/{username}/theme/unlock` | 解锁主题 |

基地址优先 `BuildConfig.API_BASE_URL`，失败则依次尝试：

1. `http://10.0.2.2:8081/`（模拟器访问本机）
2. `http://127.0.0.1:8081/`
3. `http://10.18.249.5:8081/`（真机示例，请改为电脑局域网 IP）

## 运行前提

1. 使用带 Gradle 的完整 Android Studio 工程，不要只打开本仓库根目录。
2. 放入主题背景图：`res/drawable/green`、`blue`、`yellow`、`orange`、`red`。
3. 导入 `sql/timer.sql` 并启动后端（端口 **8081**）。
4. 测试账号：`demo` / `123456`（需库中已有数据）。

本地仅用 SharedPreferences（`discipline_timer_prefs`）保存当前用户名。积分、时长、主题以服务端为准。
