package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** 注册页：调用 UserManager.register 写入 MySQL */
public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        UiHelper.setupBackButton(this);

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());

        UserManager userManager = new UserManager(this);
        EditText etAccount = findViewById(R.id.etAccount);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String account = etAccount.getText().toString().trim();
            String password = etPassword.getText().toString();
            if (account.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.empty_input, Toast.LENGTH_SHORT).show();
                return;
            }
            if (userManager.register(account, password)) {
                Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}