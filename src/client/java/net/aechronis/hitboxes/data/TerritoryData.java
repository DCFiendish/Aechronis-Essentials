package net.aechronis.hitboxes.data;

/** One entry from world.json's "territories" map. Only the fields this mod needs. */
public class TerritoryData {
    private final int id;
    private final int[] core;
    /** Flattened chunk-coordinate pairs: chunks[0]/chunks[1] is the first chunk, etc. */
    private final int[] chunks;

    /** Resolved by AechronisDataFetcher after all data sets are loaded. */
    TownData town;
    NationData nation;
    NodeRelation relationToClient = NodeRelation.NEUTRAL;

    public TerritoryData(int id, int[] core, int[] chunks) {
        this.id = id;
        this.core = core;
        this.chunks = chunks;
    }

    public int getId() {
        return id;
    }

    public int[] getCore() {
        return core;
    }

    public int[] getChunks() {
        return chunks;
    }

    public TownData getTown() {
        return town;
    }

    public NationData getNation() {
        return nation;
    }

    public NodeRelation getRelationToClient() {
        return relationToClient;
    }
}
