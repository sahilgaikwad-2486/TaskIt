package com.studiox.taskit.HistoryModel;

public class HistoryModel {
    private String task;
    private String id;

    public HistoryModel() {
    }

    public HistoryModel(String task) {
        this.task = task;
    }

    public HistoryModel(String task, String id) {
        this.task = task;
        this.id = id;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
