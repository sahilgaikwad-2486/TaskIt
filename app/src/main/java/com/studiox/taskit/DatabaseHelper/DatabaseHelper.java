package com.studiox.taskit.DatabaseHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE = "TaskIt";
    private static final int VERSION = 5;

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,task TEXT, description TEXT, date TEXT, time TEXT, completed INTEGER DEFAULT 0, reminder INTEGER DEFAULT 0, repeat TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS history(id INTEGER PRIMARY KEY AUTOINCREMENT, task TEXT, deleted_at TEXT)");
    }

    /*@Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS tasks");
        onCreate(db);
    }*/

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN completed INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN reminder INTEGER DEFAULT 0");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS history(id INTEGER PRIMARY KEY AUTOINCREMENT, task TEXT, deleted_at TEXT)");
        }

        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN repeat TEXT");
        }

    }


    public void insertData(String task, String description, String date, String time, boolean reminder, String repeat) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("task", task);
        values.put("description", description);
        values.put("date", date);
        values.put("time", time);
        values.put("completed", 0);
        values.put("reminder", reminder ? 1 : 0);
        values.put("repeat", repeat);
        database.insert("tasks", null, values);
    }

    public Cursor showData() {
        SQLiteDatabase database = this.getReadableDatabase();
        return database.rawQuery("SELECT * FROM tasks", null);
    }

    public void deleteData(String id) {
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete("tasks", "id=?", new String[]{id});
    }

    public Cursor getCompletedTasks() {
        SQLiteDatabase database = this.getReadableDatabase();
        return database.rawQuery("SELECT * FROM tasks WHERE completed=1", null);
    }

    public void updateTaskCompletion(int id, boolean isCompleted) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("completed", isCompleted ? 1 : 0);
        database.update("tasks", values, "id=?", new String[]{String.valueOf(id)});
        database.close();
    }

    public Cursor getPendingTasks() {
        SQLiteDatabase database = this.getReadableDatabase();
        return database.rawQuery("SELECT * FROM tasks WHERE completed=0", null);
    }

    public int getCurrentTaskCount() {
        SQLiteDatabase database = this.getReadableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String todayDate = sdf.format(Calendar.getInstance().getTime());

        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM tasks WHERE date = ? AND completed = 0", new String[]{todayDate});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }


    public Cursor getTodaysTasks() {
        SQLiteDatabase database = this.getReadableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String todayDate = sdf.format(Calendar.getInstance().getTime());

        return database.rawQuery("SELECT * FROM tasks WHERE date = ? AND completed = 0", new String[]{todayDate});
    }


    public int getTaskCountByStatus(boolean isCompleted) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM tasks WHERE completed = ?";
        Cursor cursor = db.rawQuery(query, new String[]{isCompleted ? "1" : "0"});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getTotalTaskCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM tasks", null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getLastInsertedTaskId() {
        SQLiteDatabase database = this.getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT id FROM tasks ORDER BY id DESC LIMIT 1", null);
        int lastTaskId = -1;
        if (cursor.moveToFirst()) {
            lastTaskId = cursor.getInt(0);
        }
        cursor.close();
        database.close();
        return lastTaskId;
    }

    public void clearDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM tasks"); // Replace 'tasks' with your actual table name
        db.close();
    }

    public void insertHistory(String taskName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("task", taskName);

        db.insert("history", null, values);
        db.close();
    }

    public Cursor getHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM history ORDER BY id DESC", null);
    }

    public void deleteHistoryTask(String taskName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("history", "task=?", new String[]{taskName});
        db.close();
    }

    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM history");
        db.close();
    }


}
