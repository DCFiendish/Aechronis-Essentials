package net.aechronis.hitboxes.data;

/**
 * Resolves TOWN/NATION/ALLIED/ENEMIES/NEUTRAL relations from the data cached by
 * AechronisDataFetcher. Mirrors CrusalisUtils' DynampUtils relation logic (GPLv3), adapted to
 * Aechronis's UUID-keyed resident model and nation-level allies/enemies (name strings).
 * <p>
 * Ally requires mutual reciprocity (both nations list each other); enemy only needs one side
 * to declare it - this matches the semantics of Aechronis's actual Nodes plugin (Nation.kt).
 */
public final class RelationResolver {
    private RelationResolver() {
    }

    public static NodeRelation getRelationToClient(String targetUuid) {
        ResidentData client = AechronisDataFetcher.getClientResident();
        ResidentData target = AechronisDataFetcher.getResidentByUuid(targetUuid);
        return resolve(client, target);
    }

    public static NodeRelation getRelationToClientByName(String targetName) {
        ResidentData client = AechronisDataFetcher.getClientResident();
        ResidentData target = AechronisDataFetcher.getResidentByName(targetName);
        return resolve(client, target);
    }

    private static NodeRelation resolve(ResidentData client, ResidentData target) {
        if (client == null || target == null) return NodeRelation.NEUTRAL;

        String clientNationName = effectiveNationName(client);
        String targetNationName = effectiveNationName(target);
        if (clientNationName == null || targetNationName == null) return NodeRelation.NEUTRAL;

        if (clientNationName.equals(targetNationName)) {
            String clientTown = client.getTown();
            String targetTown = target.getTown();
            if (clientTown != null && clientTown.equals(targetTown)) {
                return NodeRelation.TOWN;
            }
            return NodeRelation.NATION;
        }

        NationData clientNation = AechronisDataFetcher.getNation(clientNationName);
        NationData targetNation = AechronisDataFetcher.getNation(targetNationName);
        if (clientNation == null || targetNation == null) return NodeRelation.NEUTRAL;

        boolean mutualAlly = clientNation.getAllies().contains(targetNationName)
                && targetNation.getAllies().contains(clientNationName);
        if (mutualAlly) return NodeRelation.ALLIED;

        boolean eitherEnemy = clientNation.getEnemies().contains(targetNationName)
                || targetNation.getEnemies().contains(clientNationName);
        if (eitherEnemy) return NodeRelation.ENEMIES;

        return NodeRelation.NEUTRAL;
    }

    public static NodeRelation getRelationToTerritory(TerritoryData territory) {
        if (territory == null) return NodeRelation.NEUTRAL;

        ResidentData client = AechronisDataFetcher.getClientResident();
        NationData territoryNation = territory.getNation();
        if (client == null || territoryNation == null) return NodeRelation.NEUTRAL;

        String clientNationName = effectiveNationName(client);
        if (clientNationName == null) return NodeRelation.NEUTRAL;

        NodeRelation relation;
        if (clientNationName.equals(territoryNation.getName())) {
            TownData territoryTown = territory.getTown();
            String clientTown = client.getTown();
            relation = (clientTown != null && territoryTown != null && clientTown.equals(territoryTown.getName()))
                    ? NodeRelation.TOWN
                    : NodeRelation.NATION;
        } else {
            NationData clientNation = AechronisDataFetcher.getNation(clientNationName);
            if (clientNation == null) {
                relation = NodeRelation.NEUTRAL;
            } else if (clientNation.getAllies().contains(territoryNation.getName())
                    && territoryNation.getAllies().contains(clientNationName)) {
                relation = NodeRelation.ALLIED;
            } else if (clientNation.getEnemies().contains(territoryNation.getName())
                    || territoryNation.getEnemies().contains(clientNationName)) {
                relation = NodeRelation.ENEMIES;
            } else {
                relation = NodeRelation.NEUTRAL;
            }
        }

        territory.relationToClient = relation;
        return relation;
    }

    public static TerritoryData getTerritoryAtChunk(int chunkX, int chunkZ) {
        return AechronisDataFetcher.getTerritoryAtChunk(chunkX, chunkZ);
    }

    /**
     * A resident's own "nation" field can be stale/unset even when their town is actually in a
     * nation (observed against a Nodes fork where residents.nation didn't get backfilled after
     * their town joined a nation) - fall back to the resident's town's nation link, which is
     * derived straight from the authoritative nations->towns list.
     */
    private static String effectiveNationName(ResidentData resident) {
        String nation = resident.getNation();
        if (nation != null) return nation;

        TownData town = AechronisDataFetcher.getTown(resident.getTown());
        NationData townNation = town != null ? town.getNation() : null;
        return townNation != null ? townNation.getName() : null;
    }
}
