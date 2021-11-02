package com.example.readysteady.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.readysteady.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button rider = findViewById(R.id.rider);
        Button driver = findViewById(R.id.driver);
        rider.setOnClickListener(view -> {
            Intent login = new Intent(getApplicationContext(), LoginActivity.class);
            login.putExtra("role", "rider");
            startActivity(login);
        });
        driver.setOnClickListener(view -> {
            Intent login = new Intent(getApplicationContext(), LoginActivity.class);
            login.putExtra("role", "driver");
            startActivity(login);
        });
    }
}