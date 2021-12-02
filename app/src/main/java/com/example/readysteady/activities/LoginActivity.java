package com.example.readysteady.activities;

import android.content.Intent;
import android.os.Bundle;
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

            Intent login;
            if (role.equals("driver")) {
                if (!email.getText().toString().equals("driver@gmail.com")){
                    Toast.makeText(getApplicationContext(),"You username and password is not registered. Please register yourself", Toast.LENGTH_LONG).show();
                    return;
                }
                login = new Intent(getApplicationContext(), MapDriverActivity.class);
            } else {
                if (!email.getText().toString().equals("rider@gmail.com")){
                    Toast.makeText(getApplicationContext(),"You username and password is not registered. Please register yourself", Toast.LENGTH_LONG).show();
                    return;
                }
                login = new Intent(getApplicationContext(), MapRiderActivity.class);
            }
            login.putExtra("loginUser", new LoginModel(email.getText().toString(), password.getText().toString(), role));
            startActivity(login);
        });
    }
}
