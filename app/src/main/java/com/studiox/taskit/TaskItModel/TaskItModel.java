package com.studiox.taskit.TaskItModel;

public class TaskItModel {

    private String task, description, date, time, firebaseId, repeat;
    private int localId;
    private boolean completed;
    private boolean reminder;

    public TaskItModel() {
    }

    public TaskItModel(int localId, String firebaseId, String task, String description, String date, String time, boolean completed, boolean reminder, String repeat) {
        this.localId = localId;
        this.firebaseId = firebaseId;
        this.task = task;
        this.description = description;
        this.date = date;
        this.time = time;
        this.completed = completed;
        this.reminder = reminder;
        this.repeat = repeat;
    }

    public String getFirebaseId() {
        return firebaseId;
    }

    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isReminder() {
        return reminder;
    }

    public void setReminder(boolean reminder) {
        this.reminder = reminder;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }
}
