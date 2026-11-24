package com.example.uollostfound;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // White status-bar icons over the navy app bar, dark icons over the light canvas below
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
        setContentView(R.layout.activity_main);

        ExtendedFloatingActionButton fabPost = findViewById(R.id.fabPost);
        applyWindowInsets(fabPost);

        fabPost.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PostItemActivity.class)));
    }

    private void applyWindowInsets(ExtendedFloatingActionButton fab) {
        int fabMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBar), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, 0);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(fab, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = fabMargin + bars.bottom;
            lp.rightMargin = fabMargin + bars.right;
            v.setLayoutParams(lp);
            return insets;
        });
    }
}
