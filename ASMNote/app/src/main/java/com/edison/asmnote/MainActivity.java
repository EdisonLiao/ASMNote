package com.edison.asmnote;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hookLoggg();
    }

    private void hookLoggg(){
        TextView tv = findViewById(R.id.tv_hello);
        tv.setText("Hello Kitty");
    }

}
