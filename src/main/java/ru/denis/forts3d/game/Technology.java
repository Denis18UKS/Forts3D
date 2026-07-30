package ru.denis.forts3d.game;

/** Ordered Forts research tree. */
public enum Technology {
    WORKSHOP("Мастерская", 100, 150, null),
    FACTORY("Завод", 250, 300, WORKSHOP),
    MUNITIONS("Военная промышленность", 200, 250, WORKSHOP),
    ROCKETRY("Ракетная техника", 500, 700, MUNITIONS),
    LASERS("Лазерное оружие", 800, 1200, FACTORY),
    SHIELDS("Силовые щиты", 1000, 1500, LASERS),
    RADAR("Радиолокация", 300, 450, WORKSHOP),
    DRONES("Боевые дроны", 700, 900, RADAR),
    ADVANCED_ARMOR("Композитная броня", 600, 700, FACTORY);

    private final String title;
    private final int metal;
    private final int energy;
    private final Technology prerequisite;

    Technology(String title, int metal, int energy, Technology prerequisite) {
        this.title = title;
        this.metal = metal;
        this.energy = energy;
        this.prerequisite = prerequisite;
    }

    public String title() {
        return title;
    }

    public int metal() {
        return metal;
    }

    public int energy() {
        return energy;
    }

    public Technology prerequisite() {
        return prerequisite;
    }
}
