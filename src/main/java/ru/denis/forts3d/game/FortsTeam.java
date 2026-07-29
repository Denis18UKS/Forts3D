package ru.denis.forts3d.game;
public enum FortsTeam {
    RED("red", "§c"), BLUE("blue", "§9"), SPECTATOR("spectator", "§7");
    private final String id, color;
    FortsTeam(String id, String color){this.id=id;this.color=color;}
    public String id(){return id;} public String color(){return color;}
    public static FortsTeam byId(String id){for(var t:values())if(t.id.equalsIgnoreCase(id))return t;return SPECTATOR;}
}
