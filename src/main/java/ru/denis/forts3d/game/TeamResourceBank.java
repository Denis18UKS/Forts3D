package ru.denis.forts3d.game;

import java.util.EnumSet;
import java.util.Set;

/** Server-authoritative resource wallet shared by one team. */
public final class TeamResourceBank {
    private long metal;
    private long energy;
    private long energyCapacity;
    private long ammo;
    private long ammoCapacity;
    private final EnumSet<Technology> technologies = EnumSet.noneOf(Technology.class);

    public TeamResourceBank(long metal, long energy, long capacity) {
        this(metal, energy, capacity, 250, 5000);
    }

    public TeamResourceBank(
        long metal,
        long energy,
        long energyCapacity,
        long ammo,
        long ammoCapacity
    ) {
        this.metal = Math.max(0, metal);
        this.energyCapacity = Math.max(0, energyCapacity);
        this.energy = clamp(energy, 0, this.energyCapacity);
        this.ammoCapacity = Math.max(0, ammoCapacity);
        this.ammo = clamp(ammo, 0, this.ammoCapacity);
    }

    public synchronized long metal() {
        return metal;
    }

    public synchronized long energy() {
        return energy;
    }

    public synchronized long capacity() {
        return energyCapacity;
    }

    public synchronized long ammo() {
        return ammo;
    }

    public synchronized long ammoCapacity() {
        return ammoCapacity;
    }

    public synchronized Set<Technology> technologies() {
        return Set.copyOf(technologies);
    }

    public synchronized void addMetal(long value) {
        metal = Math.max(0, saturatingAdd(metal, value));
    }

    public synchronized void addEnergy(long value) {
        energy = clamp(saturatingAdd(energy, value), 0, energyCapacity);
    }

    public synchronized void addAmmo(long value) {
        ammo = clamp(saturatingAdd(ammo, value), 0, ammoCapacity);
    }

    public synchronized void addCapacity(long value) {
        energyCapacity = Math.max(0, saturatingAdd(energyCapacity, value));
        energy = Math.min(energy, energyCapacity);
    }

    public synchronized void addAmmoCapacity(long value) {
        ammoCapacity = Math.max(0, saturatingAdd(ammoCapacity, value));
        ammo = Math.min(ammo, ammoCapacity);
    }

    public synchronized boolean spend(long metalCost, long energyCost) {
        return spend(metalCost, energyCost, 0);
    }

    public synchronized boolean spendAmmo(long value) {
        return spend(0, 0, value);
    }

    /** Atomically pays all costs so partial payments can never occur. */
    public synchronized boolean spend(long metalCost, long energyCost, long ammoCost) {
        if (metalCost < 0 || energyCost < 0 || ammoCost < 0) {
            throw new IllegalArgumentException("Resource costs cannot be negative");
        }
        if (metal < metalCost || energy < energyCost || ammo < ammoCost) {
            return false;
        }
        metal -= metalCost;
        energy -= energyCost;
        ammo -= ammoCost;
        return true;
    }

    public synchronized boolean canUnlock(Technology technology) {
        Technology prerequisite = technology.prerequisite();
        return !technologies.contains(technology)
            && (prerequisite == null || technologies.contains(prerequisite))
            && metal >= technology.metal()
            && energy >= technology.energy();
    }

    public synchronized boolean unlock(Technology technology) {
        if (!canUnlock(technology)
            || !spend(technology.metal(), technology.energy())) {
            return false;
        }
        technologies.add(technology);
        return true;
    }

    public synchronized boolean has(Technology technology) {
        return technologies.contains(technology);
    }

    private static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0 && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
    }
}
