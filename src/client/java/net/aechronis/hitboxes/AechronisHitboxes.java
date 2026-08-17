package net.aechronis.hitboxes;

import net.aechronis.hitboxes.config.ModConfig;
import net.aechronis.hitboxes.data.AechronisDataFetcher;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AechronisHitboxes implements ClientModInitializer {

    public static final String MOD_ID = "aechronishitboxes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Servers this build is allowed to activate on, and each one's Nodes map data source.
     * Deliberately hardcoded rather than config-driven - pointing the mod at an arbitrary
     * server/URL is a release-time decision, not a runtime one, so adding a new server means
     * cutting a new release, not editing a config file.
     */
    private static final Map<String, String> SERVER_MAP_LINKS = Map.of(
            "aechronis.net", "https://map.aechronis.net"
    );

    public static ModConfig config;

    /**
     * Tracks whether the player was dead as of the last tick, so a death->alive transition can
     * be detected exactly once per death. Unlike upstream CrusalisUtils (whose equivalent
     * "justRespawned" latch was never reset, so auto /t spawn only ever fired once per session),
     * this resets on every death so it fires on every respawn.
     */
    private static boolean wasDead = false;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var serverData = client.getCurrentServer();
            String serverAddress = serverData != null ? serverData.ip : null;
            String mapLink = serverAddress == null ? null : resolveMapLink(serverAddress);
            if (mapLink == null) {
                LOGGER.info("Not connected to an allowed Aechronis server (address={}), mod inactive.", serverAddress);
                return;
            }

            LOGGER.info("Connected to Aechronis server (address={}), mod active.", serverAddress);
            if (client.player != null) {
                AechronisDataFetcher.setClientUuid(client.player.getStringUUID());
            }
            AechronisDataFetcher.initialize(mapLink);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, sender) -> wasDead = false);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !config.autoTSpawn) {
                return;
            }

            if (client.player.isDeadOrDying()) {
                wasDead = true;
            } else if (wasDead) {
                wasDead = false;
                if (client.getConnection() != null) {
                    client.getConnection().sendCommand("t spawn");
                }
            }
        });
    }

    /** Map link for the first hardcoded host substring found in the server address, or null. */
    private static String resolveMapLink(String serverAddress) {
        String lowerAddress = serverAddress.toLowerCase();
        for (Map.Entry<String, String> entry : SERVER_MAP_LINKS.entrySet()) {
            if (lowerAddress.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
