package com.studiox.taskit.TaskItAdapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.studiox.taskit.DatabaseHelper.DatabaseHelper;
import com.studiox.taskit.HistoryModel.HistoryModel;
import com.studiox.taskit.Notification.NotificationHelper;
import com.studiox.taskit.R;
import com.studiox.taskit.TaskDetailsActivity;
import com.studiox.taskit.TaskItModel.TaskItModel;
import com.studiox.taskit.TaskUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TaskItAdapter extends RecyclerView.Adapter<TaskItAdapter.ViewHolder> {

    private final List<TaskItModel> taskListFull;
    Context context;
    ArrayList<TaskItModel> arrayList;
    TextView noTaskFoundTextView;
    ImageView noTaskFoundImageView;

    public TaskItAdapter(Context context, ArrayList<TaskItModel> arrayList, TextView noTaskFoundTextView, ImageView noTaskFoundImageView) {
        this.context = context;
        this.arrayList = arrayList;
        this.taskListFull = new ArrayList<>(arrayList);
        this.noTaskFoundTextView = noTaskFoundTextView;
        this.noTaskFoundImageView = noTaskFoundImageView;
    }

    @NonNull
    @Override
    public TaskItAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.task_recyclerview_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskItAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        final TaskItModel task = arrayList.get(position);


        holder.displayTask.setText(arrayList.get(position).getTask());
        holder.displayDescription.setText(arrayList.get(position).getDescription());
        holder.displaydate.setText(arrayList.get(position).getDate());
        String date = arrayList.get(position).getDate();
        if (date == null || date.trim().isEmpty()) {
            holder.displaydate.setText("Repeat : EveryDay");
        } else {
            holder.displaydate.setText(date);
        }
        holder.taskCheckbox.setOnCheckedChangeListener(null);
        holder.taskCheckbox.setChecked(task.isCompleted());

        if (task.isReminder()) {
            holder.taskReminder.setVisibility(View.VISIBLE);
        } else {
            holder.taskReminder.setVisibility(View.GONE);
        }

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String todayDate = sdf.format(Calendar.getInstance().getTime()).trim();
        String taskDate = task.getDate().trim();

        if (task.isCompleted()) {
            holder.itemView.setAlpha(0.6f);
            holder.statusColorBackground.setBackgroundColor(Color.parseColor("#6C63FF")); // Completed (Purple)
        } else if (taskDate.equals(todayDate)) {
            holder.itemView.setAlpha(1.0f);
            holder.statusColorBackground.setBackgroundColor(Color.parseColor("#FFD700")); // Pending but due today (Yellow)
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.statusColorBackground.setBackgroundColor(Color.parseColor("#FF6363")); // Pending (Red)
        }

        holder.taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            databaseHelper.updateTaskCompletion(task.getLocalId(), isChecked);
            task.setCompleted(isChecked);

            if (currentUser != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String userId = currentUser.getUid();

                db.collection("users").document(userId)
                        .collection("tasks")
                        .document(task.getFirebaseId()) // Ensure correct task
                        .update("completed", isChecked)
                        .addOnSuccessListener(aVoid -> {
                            TaskUtils.updateTaskStatsInFirestore();
                            task.setCompleted(isChecked); // Update local model
                            if (isChecked) {
                                NotificationHelper.firebasecancelNotification(context, task.getFirebaseId());
                                holder.itemView.setAlpha(0.6f);
                                holder.statusColorBackground.setBackgroundColor(Color.parseColor("#6C63FF")); // Completed
                            } else if (task.getDate().trim().equals(todayDate)) {
                                holder.itemView.setAlpha(1.0f);
                                holder.statusColorBackground.setBackgroundColor(Color.parseColor("#FFD700")); // Due today
                            } else {
                                holder.itemView.setAlpha(1.0f);
                                holder.statusColorBackground.setBackgroundColor(Color.parseColor("#FF6363")); // Pending
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Failed to update task!", Toast.LENGTH_SHORT).show()
                        );

            }

            if (isChecked) {
                NotificationHelper.cancelNotification(context, task.getLocalId());
                holder.itemView.setAlpha(0.6f);
                holder.statusColorBackground.setBackgroundColor(Color.parseColor("#6C63FF")); // Completed
            } else if (task.getDate().trim().equals(todayDate)) {
                holder.itemView.setAlpha(1.0f);
                holder.statusColorBackground.setBackgroundColor(Color.parseColor("#FFD700")); // Due today
            } else {
                holder.itemView.setAlpha(1.0f);
                holder.statusColorBackground.setBackgroundColor(Color.parseColor("#FF6363")); // Pending
            }
        });

        holder.deleteTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String taskName = arrayList.get(position).getTask();
                String taskDescription = arrayList.get(position).getDescription();

                new MaterialAlertDialogBuilder(context)
                        .setTitle("Delete Task!")
                        .setMessage("Task: " + taskName + "\nDescription: " + taskDescription)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                                FirebaseUser currentUser = mAuth.getCurrentUser();
                                DatabaseHelper databaseHelper = new DatabaseHelper(context);
                                TaskItModel task = arrayList.get(position);

                                if (currentUser != null) {
                                    // Delete from Firebase Firestore
                                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                                    String userId = currentUser.getUid();

                                    db.collection("users").document(userId)
                                            .collection("tasks")
                                            .document(task.getFirebaseId())
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                db.collection("users").document(userId)
                                                        .collection("history")
                                                        .add(new HistoryModel(task.getTask()))
                                                        .addOnSuccessListener(historyRef -> {
                                                            TaskUtils.updateTaskStatsInFirestore();
                                                            NotificationHelper.firebasecancelNotification(context, task.getFirebaseId());
                                                            removeTask(position);
                                                            Toast.makeText(context, "Task deleted!", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> Toast.makeText(context, "Deleted, but failed to add to history", Toast.LENGTH_SHORT).show());
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(context, "Failed to delete task!", Toast.LENGTH_SHORT).show()
                                            );
                                } else {
                                    // Delete from SQLite when offline
                                    databaseHelper.deleteData(String.valueOf(task.getLocalId()));
                                    NotificationHelper.cancelNotification(context, task.getLocalId());
                                    removeTask(position);
                                    databaseHelper.insertHistory(task.getTask());
                                    Toast.makeText(context, "Task deleted!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        holder.taskDetailsBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, TaskDetailsActivity.class);
                intent.putExtra("task_id", task.getLocalId());
                intent.putExtra("name", task.getTask());
                intent.putExtra("description", task.getDescription());
                intent.putExtra("date", task.getDate());
                intent.putExtra("time", task.getTime());
                intent.putExtra("repeat", task.getRepeat());
                intent.putExtra("reminder", task.isReminder() ? "On" : "Off");
                context.startActivity(intent);
            }
        });

    }


    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public void filterList(String query) {
        List<TaskItModel> filteredList = new ArrayList<>();

        if (query.isEmpty()) {
            filteredList.addAll(taskListFull);
        } else {
            for (TaskItModel task : taskListFull) {
                if (task.getTask().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(task);
                }
            }
        }

        arrayList.clear();
        arrayList.addAll(filteredList);
        notifyDataSetChanged();

        if (filteredList.isEmpty()) {
            noTaskFoundImageView.setVisibility(View.VISIBLE);
            noTaskFoundTextView.setVisibility(View.VISIBLE);
        } else {
            noTaskFoundImageView.setVisibility(View.GONE);
            noTaskFoundTextView.setVisibility(View.GONE);
        }
    }

    public void updateTaskList(List<TaskItModel> newList) {
        arrayList.clear();
        arrayList.addAll(newList);

        taskListFull.clear();
        taskListFull.addAll(newList);

        notifyDataSetChanged();
    }

    // Method to remove task from RecyclerView and update UI
    private void removeTask(int position) {
        TaskItModel deletedTask = arrayList.get(position);
        arrayList.remove(position);
        taskListFull.remove(deletedTask);  // Ensure consistency in both lists

        // Notify RecyclerView
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, arrayList.size());

        // Update UI if the task list is empty
        if (arrayList.isEmpty()) {
            noTaskFoundImageView.setVisibility(View.VISIBLE);
            noTaskFoundTextView.setVisibility(View.VISIBLE);
        } else {
            noTaskFoundImageView.setVisibility(View.GONE);
            noTaskFoundTextView.setVisibility(View.GONE);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView displayTask, displayDescription, displaydate;
        LinearLayout deleteTask, taskDetailsBackground, statusColorBackground, taskReminder;
        CheckBox taskCheckbox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            displayTask = itemView.findViewById(R.id.taskTextView);
            displayDescription = itemView.findViewById(R.id.descriptionTextView);
            displaydate = itemView.findViewById(R.id.dateTextView);
            deleteTask = itemView.findViewById(R.id.deleteTask);
            taskCheckbox = itemView.findViewById(R.id.taskCheckBox);
            taskDetailsBackground = itemView.findViewById(R.id.taskDetailsBackground);
            statusColorBackground = itemView.findViewById(R.id.statuscolor_background);
            taskReminder = itemView.findViewById(R.id.taskReminder);
        }
    }
}
