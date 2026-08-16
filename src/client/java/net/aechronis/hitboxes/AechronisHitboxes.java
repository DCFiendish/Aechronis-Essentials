package net.aechronis.hitboxes;

import net.aechronis.hitboxes.config.ModConfig;
import net.aechronis.hitboxes.data.AechronisDataFetcher;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AechronisHitboxes implements ClientModInitializer {

    public static final String MOD_ID = "aechronishitboxes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ModConfig config;

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
    }
}
