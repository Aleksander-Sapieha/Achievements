package com.sourceguy.achievements;


public class Tier {
    private int id;
    private String name;
    private int points;
    private boolean completed;

    public Tier(int id, String name, int points) {
        this.id = id;
        this.name = name;
        this.points = points;
        this.completed = false;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
