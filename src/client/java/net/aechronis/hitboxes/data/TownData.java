package net.aechronis.hitboxes.data;

/** One entry from towns.json's "towns" map. Only the fields this mod needs. */
public class TownData {
    private final String name;
    private final int[] territories;

    /**
     * Resolved by AechronisDataFetcher after all data sets are loaded, on a background fetch
     * thread; read from the render thread via RelationResolver/ColorUtil - volatile so a write
     * here is guaranteed visible without needing another synchronization point in between.
     */
    volatile NationData nation;

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
