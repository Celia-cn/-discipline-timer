package com.example.myapplication;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/** 修改密码（需已登录，校验原密码） */
public class ChangePasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserManager userManager = new UserManager(this);
        if (!UiHelper.ensureLoggedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_change_password);
        setTitle(R.string.change_password);
        UiHelper.setupBackButton(this);
        TextView tvAccount = findViewById(R.id.tvAccount);
        EditText etOldPassword = findViewById(R.id.etOldPassword);
        EditText etNewPassword = findViewById(R.id.etNewPassword);
        MaterialButton btnConfirm = findViewById(R.id.btnConfirm);

        String username = userManager.getCurrentUsername();
        tvAccount.setText(getString(R.string.account_hint) + "：" + username);

        btnConfirm.setOnClickListener(v -> {
            String oldPwd = etOldPassword.getText().toString();
            String newPwd = etNewPassword.getText().toString();
            if (oldPwd.isEmpty() || newPwd.isEmpty()) {
                Toast.makeText(this, R.string.empty_input, Toast.LENGTH_SHORT).show();
                return;
            }
            if (userManager.changePassword(oldPwd, newPwd)) {
                Toast.makeText(this, R.string.change_password_success, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.old_password_wrong, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiHelper.refreshBackButtonTheme(this);
    }
}