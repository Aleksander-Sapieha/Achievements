package com.sourceguy.achievements;


import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper database;

    private TextView totalPoints;
    private LinearLayout categoryContainer;
    private LinearLayout achievementContainer;

    private List<Category> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(R.layout.activity_main);
        android.widget.ScrollView scrollView =
            findViewById(R.id.mainScrollView);

        scrollView.post(() -> scrollView.scrollTo(0, 0));

        database = new DatabaseHelper(this);

        totalPoints = findViewById(R.id.totalPoints);
        categoryContainer = findViewById(R.id.categoryContainer);
        achievementContainer = findViewById(R.id.achievementContainer);

        Button addCategory = findViewById(R.id.addCategoryButton);
        Button addAchievement = findViewById(R.id.addAchievementButton);

        addCategory.setOnClickListener(v -> showCategoryDialog());
        addAchievement.setOnClickListener(v -> showAchievementDialog());

        refreshUI();
    }

    private int dp(int value) {
        return (int) (value * getResources()
            .getDisplayMetrics().density);
    }

    private TextView createText(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        return view;
    }

    private void showCategoryDialog() {

        EditText input = new EditText(this);
        input.setHint("Category name");
        input.setSingleLine(true);

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(dp(20), dp(8), dp(20), dp(8));
        layout.addView(input);

        new AlertDialog.Builder(this)
            .setTitle("Create Category")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create", (dialog, which) -> {

                String name = input.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(this,
                        "Enter a category name!",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                long result = database.addCategory(name);

                if (result != -1) {
                    Toast.makeText(this,
                        "Category created!",
                        Toast.LENGTH_SHORT).show();
                    refreshUI();
                } else {
                    Toast.makeText(this,
                        "Could not create category.",
                        Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private void showAchievementDialog() {

        categories = database.getCategories();

        if (categories.isEmpty()) {
            Toast.makeText(this,
                "Create a category first!",
                Toast.LENGTH_SHORT).show();
            return;
        }

        EditText nameInput = new EditText(this);
        nameInput.setHint("Achievement name");
        nameInput.setSingleLine(true);

        EditText descriptionInput = new EditText(this);
        descriptionInput.setHint("Description");

        Spinner categorySpinner = new Spinner(this);

        List<String> categoryNames = new ArrayList<>();

        for (Category category : categories) {
            categoryNames.add(category.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            categoryNames
        );

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );

        categorySpinner.setAdapter(adapter);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), dp(8));

        layout.addView(nameInput);
        layout.addView(descriptionInput);

        TextView label = createText("Category", 14);
        label.setTextColor(Color.DKGRAY);

        layout.addView(label);
        layout.addView(categorySpinner);

        new AlertDialog.Builder(this)
            .setTitle("Create Achievement")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create", (dialog, which) -> {

                String name = nameInput.getText().toString().trim();

                String description =
                    descriptionInput.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(this,
                        "Enter an achievement name!",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                int position = categorySpinner.getSelectedItemPosition();

                if (position < 0 || position >= categories.size()) {
                    return;
                }

                int categoryId = categories.get(position).getId();

                long result = database.addAchievement(
                    name,
                    description,
                    categoryId
                );

                if (result != -1) {
                    Toast.makeText(this,
                        "Achievement created!",
                        Toast.LENGTH_SHORT).show();
                    refreshUI();
                } else {
                    Toast.makeText(this,
                        "Could not create achievement.",
                        Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private void refreshUI() {

        totalPoints.setText(database.getTotalPoints() + " XP");

        refreshCategories();
        refreshAchievements();
    }

    private void refreshCategories() {

        categoryContainer.removeAllViews();

        categories = database.getCategories();

        if (categories.isEmpty()) {
            categoryContainer.addView(
                createText("No categories yet.", 14)
            );
            return;
        }

        for (Category category : categories) {

            TextView item = createText(
                "📁  " + category.getName() +
                "  •  " +
                database.getCategoryPoints(category.getId()) + " XP",
                16
            );

            item.setPadding(dp(12), dp(14), dp(12), dp(14));
            item.setBackgroundColor(Color.rgb(32, 35, 41));

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );

            params.bottomMargin = dp(8);
            item.setOnLongClickListener(v -> {
                confirmDeleteCategory(category);
                return true;
            });
            categoryContainer.addView(item, params);
        }
    }

    
    private void refreshAchievements() {

        achievementContainer.removeAllViews();

        SQLiteDatabase db = database.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
            "SELECT a.id, a.name, a.description, c.name " +
            "FROM achievements a " +
            "INNER JOIN categories c " +
            "ON a.category_id = c.id " +
            "ORDER BY a.id DESC",
            null
        )) {

            if (cursor.getCount() == 0) {
                achievementContainer.addView(
                    createText("No achievements yet.", 14)
                );
                return;
            }

            while (cursor.moveToNext()) {

                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String description = cursor.getString(2);
                String category = cursor.getString(3);

                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setPadding(dp(14), dp(12), dp(14), dp(12));
                item.setBackgroundColor(Color.rgb(32, 35, 41));

                TextView title = createText("🏆  " + name, 17);
                title.setTypeface(null, 1);

                TextView details = createText(
                    description == null ? "" : description, 13
                );

                TextView categoryText = createText(
                    "Category: " + category, 12
                );

                categoryText.setTextColor(
                    Color.rgb(85, 221, 160)
                );

                TextView hint = createText(
                    "Tap to view tiers →", 12
                );

                hint.setTextColor(Color.LTGRAY);

                item.addView(title);
                item.addView(details);
                item.addView(categoryText);
                item.addView(hint);

                item.setOnClickListener(v -> {
                    android.content.Intent intent =
                        new android.content.Intent(
                            this,
                            AchievementDetailsActivity.class
                        );

                    intent.putExtra("achievement_id", id);

                    startActivity(intent);
                });
                
                item.setOnLongClickListener(v -> {
                    confirmDeleteAchievement(id, name);
                    return true;
                });

                LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    );

                params.bottomMargin = dp(8);

                achievementContainer.addView(item, params);
                
            }
        }
    }
    
    
private void confirmDeleteCategory(Category category) {

    new AlertDialog.Builder(this)
        .setTitle("Delete Category")
        .setMessage(
            "Delete '" + category.getName() +
            "' and ALL achievements and tiers inside it?"
        )
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Delete", (dialog, which) -> {

            database.deleteCategory(category.getId());

            Toast.makeText(this,
                "Category deleted!",
                Toast.LENGTH_SHORT).show();

            refreshUI();
        })
        .show();
}

    private void confirmDeleteAchievement(int id, String name) {

        new AlertDialog.Builder(this)
            .setTitle("Delete Achievement")
            .setMessage(
                "Are you sure you want to delete '" +
                name + "' and all its tiers?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (dialog, which) -> {

                database.deleteAchievement(id);

                Toast.makeText(this,
                    "Achievement deleted!",
                    Toast.LENGTH_SHORT).show();

                refreshUI();
            })
            .show();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (database != null && totalPoints != null) {
            refreshUI();
        }
    }
}
