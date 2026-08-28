package com.wanderwildwood.tancho;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyTheme();
    }

    /**
     * Called before anything is drawn, and after the manifest's theme has been applied, so
     * this is what an activity overrides to choose its own. Whatever android:theme says is
     * replaced by what happens here.
     */
    protected void applyTheme() {
        setTheme(R.style.AppTheme);
    }
}
