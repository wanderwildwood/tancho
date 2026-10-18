package com.wanderwildwood.tancho;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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

    /**
     * Android 15 and later draw every window of an app that targets them under the status bar,
     * and leave the bar clear, so its icons sit on the app's white. They are white too unless
     * asked otherwise, and the top of the screen reads as an empty gap. So here every screen
     * asks for dark icons and keeps its content below the bar. Mudita's own OS (Android 12)
     * never gets here, and its bar stays as it was.
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < 35) return;
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        View content = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            int bars = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            Insets i = insets.getInsets(bars);
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return new WindowInsetsCompat.Builder(insets).setInsets(bars, Insets.NONE).build();
        });
        ViewCompat.requestApplyInsets(content);
    }
}
