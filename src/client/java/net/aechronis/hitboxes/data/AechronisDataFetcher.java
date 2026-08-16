package net.aechronis.hitboxes.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.aechronis.hitboxes.AechronisHitboxes;
import net.aechronis.hitboxes.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fetches and caches Aechronis's public Nodes map data (https://map.aechronis.net/nodes/*.json)
 * and resolves town/nation/ally/enemy relationships from it.
 * <p>
 * Structure follows CrusalisUtils' DynmapFetcher/DynampUtils (GPLv3), rewritten against
 * Aechronis's Nodes JSON schema instead of Crusalis's Dynmap/Towny schema, and only pulling
 * towns.json + world.json (ports.json/war.json/buildings.json aren't needed for hitbox coloring).
 */
public class AechronisDataFetcher {

    private static final String USER_AGENT = "AechronisHitboxes/1.0";

    /** towns.json changes often (residents move/join towns) - poll it fairly frequently. */
    private static final long TOWNS_POLL_INTERVAL_SECONDS = 120; // 2 minutes

    /** world.json is large (~16MB) and land claims change rarely - poll it less often. */
    private static final long WORLD_POLL_INTERVAL_SECONDS = 600; // 10 minutes

    private static volatile Map<String, ResidentData> residentsByUuid = Map.of();
    private static volatile Map<String, TownData> townsByName = Map.of();
    private static volatile Map<String, NationData> nationsByName = Map.of();
    private static volatile Map<Integer, TerritoryData> territoriesById = Map.of();
    /** chunk (packed x/z) -> territory, rebuilt whenever world.json refreshes. */
    private static volatile Map<Long, TerritoryData> territoryByChunk = Map.of();

    private static String clientUuid = null;

    private static ScheduledExecutorService scheduler;
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) {
            refreshTowns();
            refreshWorld();
            return;
        }
        initialized = true;

        refreshTowns();
        refreshWorld();

        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "aechronis-hitboxes-data-fetcher");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(AechronisDataFetcher::refreshTowns,
                TOWNS_POLL_INTERVAL_SECONDS, TOWNS_POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(AechronisDataFetcher::refreshWorld,
                WORLD_POLL_INTERVAL_SECONDS, WORLD_POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public static void setClientUuid(String uuid) {
        clientUuid = uuid;
    }

    private static String mapLink() {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        return config.mapLink;
    }

    public static void refreshTowns() {
        fetchJson(mapLink() + "/nodes/towns.json").thenAccept(root -> {
            if (root == null) return;
            try {
                Map<String, ResidentData> residents = parseResidents(root);
                Map<String, TownData> towns = parseTowns(root);
                Map<String, NationData> nations = parseNations(root);

                linkTownsToNations(towns, nations);
                linkTerritoriesToTowns(territoriesById, towns);

                residentsByUuid = residents;
                townsByName = towns;
                nationsByName = nations;
            } catch (Exception e) {
                AechronisHitboxes.LOGGER.error("Failed to parse towns.json", e);
            }
        });
    }

    public static void refreshWorld() {
        fetchJson(mapLink() + "/nodes/world.json").thenAccept(root -> {
            if (root == null) return;
            try {
                Map<Integer, TerritoryData> territories = parseTerritories(root);
                linkTerritoriesToTowns(territories, townsByName);

                territoriesById = territories;
                territoryByChunk = buildChunkIndex(territories);
            } catch (Exception e) {
                AechronisHitboxes.LOGGER.error("Failed to parse world.json", e);
            }
        });
    }

    private static CompletableFuture<JsonObject> fetchJson(String link) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(link).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(30_000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    AechronisHitboxes.LOGGER.warn("Aechronis map fetch failed ({}): {}", responseCode, link);
                    return null;
                }

                StringBuilder responseBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                }

                return JsonParser.parseString(responseBuilder.toString()).getAsJsonObject();
            } catch (Exception e) {
                AechronisHitboxes.LOGGER.warn("Aechronis map fetch error for {}: {}", link, e.toString());
                return null;
            }
        });
    }

    private static Map<String, ResidentData> parseResidents(JsonObject root) {
        Map<String, ResidentData> residents = new HashMap<>();
        if (!root.has("residents")) return residents;

        JsonObject residentsJson = root.getAsJsonObject("residents");
        for (Map.Entry<String, JsonElement> entry : residentsJson.entrySet()) {
            String uuid = entry.getKey();
            JsonObject obj = entry.getValue().getAsJsonObject();
            String name = obj.has("name") ? obj.get("name").getAsString() : null;
            String town = nullableString(obj, "town");
            String nation = nullableString(obj, "nation");
            residents.put(uuid, new ResidentData(uuid, name, town, nation));
        }
        return residents;
    }

    private static Map<String, TownData> parseTowns(JsonObject root) {
        Map<String, TownData> towns = new HashMap<>();
        if (!root.has("towns")) return towns;

        JsonObject townsJson = root.getAsJsonObject("towns");
        for (Map.Entry<String, JsonElement> entry : townsJson.entrySet()) {
            String townName = entry.getKey();
            JsonObject obj = entry.getValue().getAsJsonObject();

            int[] territories = new int[0];
            if (obj.has("territories") && obj.get("territories").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("territories");
                territories = new int[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    territories[i] = arr.get(i).getAsInt();
                }
            }

            towns.put(townName, new TownData(townName, territories));
        }
        return towns;
    }

    private static Map<String, NationData> parseNations(JsonObject root) {
        Map<String, NationData> nations = new HashMap<>();
        if (!root.has("nations")) return nations;

        JsonObject nationsJson = root.getAsJsonObject("nations");
        for (Map.Entry<String, JsonElement> entry : nationsJson.entrySet()) {
            String nationName = entry.getKey();
            JsonObject obj = entry.getValue().getAsJsonObject();

            nations.put(nationName, new NationData(
                    nationName,
                    stringList(obj, "towns"),
                    stringList(obj, "allies"),
                    stringList(obj, "enemies")
            ));
        }
        return nations;
    }

    private static Map<Integer, TerritoryData> parseTerritories(JsonObject root) {
        Map<Integer, TerritoryData> territories = new HashMap<>();
        if (!root.has("territories")) return territories;

        JsonObject territoriesJson = root.getAsJsonObject("territories");
        for (Map.Entry<String, JsonElement> entry : territoriesJson.entrySet()) {
            JsonObject obj = entry.getValue().getAsJsonObject();
            if (!obj.has("chunks") || !obj.has("core")) continue;

            int id;
            try {
                id = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }

            int[] chunks = intArray(obj.getAsJsonArray("chunks"));
            int[] core = intArray(obj.getAsJsonArray("core"));

            territories.put(id, new TerritoryData(id, core, chunks));
        }
        return territories;
    }

    private static void linkTownsToNations(Map<String, TownData> towns, Map<String, NationData> nations) {
        for (NationData nation : nations.values()) {
            for (String townName : nation.getTowns()) {
                TownData town = towns.get(townName);
                if (town != null) {
                    town.nation = nation;
                }
            }
        }
    }

    private static void linkTerritoriesToTowns(Map<Integer, TerritoryData> territories, Map<String, TownData> towns) {
        if (territories.isEmpty() || towns.isEmpty()) return;
        for (TownData town : towns.values()) {
            for (int territoryId : town.getTerritories()) {
                TerritoryData territory = territories.get(territoryId);
                if (territory != null) {
                    territory.town = town;
                    territory.nation = town.getNation();
                }
            }
        }
    }

    private static Map<Long, TerritoryData> buildChunkIndex(Map<Integer, TerritoryData> territories) {
        Map<Long, TerritoryData> index = new HashMap<>();
        for (TerritoryData territory : territories.values()) {
            int[] chunks = territory.getChunks();
            for (int i = 0; i + 1 < chunks.length; i += 2) {
                index.put(packChunk(chunks[i], chunks[i + 1]), territory);
            }
        }
        return index;
    }

    static long packChunk(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static String nullableString(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : null;
    }

    private static List<String> stringList(JsonObject obj, String key) {
        List<String> list = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray(key)) {
                list.add(el.getAsString());
            }
        }
        return list;
    }

    private static int[] intArray(JsonArray arr) {
        int[] out = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            out[i] = arr.get(i).getAsInt();
        }
        return out;
    }

    // --------------------------
    // Accessors used by RelationResolver
    // --------------------------

    static ResidentData getClientResident() {
        if (clientUuid == null) return null;
        return residentsByUuid.get(clientUuid);
    }

    static ResidentData getResidentByUuid(String uuid) {
        return residentsByUuid.get(uuid);
    }

    static ResidentData getResidentByName(String name) {
        for (ResidentData resident : residentsByUuid.values()) {
            if (resident.getName() != null && resident.getName().equalsIgnoreCase(name)) {
                return resident;
            }
        }
        return null;
    }

    static NationData getNation(String name) {
        return name == null ? null : nationsByName.get(name);
    }

    static TerritoryData getTerritoryAtChunk(int chunkX, int chunkZ) {
        return territoryByChunk.get(packChunk(chunkX, chunkZ));
    }
}
