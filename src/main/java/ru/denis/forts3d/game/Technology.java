package ru.denis.forts3d.game;
public enum Technology {
    WORKSHOP(100,150), FACTORY(250,300), MUNITIONS(200,250), ROCKETRY(500,700), LASERS(800,1200),
    SHIELDS(1000,1500), RADAR(300,450), DRONES(700,900), ADVANCED_ARMOR(600,700);
    public final int metal, energy;
    Technology(int metal,int energy){this.metal=metal;this.energy=energy;}
}
