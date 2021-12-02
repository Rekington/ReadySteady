package com.example.readysteady.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.readysteady.R;
import com.example.readysteady.models.LoginModel;

public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LoginModel loginModel = (LoginModel) getIntent().getSerializableExtra("loginUser");
                String role = loginModel.getRole();
                String email = loginModel.getUsername();
                Intent login;
                if (role.equals("driver")) {
                    if (!email.equals("driver@gmail.com")){
                        Toast.makeText(getApplicationContext(),"You username and password is not registered. Please register yourself", Toast.LENGTH_LONG).show();
                        return;
                    }
                    login = new Intent(getApplicationContext(), MapDriverActivity.class);
                } else {
                    if (!email.equals("rider@gmail.com")){
                        Toast.makeText(getApplicationContext(),"You username and password is not registered. Please register yourself", Toast.LENGTH_LONG).show();
                        return;
                    }
                    login = new Intent(getApplicationContext(), MapRiderActivity.class);
                }
                login.putExtra("loginUser", loginModel);
                startActivity(login);
                finish();
            }
        }, 2000);
        ImageView img_ani = (ImageView) findViewById(R.id.cab);

        // New movement from left to right (fromXDelta = Start,toXDelta = end)
        TranslateAnimation animation = new TranslateAnimation(-750.0f, 1200.0f,
                0.0f, 0.0f);

        animation.setDuration(2500);  // animation duration
        animation.setRepeatCount(5);  // animation repeat count
        animation.setRepeatMode(1);   // repeat animation (left to right, right to left )
        img_ani.startAnimation(animation);  // start animation
    }
}
