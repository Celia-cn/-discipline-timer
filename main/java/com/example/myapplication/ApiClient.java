package com.example.myapplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 网络层：封装对 Spring Boot 后端的 HTTP 调用（数据访问的客户端实现）。
 * 对应后端 {@code ApiController} 的每个接口；解析统一 JSON 格式 ApiResponse。
 */
public final class ApiClient {

    /** 请求体 Content-Type */
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /** 全局单例 OkHttp，配置连接/读/写超时 */
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS) // 连接超时（秒）
            .readTimeout(20, TimeUnit.SECONDS) // 读取超时
            .writeTimeout(20, TimeUnit.SECONDS) // 写入超时
            .build();

    private static final String[] FALLBACK_BASE_URLS = { // 依次尝试的后端地址
            "http://10.0.2.2:8081/",       // Android 模拟器访问电脑
            "http://127.0.0.1:8081/",      // 部分环境
            "http://10.18.249.5:8081/",    // 真机与电脑同一 WiFi 时
    };

    private String baseUrl; // 当前可用的后端根地址
    private volatile boolean baseUrlResolved; // 是否已在子线程完成地址探测

    public ApiClient() { // 构造：仅读取配置，不在主线程发网络请求
        baseUrl = normalizeBaseUrl(BuildConfig.API_BASE_URL); // 默认用 gradle 配置
    }

    // 供调试查看当前使用的地址
    public String getBaseUrl() {
        ensureBaseUrlResolved(); // 确保已探测
        return baseUrl;
    }

    // 首次请求前在子线程探测可用地址
    private synchronized void ensureBaseUrlResolved() {
        if (baseUrlResolved) { // 已解析则跳过
            return;
        }
        baseUrl = resolveWorkingBaseUrl(); // 遍历候选地址
        baseUrlResolved = true; // 标记完成
    }

    /** GET /api/health — 登录前探测后端是否启动（须在子线程调用） */
    public boolean isServerReachable() {
        ensureBaseUrlResolved(); // 先解析可用 baseUrl
        return pingHealth(baseUrl); // 检测当前 baseUrl
    }

    // 选择第一个能连通的地址
    private String resolveWorkingBaseUrl() {
        String configured = normalizeBaseUrl(BuildConfig.API_BASE_URL); // gradle 配置
        if (pingHealth(configured)) { // 若配置地址可用
            return configured; // 使用配置
        }
        for (String candidate : FALLBACK_BASE_URLS) { // 遍历备选
            if (pingHealth(candidate)) { // 尝试连通
                return candidate; // 使用备选
            }
        }
        return configured; // 都失败则仍返回配置（后续请求会报错）
    }

    // 保证以 / 结尾
    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isEmpty()) { // 空则默认模拟器地址
            return "http://10.0.2.2:8081/";
        }
        return url.endsWith("/") ? url : url + "/"; // 补全斜杠
    }

    // 请求健康检查接口
    private boolean pingHealth(String base) {
        try {
            Request request = new Request.Builder()
                    .url(base + "api/health")
                    .get()
                    .build();
            JSONObject root = new JSONObject(execute(request));
            return root.optBoolean("success", false);
        } catch (Exception e) { // 网络失败
            return false; // 不可达
        }
    }

    public UserProfile login(String username, String password) throws IOException {
        JSONObject body = jsonBody("username", username, "password", password);
        ApiResult<UserProfile> result = postProfile("/api/auth/login", body);
        return result.success ? result.data : null;
    }

    public boolean register(String username, String password) throws IOException {
        JSONObject body = jsonBody("username", username, "password", password);
        return postVoid("/api/auth/register", body).success;
    }

    public boolean userExists(String username) throws IOException {
        String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8);
        ApiResult<Boolean> result = getBoolean("/api/auth/exists?username=" + encoded);
        return result.success && Boolean.TRUE.equals(result.data);
    }

    public boolean resetPassword(String username, String newPassword) throws IOException {
        JSONObject body = jsonBody("username", username, "newPassword", newPassword);
        return postVoid("/api/auth/reset-password", body).success;
    }

    public UserProfile fetchProfile(String username) throws IOException {
        ApiResult<UserProfile> result = getProfile("/api/users/" + encodePath(username));
        return result.success ? result.data : null;
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) throws IOException {
        JSONObject body = jsonBody("oldPassword", oldPassword, "newPassword", newPassword);
        return postVoid("/api/users/" + encodePath(username) + "/change-password", body).success;
    }

    public UserProfile recordFocusComplete(String username, long seconds) throws IOException {
        JSONObject body = jsonBody("seconds", seconds);
        ApiResult<UserProfile> result = postProfile("/api/users/" + encodePath(username) + "/focus/complete", body);
        return result.success ? result.data : null;
    }

    public UserProfile recordFocusAbandon(String username, long sessionDuration, long remaining) throws IOException {
        JSONObject body = jsonBody(
                "sessionDurationSeconds", sessionDuration,
                "remainingSeconds", remaining);
        ApiResult<UserProfile> result = postProfile("/api/users/" + encodePath(username) + "/focus/abandon", body);
        return result.success ? result.data : null;
    }

    public UserProfile selectTheme(String username, String themeId) throws IOException {
        JSONObject body = jsonBody("themeId", themeId);
        ApiResult<UserProfile> result = putProfile("/api/users/" + encodePath(username) + "/theme/select", body);
        return result.success ? result.data : null;
    }

    public UserProfile unlockTheme(String username, String themeId) throws IOException {
        JSONObject body = jsonBody("themeId", themeId);
        ApiResult<UserProfile> result = postProfile("/api/users/" + encodePath(username) + "/theme/unlock", body);
        return result.success ? result.data : null;
    }

    private static String encodePath(String username) {
        return URLEncoder.encode(username, StandardCharsets.UTF_8);
    }

    private ApiResult<UserProfile> postProfile(String path, JSONObject body) throws IOException {
        ensureBaseUrlResolved(); // 子线程中解析可用后端地址
        Request request = new Request.Builder()
                .url(baseUrl + trimPath(path))
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        return parseProfileResponse(execute(request));
    }

    private ApiResult<Void> postVoid(String path, JSONObject body) throws IOException {
        ensureBaseUrlResolved(); // 子线程中解析可用后端地址
        Request request = new Request.Builder()
                .url(baseUrl + trimPath(path))
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        return parseVoidResponse(execute(request));
    }

    private ApiResult<UserProfile> putProfile(String path, JSONObject body) throws IOException {
        ensureBaseUrlResolved(); // 子线程中解析可用后端地址
        Request request = new Request.Builder()
                .url(baseUrl + trimPath(path))
                .put(RequestBody.create(body.toString(), JSON))
                .build();
        return parseProfileResponse(execute(request));
    }

    private ApiResult<UserProfile> getProfile(String path) throws IOException {
        ensureBaseUrlResolved(); // 子线程中解析可用后端地址
        Request request = new Request.Builder()
                .url(baseUrl + trimPath(path))
                .get()
                .build();
        return parseProfileResponse(execute(request));
    }

    private ApiResult<Boolean> getBoolean(String path) throws IOException {
        ensureBaseUrlResolved(); // 子线程中解析可用后端地址
        Request request = new Request.Builder()
                .url(baseUrl + trimPath(path))
                .get()
                .build();
        return parseBooleanResponse(execute(request));
    }

    private String execute(Request request) throws IOException {
        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.body() == null) {
                throw new IOException("empty response");
            }
            return response.body().string();
        }
    }

    private String trimPath(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private JSONObject jsonBody(Object... keyValues) throws IOException {
        JSONObject body = new JSONObject();
        for (int i = 0; i < keyValues.length; i += 2) {
            try {
                body.put((String) keyValues[i], keyValues[i + 1]);
            } catch (JSONException e) {
                throw new IOException(e);
            }
        }
        return body;
    }

    private ApiResult<UserProfile> parseProfileResponse(String raw) {
        ApiResult<UserProfile> result = new ApiResult<>();
        try {
            JSONObject root = new JSONObject(raw);
            result.success = root.optBoolean("success", false);
            result.message = root.optString("message", "");
            if (root.has("data") && !root.isNull("data")) {
                result.data = UserProfile.fromJson(root.getJSONObject("data"));
            }
        } catch (Exception e) {
            result.success = false;
            result.message = e.getMessage();
        }
        return result;
    }

    private ApiResult<Boolean> parseBooleanResponse(String raw) {
        ApiResult<Boolean> result = new ApiResult<>();
        try {
            JSONObject root = new JSONObject(raw);
            result.success = root.optBoolean("success", false);
            result.message = root.optString("message", "");
            if (root.has("data") && !root.isNull("data")) {
                result.data = root.getBoolean("data");
            }
        } catch (Exception e) {
            result.success = false;
            result.message = e.getMessage();
        }
        return result;
    }

    private ApiResult<Void> parseVoidResponse(String raw) {
        ApiResult<Void> result = new ApiResult<>();
        try {
            JSONObject root = new JSONObject(raw);
            result.success = root.optBoolean("success", false);
            result.message = root.optString("message", "");
        } catch (Exception e) {
            result.success = false;
            result.message = e.getMessage();
        }
        return result;
    }

    private static class ApiResult<T> {
        boolean success;
        String message;
        T data;
    }
}