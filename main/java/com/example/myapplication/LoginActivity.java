package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 登录页（启动 Activity）。
 * 校验后端可达后调用 UserManager.login，成功跳转 HomeActivity。
 */
public class LoginActivity extends AppCompatActivity {

    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = new UserManager(this);
        // 已登录则直接进入首页，避免重复登录
        if (userManager.isLoggedIn()) {
            goHome();
            return;
        }
        setContentView(R.layout.activity_login);
        UiHelper.setupBackButton(this);
        if (isTaskRoot()) {
            findViewById(R.id.btnBack).setVisibility(android.view.View.GONE);
        }

        EditText etAccount = findViewById(R.id.etAccount);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgot = findViewById(R.id.tvForgot);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> {
            String account = etAccount.getText().toString().trim();
            String password = etPassword.getText().toString();
            if (account.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.empty_input, Toast.LENGTH_SHORT).show();
                return;
            }
            btnLogin.setEnabled(false); // 防止重复点击
            new Thread(() -> { // 登录逻辑放子线程（含网络请求）
                boolean reachable = userManager.isServerReachable(); // 子线程检测后端
                if (!reachable) { // 连不上
                    runOnUiThread(() -> { // 回主线程提示
                        btnLogin.setEnabled(true); // 恢复按钮
                        Toast.makeText(this, R.string.server_unreachable, Toast.LENGTH_LONG).show(); // 提示
                    });
                    return;
                }
                boolean ok = userManager.login(account, password); // 子线程登录
                runOnUiThread(() -> { // 回主线程更新 UI
                    btnLogin.setEnabled(true); // 恢复按钮
                    if (ok) { // 登录成功
                        goHome(); // 进首页
                    } else { // 失败
                        Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show(); // 账号或密码错误
                    }
                });
            }).start(); // 启动线程
        });

        tvForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}