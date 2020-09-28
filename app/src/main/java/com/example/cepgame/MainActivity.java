package com.example.cepgame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickClient(View v){
        startActivity(new Intent(this, Connect.class));
    }

    public void onClickServer(View v) {
        startActivity(new Intent(this, CreateServer.class));
    }
}