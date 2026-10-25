package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** 忘记密码：校验账号存在后 resetPassword */
public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        UiHelper.setupBackButton(this);

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());

        UserManager userManager = new UserManager(this);
        EditText etAccount = findViewById(R.id.etAccount);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnConfirm = findViewById(R.id.btnConfirm);

        btnConfirm.setOnClickListener(v -> {
            String account = etAccount.getText().toString().trim();
            String password = etPassword.getText().toString();
            if (account.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.empty_input, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!userManager.userExists(account)) {
                Toast.makeText(this, R.string.user_not_found, Toast.LENGTH_SHORT).show();
                return;
            }
            if (userManager.resetPassword(account, password)) {
                Toast.makeText(this, R.string.reset_success, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}