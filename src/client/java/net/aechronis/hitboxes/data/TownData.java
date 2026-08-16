package net.aechronis.hitboxes.data;

/** One entry from towns.json's "towns" map. Only the fields this mod needs. */
public class TownData {
    private final String name;
    private final int[] territories;

    /** Resolved by AechronisDataFetcher after all data sets are loaded. */
    NationData nation;

    public TownData(String name, int[] territories) {
        this.name = name;
        this.territories = territories;
    }

    public String getName() {
        return name;
    }

    public int[] getTerritories() {
        return territories;
    }

    public NationData getNation() {
        return nation;
    }
}
