package ru.denis.forts3d.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FortsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue STRUCTURAL_COLLAPSE, WORLD_DAMAGE, FRIENDLY_FIRE, FIRE_SPREAD,
        REACTOR_MELTDOWN, REQUIRE_TECH, LIMITED_BUILD_ZONE, WEATHER_BALLISTICS, DRONE_RAIDS;
    public static final ModConfigSpec.IntValue MAX_PROJECTILES, STRUCTURE_TICK_RATE, MATCH_TIME_SECONDS, START_METAL,
        START_ENERGY, RESOURCE_TICK_RATE, REACTOR_HEALTH, BUILD_RADIUS, MAX_TEAM_SIZE;
    public static final ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER, RESOURCE_MULTIPLIER, WIND_MULTIPLIER;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("simulation");
        STRUCTURAL_COLLAPSE = b.define("structuralCollapse", true);
        STRUCTURE_TICK_RATE = b.defineInRange("structureTickRate", 10, 1, 200);
        MAX_PROJECTILES = b.defineInRange("maxActiveProjectiles", 512, 16, 8192);
        WORLD_DAMAGE = b.define("worldDamage", true);
        FIRE_SPREAD = b.define("fireSpread", true);
        REACTOR_MELTDOWN = b.define("reactorMeltdown", true);
        WEATHER_BALLISTICS = b.define("weatherAffectsBallistics", true);
        WIND_MULTIPLIER = b.defineInRange("windMultiplier", 1.0, 0.0, 5.0);
        DAMAGE_MULTIPLIER = b.defineInRange("damageMultiplier", 1.0, 0.0, 10.0);
        b.pop().push("match");
        FRIENDLY_FIRE = b.define("friendlyFire", false);
        MATCH_TIME_SECONDS = b.defineInRange("matchTimeSeconds", 1800, 60, 86400);
        START_METAL = b.defineInRange("startingMetal", 500, 0, 1_000_000);
        START_ENERGY = b.defineInRange("startingEnergy", 1000, 0, 1_000_000);
        RESOURCE_TICK_RATE = b.defineInRange("resourceTickRate", 20, 1, 1200);
        RESOURCE_MULTIPLIER = b.defineInRange("resourceMultiplier", 1.0, 0.0, 100.0);
        REACTOR_HEALTH = b.defineInRange("reactorHealth", 10000, 100, 10_000_000);
        LIMITED_BUILD_ZONE = b.define("limitedBuildZone", true);
        BUILD_RADIUS = b.defineInRange("buildRadius", 96, 16, 1024);
        MAX_TEAM_SIZE = b.defineInRange("maxTeamSize", 16, 1, 128);
        REQUIRE_TECH = b.define("requireTechnology", true);
        DRONE_RAIDS = b.define("droneRaids", true);
        b.pop(); SPEC = b.build();
    }
    private FortsConfig() {}
}
