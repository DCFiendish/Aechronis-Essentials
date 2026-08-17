package net.aechronis.hitboxes.data;

/** One entry from world.json's "territories" map. Only the fields this mod needs. */
public class TerritoryData {
    private final int id;
    /** Flattened chunk-coordinate pairs: chunks[0]/chunks[1] is the first chunk, etc. */
    private final int[] chunks;

    /**
     * Resolved/updated by AechronisDataFetcher on a background fetch thread; read (and, for
     * relationToClient, written) from the render thread - volatile so those cross-thread reads
     * and writes are guaranteed visible without needing another synchronization point in between.
     */
    volatile TownData town;
    volatile NationData nation;
    volatile NodeRelation relationToClient = NodeRelation.NEUTRAL;

    public TerritoryData(int id, int[] chunks) {
        this.id = id;
        this.chunks = chunks;
    }

    public int getId() {
        return id;
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
