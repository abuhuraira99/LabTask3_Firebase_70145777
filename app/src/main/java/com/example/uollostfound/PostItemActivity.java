package com.example.uollostfound;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PostItemActivity extends AppCompatActivity {

    static final String NODE_LOST_FOUND = "LostFound";
    static final String STATUS_OPEN = "Open";

    private TextInputLayout tilItemName, tilLocation, tilContact;
    private TextInputEditText etItemName, etLocation, etContact;
    private RadioGroup rgType;
    private MaterialButton btnPost;
    private View offlineBanner;

    private DatabaseReference lostFoundRef;
    private DatabaseReference connectionRef;
    private ValueEventListener connectionListener;
    private boolean isConnected = true;
    private boolean isPosting = false;

    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final Runnable showOfflineBanner = () -> offlineBanner.setVisibility(View.VISIBLE);

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
        offlineBanner = findViewById(R.id.offlineBanner);

        // Every post is stored under the LostFound node of the Realtime Database
        lostFoundRef = FirebaseDatabase.getInstance().getReference(NODE_LOST_FOUND);
        watchConnection();

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

    /**
     * Writes one post under LostFound:
     * push().getKey() makes a new time-ordered key, the key is stored as the item's id with
     * status "Open", and child(key).setValue(item) writes it. The completion listener runs
     * once the Firebase server has accepted (or rejected) the write.
     */
    private void savePost() {
        String key = lostFoundRef.push().getKey();
        if (key == null) {
            Toast.makeText(this, getString(R.string.toast_post_failed, "no key"),
                    Toast.LENGTH_LONG).show();
            return;
        }

        LostItem item = new LostItem(
                key,
                textOf(etItemName),
                selectedType(),
                textOf(etLocation),
                textOf(etContact),
                STATUS_OPEN);

        setPosting(true);
        lostFoundRef.child(key).setValue(item, (error, ref) -> {
            if (error == null) {
                Toast.makeText(PostItemActivity.this, R.string.toast_posted,
                        Toast.LENGTH_SHORT).show();
                finish();
            } else {
                setPosting(false);
                Toast.makeText(PostItemActivity.this,
                        getString(R.string.toast_post_failed, error.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /** While a write is pending the form is locked so the same post cannot be sent twice. */
    private void setPosting(boolean posting) {
        isPosting = posting;
        btnPost.setEnabled(!posting);
        etItemName.setEnabled(!posting);
        etLocation.setEnabled(!posting);
        etContact.setEnabled(!posting);
        for (int i = 0; i < rgType.getChildCount(); i++) {
            rgType.getChildAt(i).setEnabled(!posting);
        }
        updatePostButtonText();
    }

    private void updatePostButtonText() {
        if (!isPosting) {
            btnPost.setText(R.string.btn_post);
        } else {
            btnPost.setText(isConnected ? R.string.btn_posting : R.string.btn_waiting);
        }
    }

    /**
     * .info/connected is a special Firebase location that is true while the app is connected
     * to the database. It drives the offline banner, so the student knows the post is queued.
     */
    private void watchConnection() {
        connectionRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                isConnected = connected != null && connected;
                bannerHandler.removeCallbacks(showOfflineBanner);
                if (isConnected) {
                    offlineBanner.setVisibility(View.GONE);
                } else {
                    // Short grace period: the first value is "false" until the socket opens
                    bannerHandler.postDelayed(showOfflineBanner, 1500);
                }
                updatePostButtonText();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Nothing to show: the form still works without the banner
            }
        };
        connectionRef.addValueEventListener(connectionListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bannerHandler.removeCallbacks(showOfflineBanner);
        if (connectionRef != null && connectionListener != null) {
            connectionRef.removeEventListener(connectionListener);
        }
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
