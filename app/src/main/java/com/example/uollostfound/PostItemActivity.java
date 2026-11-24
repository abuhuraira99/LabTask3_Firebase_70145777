package com.example.uollostfound;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.widget.RadioGroup;

public class PostItemActivity extends AppCompatActivity {

    private TextInputLayout tilItemName, tilLocation, tilContact;
    private TextInputEditText etItemName, etLocation, etContact;
    private RadioGroup rgType;
    private MaterialButton btnPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
        setContentView(R.layout.activity_post_item);
        applyWindowInsets();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        tilItemName = findViewById(R.id.tilItemName);
        tilLocation = findViewById(R.id.tilLocation);
        tilContact = findViewById(R.id.tilContact);
        etItemName = findViewById(R.id.etItemName);
        etLocation = findViewById(R.id.etLocation);
        etContact = findViewById(R.id.etContact);
        rgType = findViewById(R.id.rgType);
        btnPost = findViewById(R.id.btnPost);

        // An error disappears as soon as the student starts typing in that field
        clearErrorWhileTyping(etItemName, tilItemName);
        clearErrorWhileTyping(etLocation, tilLocation);
        clearErrorWhileTyping(etContact, tilContact);

        btnPost.setOnClickListener(v -> {
            if (validateForm()) {
                savePost();
            }
        });
    }

    /** Refuses empty fields: each empty field gets its own message through setError(). */
    private boolean validateForm() {
        boolean valid = true;
        TextInputLayout firstInvalid = null;

        if (textOf(etItemName).isEmpty()) {
            tilItemName.setError(getString(R.string.error_item_name));
            firstInvalid = tilItemName;
            valid = false;
        }
        if (textOf(etLocation).isEmpty()) {
            tilLocation.setError(getString(R.string.error_location));
            if (firstInvalid == null) firstInvalid = tilLocation;
            valid = false;
        }
        if (textOf(etContact).isEmpty()) {
            tilContact.setError(getString(R.string.error_contact));
            if (firstInvalid == null) firstInvalid = tilContact;
            valid = false;
        }

        if (firstInvalid != null && firstInvalid.getEditText() != null) {
            firstInvalid.getEditText().requestFocus();
        }
        return valid;
    }

    private void savePost() {
        // The Firebase write is added in the next step
        btnPost.setEnabled(false);
    }

    private String selectedType() {
        return rgType.getCheckedRadioButtonId() == R.id.rbFound
                ? getString(R.string.type_found)
                : getString(R.string.type_lost);
    }

    private static String textOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private static void clearErrorWhileTyping(TextInputEditText field, TextInputLayout layout) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) {
                    layout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /** Pads the app bar below the status bar and keeps the Post button above the keyboard. */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBar), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, 0);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.postRoot), (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(nav.left, 0, nav.right, Math.max(nav.bottom, ime.bottom));
            return insets;
        });
    }
}
