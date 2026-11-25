package com.sourceguy.achievements;

import java.util.ArrayList;
import java.util.List;

public class Achievement {
    private int id;
    private String name;
    private String description;
    private int categoryId;

    private List<Tier> tiers = new ArrayList<>();

    public Achievement(int id, String name,
                       String description, int categoryId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
    }

    public void addTier(Tier tier) {
        tiers.add(tier);
    }

    public List<Tier> getTiers() {
        return tiers;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public int getPointsEarned() {
        int total = 0;

        for (Tier tier : tiers) {
            if (tier.isCompleted()) {
                total += tier.getPoints();
            }
        }

        return total;
    }
}
