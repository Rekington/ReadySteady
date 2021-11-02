package com.example.readysteady.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.readysteady.R;

public class RequestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);
        Button request = findViewById(R.id.requestForRide);
        EditText address = findViewById(R.id.requestedPostalAddress);
        EditText time = findViewById(R.id.requestedTime);
        EditText extra = findViewById(R.id.requestExtraInfo);
        request.setOnClickListener(view -> {
            Toast.makeText(getApplicationContext(),"Your request has been sent", Toast.LENGTH_LONG).show();
        });
    }
}
