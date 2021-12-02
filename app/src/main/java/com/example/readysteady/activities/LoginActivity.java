package com.example.readysteady.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.readysteady.R;
import com.example.readysteady.models.*;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button loginButton = findViewById(R.id.loginButton);
        Button cancelButton = findViewById(R.id.loginCanelButton);
        Button forgotUsername = findViewById(R.id.loginForgotUsername);

        EditText email = findViewById(R.id.loginEmailAddress);

        EditText password = findViewById(R.id.loginPassword);
        String role = getIntent().getStringExtra("role");
        loginButton.setOnClickListener(view -> {

            Intent loading = new Intent(getApplicationContext(), LoadingActivity.class);
            loading.putExtra("loginUser", new LoginModel(email.getText().toString(), password.getText().toString(), role));
            startActivity(loading);
        });
        cancelButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        });
    }
}
