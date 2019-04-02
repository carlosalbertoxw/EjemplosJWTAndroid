package com.carlosalbertoxw.ejemplos_jwt_android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JWT jwt = new JWT();

        String token = jwt.createJWT(5);

        Log.d("jwt","token:"+token);

        Integer id = jwt.verifyJWT(token);

        Log.d("jwt","id:"+id.toString());
    }
}
