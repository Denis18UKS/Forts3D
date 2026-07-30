package ru.denis.forts3d.game;

import java.util.List;

public enum FortsTeam {
    RED("red", "§c"),
    BLUE("blue", "§9"),
    SPECTATOR("spectator", "§7");

    private static final List<FortsTeam> PLAYING_TEAMS = List.of(RED, BLUE);

    private final String id;
    private final String color;

    FortsTeam(String id, String color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public String color() {
        return color;
    }

    public FortsTeam opponent() {
        return this == RED ? BLUE : this == BLUE ? RED : SPECTATOR;
    }

    public static List<FortsTeam> playingTeams() {
        return PLAYING_TEAMS;
    }

    public static FortsTeam byId(String id) {
        for (FortsTeam team : values()) {
            if (team.id.equalsIgnoreCase(id)) {
                return team;
            }
        }
        return SPECTATOR;
    }
}
