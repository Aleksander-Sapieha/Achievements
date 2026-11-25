package com.sourceguy.achievements;


import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.*;

public class AchievementDetailsActivity extends Activity {

    private DatabaseHelper database;
    private int achievementId;

    private TextView title;
    private TextView description;
    private TextView category;
    private TextView points;

    private LinearLayout tierContainer;

    private int dp(int value) {
        return (int) (value * getResources()
            .getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievement_details);

        database = new DatabaseHelper(this);

        achievementId = getIntent().getIntExtra(
            "achievement_id", -1
        );

        if (achievementId == -1) {
            finish();
            return;
        }

        title = findViewById(R.id.achievementTitle);
        description = findViewById(R.id.achievementDescription);
        category = findViewById(R.id.achievementCategory);
        points = findViewById(R.id.achievementPoints);

        tierContainer = findViewById(R.id.tierContainer);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        findViewById(R.id.addTierButton).setOnClickListener(
            v -> showAddTierDialog()
        );

        refreshUI();
    }

    private void refreshUI() {

        SQLiteDatabase db = database.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
            "SELECT a.name, a.description, c.name " +
            "FROM achievements a " +
            "INNER JOIN categories c " +
            "ON a.category_id = c.id " +
            "WHERE a.id = ?",
            new String[]{String.valueOf(achievementId)}
        )) {

            if (!cursor.moveToFirst()) {
                finish();
                return;
            }

            title.setText(cursor.getString(0));

            String desc = cursor.getString(1);
            description.setText(
                desc == null || desc.isEmpty()
                    ? "No description."
                    : desc
            );

            category.setText("Category: " + cursor.getString(2));
        }

        try (Cursor cursor = db.rawQuery(
            "SELECT COALESCE(SUM(points), 0) " +
            "FROM tiers " +
            "WHERE achievement_id = ? AND completed = 1",
            new String[]{String.valueOf(achievementId)}
        )) {
            if (cursor.moveToFirst()) {
                points.setText(cursor.getInt(0) + " XP");
            }
        }

        refreshTiers();
    }

    private void refreshTiers() {

        tierContainer.removeAllViews();

        SQLiteDatabase db = database.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
            "SELECT id, name, points, completed " +
            "FROM tiers WHERE achievement_id = ? " +
            "ORDER BY id ASC",
            new String[]{String.valueOf(achievementId)}
        )) {

            if (cursor.getCount() == 0) {
                TextView empty = new TextView(this);
                empty.setText("No tiers yet. Add your first tier!");
                empty.setTextColor(Color.LTGRAY);
                tierContainer.addView(empty);
                return;
            }

            while (cursor.moveToNext()) {

                int tierId = cursor.getInt(0);
                String tierName = cursor.getString(1);
                int tierPoints = cursor.getInt(2);
                boolean completed = cursor.getInt(3) == 1;

                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.HORIZONTAL);
                item.setGravity(android.view.Gravity.CENTER_VERTICAL);
                item.setPadding(dp(12), dp(10), dp(12), dp(10));
                item.setBackgroundColor(Color.rgb(32, 35, 41));

                LinearLayout textContainer = new LinearLayout(this);
                textContainer.setOrientation(LinearLayout.VERTICAL);

                TextView name = new TextView(this);
                name.setText(tierName);
                name.setTextColor(Color.WHITE);
                name.setTextSize(16);
                name.setTypeface(null, 1);

                TextView reward = new TextView(this);
                reward.setText(tierPoints + " XP");
                reward.setTextColor(Color.rgb(85, 221, 160));
                reward.setTextSize(13);

                textContainer.addView(name);
                textContainer.addView(reward);

                LinearLayout.LayoutParams textParams =
                    new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                    );

                item.addView(textContainer, textParams);

                CheckBox checkbox = new CheckBox(this);
                checkbox.setButtonTintList(
                    android.content.res.ColorStateList.valueOf(
                        Color.rgb(85, 221, 160)
                    )
                );
                checkbox.setChecked(completed);
                checkbox.setText("Done");
                checkbox.setTextColor(Color.WHITE);

                item.addView(checkbox);

                checkbox.setOnCheckedChangeListener(
                    (button, checked) -> {

                        database.setTierCompleted(tierId, checked);
                        refreshUI();
                    }
                );

                LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    );

                params.bottomMargin = dp(8);

                tierContainer.addView(item, params);
            }
        }
    }

    private void showAddTierDialog() {

        EditText nameInput = new EditText(this);
        nameInput.setHint("Tier name");
        nameInput.setSingleLine(true);

        EditText pointsInput = new EditText(this);
        pointsInput.setHint("Points reward");
        pointsInput.setInputType(
            InputType.TYPE_CLASS_NUMBER
        );

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), dp(8));

        layout.addView(nameInput);
        layout.addView(pointsInput);

        new AlertDialog.Builder(this)
            .setTitle("Create Tier")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create", (dialog, which) -> {

                String name = nameInput.getText()
                    .toString().trim();

                String value = pointsInput.getText()
                    .toString().trim();

                if (name.isEmpty() || value.isEmpty()) {
                    Toast.makeText(this,
                        "Fill in both fields!",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                int reward;

                try {
                    reward = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    Toast.makeText(this,
                        "Invalid points value!",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                if (reward < 0) {
                    Toast.makeText(this,
                        "Points cannot be negative!",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                long result = database.addTier(
                    achievementId,
                    name,
                    reward
                );

                if (result != -1) {
                    Toast.makeText(this,
                        "Tier created!",
                        Toast.LENGTH_SHORT).show();

                    refreshUI();
                } else {
                    Toast.makeText(this,
                        "Could not create tier.",
                        Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
    }
}
