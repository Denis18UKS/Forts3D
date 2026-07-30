package ru.denis.forts3d.item;

import ru.denis.forts3d.game.Technology;

/** Tuning data for each Forts weapon family. */
public enum WeaponKind {
    MACHINE_GUN(
        "Пулемёт", 1, 5, 18, 10, 0, 1.9F, 0.0, 3, Technology.WORKSHOP, 1, 0.01
    ),
    CANNON(
        "Пушка", 6, 40, 150, 220, 3, 1.25F, 0.002, 30, Technology.FACTORY, 1, 0.0
    ),
    MORTAR(
        "Миномёт", 4, 25, 100, 130, 2, 0.78F, 0.018, 24, Technology.MUNITIONS, 1, 0.0
    ),
    ROCKET_LAUNCHER(
        "Ракетная установка", 10, 80, 260, 420, 5, 1.05F, 0.0, 55, Technology.ROCKETRY, 1, 0.0
    ),
    LASER(
        "Лазер", 0, 180, 320, 500, 1, 2.8F, 0.0, 70, Technology.LASERS, 1, 0.0
    ),
    FLAK(
        "Зенитная пушка", 3, 30, 55, 35, 1, 1.65F, 0.006, 22, Technology.MUNITIONS, 5, 0.08
    );

    private final String title;
    private final int ammoCost;
    private final int energyCost;
    private final int structureDamage;
    private final int reactorDamage;
    private final int explosionPower;
    private final float velocity;
    private final double gravity;
    private final int cooldownTicks;
    private final Technology technology;
    private final int projectiles;
    private final double spread;

    WeaponKind(
        String title,
        int ammoCost,
        int energyCost,
        int structureDamage,
        int reactorDamage,
        int explosionPower,
        float velocity,
        double gravity,
        int cooldownTicks,
        Technology technology,
        int projectiles,
        double spread
    ) {
        this.title = title;
        this.ammoCost = ammoCost;
        this.energyCost = energyCost;
        this.structureDamage = structureDamage;
        this.reactorDamage = reactorDamage;
        this.explosionPower = explosionPower;
        this.velocity = velocity;
        this.gravity = gravity;
        this.cooldownTicks = cooldownTicks;
        this.technology = technology;
        this.projectiles = projectiles;
        this.spread = spread;
    }

    public String title() {
        return title;
    }

    public int ammoCost() {
        return ammoCost;
    }

    public int energyCost() {
        return energyCost;
    }

    public int structureDamage() {
        return structureDamage;
    }

    public int reactorDamage() {
        return reactorDamage;
    }

    public int explosionPower() {
        return explosionPower;
    }

    public float velocity() {
        return velocity;
    }

    public double gravity() {
        return gravity;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public Technology technology() {
        return technology;
    }

    public int projectiles() {
        return projectiles;
    }

    public double spread() {
        return spread;
    }

    public double damageRadius() {
        return Math.max(1.5, explosionPower + 1.5);
    }
}
