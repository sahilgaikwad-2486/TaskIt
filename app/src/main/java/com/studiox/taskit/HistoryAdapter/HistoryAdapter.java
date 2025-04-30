package com.studiox.taskit.HistoryAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.studiox.taskit.DatabaseHelper.DatabaseHelper;
import com.studiox.taskit.HistoryModel.HistoryModel;
import com.studiox.taskit.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<HistoryModel> taskHistory;
    private final Context context;
    private final DatabaseHelper dbHelper;
    private final OnHistoryChangedListener historyChangedListener;

    public HistoryAdapter(Context context, List<HistoryModel> taskHistory, OnHistoryChangedListener listener) {
        this.context = context;
        this.taskHistory = taskHistory;
        this.dbHelper = new DatabaseHelper(context);
        this.historyChangedListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.taskhistory_recyclerview_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryModel model = taskHistory.get(position);
        holder.taskTextView.setText(model.getTask());

        holder.deleteTask.setOnClickListener(v -> {
            String taskName = model.getTask();

            if (model.getId() != null && !model.getId().isEmpty()) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user != null) {
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .collection("history")
                            .document(model.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                taskHistory.remove(holder.getAdapterPosition());
                                notifyItemRemoved(holder.getAdapterPosition());
                                if (taskHistory.isEmpty()) {
                                    historyChangedListener.onHistoryChanged();
                                }
                            });
                }
            } else {
                dbHelper.deleteHistoryTask(taskName);
                taskHistory.remove(holder.getAdapterPosition());
                notifyItemRemoved(holder.getAdapterPosition());
                if (taskHistory.isEmpty()) {
                    historyChangedListener.onHistoryChanged();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskHistory.size();
    }

    public interface OnHistoryChangedListener {
        void onHistoryChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView taskTextView;
        ImageView deleteTask;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTextView = itemView.findViewById(R.id.taskTextView);
            deleteTask = itemView.findViewById(R.id.deleteTask);
        }
    }
}
