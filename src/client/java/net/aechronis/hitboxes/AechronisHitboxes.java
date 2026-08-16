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

public class AechronisHitboxes implements ClientModInitializer {

    public static final String MOD_ID = "aechronishitboxes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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
            if (serverAddress == null || !serverAddress.toLowerCase().contains("aechronis.net")) {
                return;
            }

            if (client.player != null) {
                AechronisDataFetcher.setClientUuid(client.player.getStringUUID());
            }
            AechronisDataFetcher.initialize();
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
}
