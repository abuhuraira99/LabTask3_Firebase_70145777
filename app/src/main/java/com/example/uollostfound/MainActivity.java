package com.example.uollostfound;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements LostItemAdapter.OnItemClickListener {

    private final ArrayList<LostItem> items = new ArrayList<>();
    private LostItemAdapter adapter;

    private DatabaseReference lostFoundRef;
    private ValueEventListener boardListener;

    private DatabaseReference connectionRef;
    private ValueEventListener connectionListener;
    private final Handler chipHandler = new Handler(Looper.getMainLooper());
    private final Runnable showOffline = () -> setLiveChip(false);

    private RecyclerView rvItems;
    private View progressBoard, tapHint;
    private TextView tvSummary, tvLive;
    private ImageView ivLiveDot;
    private ExtendedFloatingActionButton fabPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // White status-bar icons over the navy app bar, dark icons over the light canvas below
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
        setContentView(R.layout.activity_main);

        rvItems = findViewById(R.id.rvItems);
        progressBoard = findViewById(R.id.progressBoard);
        tapHint = findViewById(R.id.tapHint);
        tvSummary = findViewById(R.id.tvSummary);
        tvLive = findViewById(R.id.tvLive);
        ivLiveDot = findViewById(R.id.ivLiveDot);
        fabPost = findViewById(R.id.fabPost);
        applyWindowInsets();

        adapter = new LostItemAdapter(items, this);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);
        shrinkFabWhileScrolling();

        fabPost.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PostItemActivity.class)));

        lostFoundRef = FirebaseDatabase.getInstance().getReference(PostItemActivity.NODE_LOST_FOUND);
        attachBoardListener();
        watchConnection();
    }

    /**
     * The live read. onDataChange() runs once with every post and then again every time
     * anything under LostFound changes, on every phone that has the board open.
     */
    private void attachBoardListener() {
        boardListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                items.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    LostItem item = child.getValue(LostItem.class);
                    if (item != null) {
                        if (item.getId() == null) {
                            item.setId(child.getKey());
                        }
                        items.add(item);
                    }
                }
                // push() keys are time-ordered, so the newest post is last: reverse to show it first
                Collections.reverse(items);
                adapter.notifyDataSetChanged();

                progressBoard.setVisibility(View.GONE);
                updateSummary();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBoard.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this,
                        getString(R.string.toast_read_failed, error.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        };
        lostFoundRef.addValueEventListener(boardListener);
    }

    @Override
    public void onItemClick(int position) {
        // Marking an item as Returned is added in the next step
    }

    private void updateSummary() {
        int returned = 0;
        for (LostItem item : items) {
            if (LostItemAdapter.STATUS_RETURNED.equals(item.getStatus())) {
                returned++;
            }
        }
        int open = items.size() - returned;
        String posts = getResources().getQuantityString(R.plurals.post_count, items.size(), items.size());
        tvSummary.setText(getString(R.string.board_summary, posts, open, returned));
        tapHint.setVisibility(open > 0 ? View.VISIBLE : View.GONE);
    }

    /** Shows whether the board is connected to Firebase or showing what it already has. */
    private void watchConnection() {
        connectionRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                chipHandler.removeCallbacks(showOffline);
                if (connected != null && connected) {
                    setLiveChip(true);
                } else {
                    chipHandler.postDelayed(showOffline, 1500);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        connectionRef.addValueEventListener(connectionListener);
    }

    private void setLiveChip(boolean online) {
        tvLive.setText(online ? R.string.live_online : R.string.live_offline);
        int color = ContextCompat.getColor(this, online ? R.color.status_live : R.color.status_offline);
        ivLiveDot.setImageTintList(ColorStateList.valueOf(color));
        tvLive.setTextColor(ContextCompat.getColor(this,
                online ? R.color.found_text : R.color.text_secondary));
    }

    private void shrinkFabWhileScrolling() {
        rvItems.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 6 && fabPost.isExtended()) {
                    fabPost.shrink();
                } else if (dy < -6 && !fabPost.isExtended()) {
                    fabPost.extend();
                }
            }
        });
    }

    private void applyWindowInsets() {
        int fabMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        int listBottom = getResources().getDimensionPixelSize(R.dimen.board_bottom_space);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBar), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, 0);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(fabPost, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = fabMargin + bars.bottom;
            lp.rightMargin = fabMargin + bars.right;
            v.setLayoutParams(lp);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(rvItems, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    listBottom + bars.bottom);
            return insets;
        });
    }
}
