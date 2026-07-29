package ru.denis.forts3d.game;

import java.util.EnumSet;
import java.util.Set;

public final class TeamResourceBank {
    private long metal, energy, energyCapacity, ammo, ammoCapacity;
    private final EnumSet<Technology> technologies = EnumSet.noneOf(Technology.class);
    public TeamResourceBank(long metal, long energy, long capacity) { this(metal,energy,capacity,250,5000); }
    public TeamResourceBank(long metal,long energy,long capacity,long ammo,long ammoCapacity){this.metal=metal;this.energy=energy;this.energyCapacity=capacity;this.ammo=ammo;this.ammoCapacity=ammoCapacity;}
    public long metal(){return metal;} public long energy(){return energy;} public long capacity(){return energyCapacity;}
    public long ammo(){return ammo;} public Set<Technology> technologies(){return Set.copyOf(technologies);}
    public void addMetal(long value){metal=Math.max(0,metal+value);} public void addEnergy(long value){energy=Math.max(0,Math.min(energyCapacity,energy+value));}
    public void addAmmo(long value){ammo=Math.max(0,Math.min(ammoCapacity,ammo+value));}
    public void addCapacity(long value){energyCapacity=Math.max(0,energyCapacity+value);energy=Math.min(energy,energyCapacity);}
    public boolean spend(long metalCost,long energyCost){if(metal<metalCost||energy<energyCost)return false;metal-=metalCost;energy-=energyCost;return true;}
    public boolean spendAmmo(long value){if(ammo<value)return false;ammo-=value;return true;}
    public boolean unlock(Technology tech){if(technologies.contains(tech)||!spend(tech.metal,tech.energy))return false;technologies.add(tech);return true;}
    public boolean has(Technology tech){return technologies.contains(tech);}
}
