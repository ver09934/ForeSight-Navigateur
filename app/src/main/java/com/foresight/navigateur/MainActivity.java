package com.foresight.navigateur;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //System.out.println("MAPS KEY: " + getString(R.string.google_maps_key));
        //System.out.println("DIRECTIONS KEY: " + getString(R.string.directions_key));
    }

    /** Called when the user taps the Send button */
    public void openMap(View view) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }
}
