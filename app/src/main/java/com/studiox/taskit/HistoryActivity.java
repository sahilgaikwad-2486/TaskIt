package com.studiox.taskit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.studiox.taskit.DatabaseHelper.DatabaseHelper;
import com.studiox.taskit.HistoryAdapter.HistoryAdapter;
import com.studiox.taskit.HistoryModel.HistoryModel;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    LinearLayout backbutton, deleteAllTask;
    RecyclerView taskHistoryRecyclerView;
    HistoryAdapter historyAdapter;
    DatabaseHelper dbHelper;
    Cursor cursor;
    List<HistoryModel> deletedTasks;
    ImageView historyImageView;
    TextView historyTextView;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    FirebaseFirestore firestore;
    SwipeRefreshLayout swipeRefreshLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views and Firebase
        backbutton = findViewById(R.id.historypagebackbutton);
        taskHistoryRecyclerView = findViewById(R.id.taskHistoryRecyclerView);
        deleteAllTask = findViewById(R.id.deletealltask);
        historyImageView = findViewById(R.id.historyimageView);
        historyTextView = findViewById(R.id.historyTextView);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadHistory();
            swipeRefreshLayout.setRefreshing(false);
        });

        if (currentUser != null && !isNetworkAvailable()) {
            showThemedSnackbar(findViewById(android.R.id.content), "No internet connection!");
        }

        deletedTasks = new ArrayList<>();
        historyAdapter = new HistoryAdapter(this, deletedTasks, this::updateEmptyHistoryUI);
        taskHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskHistoryRecyclerView.setAdapter(historyAdapter);

        loadHistory();

        deleteAllTask.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(HistoryActivity.this)
                    .setTitle("Clear All History")
                    .setMessage("Are you sure you want to clear all history ?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (currentUser != null) {
                            firestore.collection("users")
                                    .document(currentUser.getUid())
                                    .collection("history")
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        int total = querySnapshot.size();
                                        if (total == 0) {
                                            deletedTasks.clear();
                                            historyAdapter.notifyDataSetChanged();
                                            updateEmptyHistoryUI();
                                            return;
                                        }

                                        final int[] deletedCount = {0};
                                        for (QueryDocumentSnapshot doc : querySnapshot) {
                                            doc.getReference().delete().addOnSuccessListener(aVoid -> {
                                                deletedCount[0]++;
                                                if (deletedCount[0] == total) {
                                                    deletedTasks.clear();
                                                    historyAdapter.notifyDataSetChanged();
                                                    updateEmptyHistoryUI();
                                                }
                                            });
                                        }
                                    });
                        } else {
                            dbHelper.clearHistory();
                            deletedTasks.clear();
                            historyAdapter.notifyDataSetChanged();
                            updateEmptyHistoryUI();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // Back button
        backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                Intent intent = new Intent(HistoryActivity.this, TaskActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void performHapticFeedback() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    private void loadHistory() {
        deletedTasks.clear();
        if (currentUser != null) {
            firestore.collection("users")
                    .document(currentUser.getUid())
                    .collection("history")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String taskName = doc.getString("task");
                            if (taskName != null) {
                                deletedTasks.add(new HistoryModel(taskName, doc.getId())); // 🔥 FIXED
                            }
                        }
                        historyAdapter.notifyDataSetChanged();
                        updateEmptyHistoryUI();
                    });
        } else {
            cursor = dbHelper.getHistory();
            if (cursor.moveToFirst()) {
                do {
                    String taskName = cursor.getString(cursor.getColumnIndexOrThrow("task"));
                    deletedTasks.add(new HistoryModel(taskName));
                } while (cursor.moveToNext());
            }
            cursor.close();
            historyAdapter.notifyDataSetChanged();
            updateEmptyHistoryUI();
        }
    }


    private void updateEmptyHistoryUI() {
        if (deletedTasks == null || deletedTasks.isEmpty()) {
            historyImageView.setVisibility(View.VISIBLE);
            historyTextView.setVisibility(View.VISIBLE);
        } else {
            historyImageView.setVisibility(View.GONE);
            historyTextView.setVisibility(View.GONE);
        }
    }

    private void showThemedSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

        View snackbarView = snackbar.getView();
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
        params.setMargins(70, 0, 70, 50);

        snackbarView.setLayoutParams(params);
        snackbar.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(HistoryActivity.this, TaskActivity.class);
        startActivity(intent);
        finish();
    }
}
