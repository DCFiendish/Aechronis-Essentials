package net.aechronis.hitboxes.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Force-enables vanilla's entity-hitbox debug overlay on startup, so the recolored boxes are
 * visible without the player needing to toggle it manually. Minecraft 26.2 replaced the old
 * single boolean (EntityRenderDispatcher.setRenderHitboxes) with a persisted debug-entry
 * status system; ALWAYS_ON is the equivalent of "permanently on regardless of F3 overlay
 * visibility." Mirrors CrusalisUtils' MinecraftClientMixin (GPLv3) in intent.
 */
@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Inject(at = @At("HEAD"), method = "run")
    private void aechronisHitboxes$enableHitboxRendering(CallbackInfo ci) {
        Minecraft.getInstance().debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, DebugScreenEntryStatus.ALWAYS_ON);
    }
}
