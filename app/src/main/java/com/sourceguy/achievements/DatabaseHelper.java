package com.sourceguy.achievements;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "achievements.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
            "CREATE TABLE categories (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL)"
        );

        db.execSQL(
            "CREATE TABLE achievements (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "description TEXT, " +
            "category_id INTEGER NOT NULL, " +
            "FOREIGN KEY(category_id) REFERENCES categories(id) " +
            "ON DELETE CASCADE)"
        );

        db.execSQL(
            "CREATE TABLE tiers (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "achievement_id INTEGER NOT NULL, " +
            "name TEXT NOT NULL, " +
            "points INTEGER NOT NULL, " +
            "completed INTEGER DEFAULT 0, " +
            "FOREIGN KEY(achievement_id) REFERENCES achievements(id) " +
            "ON DELETE CASCADE)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {
        // Database migrations will go here.
    }

    // CATEGORIES

    public long addCategory(String name) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);

        return db.insert("categories", null, values);
    }

    public List<Category> getCategories() {
        List<Category> categories = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT id, name FROM categories", null)) {

            while (cursor.moveToNext()) {
                categories.add(new Category(
                    cursor.getInt(0),
                    cursor.getString(1)
                ));
            }
        }

        return categories;
    }

    // ACHIEVEMENTS

    public long addAchievement(String name,
                               String description,
                               int categoryId) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("description", description);
        values.put("category_id", categoryId);

        return db.insert("achievements", null, values);
    }

    // TIERS

    public long addTier(int achievementId,
                        String name,
                        int points) {

        if (points < 0) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("achievement_id", achievementId);
        values.put("name", name);
        values.put("points", points);

        return db.insert("tiers", null, values);
    }

    public boolean setTierCompleted(int tierId,
                                    boolean completed) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("completed", completed ? 1 : 0);

        return db.update(
            "tiers",
            values,
            "id = ?",
            new String[]{String.valueOf(tierId)}
        ) > 0;
    }

    // POINTS

    public int getTotalPoints() {
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
            "SELECT COALESCE(SUM(points), 0) " +
            "FROM tiers WHERE completed = 1",
            null)) {

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    public int getCategoryPoints(int categoryId) {
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
            "SELECT COALESCE(SUM(t.points), 0) " +
            "FROM tiers t " +
            "INNER JOIN achievements a " +
            "ON t.achievement_id = a.id " +
            "WHERE a.category_id = ? AND t.completed = 1",
            new String[]{String.valueOf(categoryId)})) {

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }
    
    public boolean deleteCategory(int categoryId) {
        SQLiteDatabase db = getWritableDatabase();

        return db.delete(
            "categories",
            "id = ?",
            new String[]{String.valueOf(categoryId)}
        ) > 0;
    }

    public boolean deleteAchievement(int achievementId) {
        SQLiteDatabase db = getWritableDatabase();

        return db.delete(
            "achievements",
            "id = ?",
            new String[]{String.valueOf(achievementId)}
        ) > 0;
    }

}
