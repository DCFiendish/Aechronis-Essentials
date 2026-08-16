package net.aechronis.hitboxes.data;

import java.util.List;

/** One entry from towns.json's "nations" map. Only the fields this mod needs. */
public class NationData {
    private final String name;
    private final List<String> towns;
    private final List<String> allies;
    private final List<String> enemies;

    public NationData(String name, List<String> towns, List<String> allies, List<String> enemies) {
        this.name = name;
        this.towns = towns;
        this.allies = allies;
        this.enemies = enemies;
    }

    public String getName() {
        return name;
    }

    public List<String> getTowns() {
        return towns;
    }

    /** Nation-name strings this nation considers an ally. */
    public List<String> getAllies() {
        return allies;
    }

    /** Nation-name strings this nation considers an enemy. */
    public List<String> getEnemies() {
        return enemies;
    }
}
