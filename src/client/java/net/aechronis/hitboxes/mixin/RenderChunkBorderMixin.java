package net.aechronis.hitboxes.mixin;

import me.shedaniel.autoconfig.AutoConfig;
import net.aechronis.hitboxes.config.ModConfig;
import net.aechronis.hitboxes.data.NodeRelation;
import net.aechronis.hitboxes.data.RelationResolver;
import net.aechronis.hitboxes.data.TerritoryData;
import net.aechronis.hitboxes.hitbox.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.world.level.ChunkPos;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Recolors the F3+G chunk-border debug grid by territory relation, reusing the exact same
 * relation/color pipeline as player hitboxes. Minecraft 26.2 rewrote ChunkBorderRenderer
 * (formerly ChunkBorderDebugRenderer) to draw a proximity grid via many Gizmos.line() calls
 * instead of the old two-color line renderer, but it still sources its colors from simple
 * static final int fields, so the same @Redirect-on-GETSTATIC trick CrusalisUtils used still
 * applies - just against three fields (CELL_BORDER/YELLOW/MAJOR_LINES) instead of two.
 */
@Mixin(ChunkBorderRenderer.class)
public class RenderChunkBorderMixin {

    @Unique
    private TerritoryData aechronisHitboxes$lastTerritory;
    @Unique
    private ChunkPos aechronisHitboxes$lastChunk;

    @Unique
    private int aechronisHitboxes$computeColor(int defaultColor) {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (!config.autoChunkBorders) {
            return defaultColor;
        }

        if (Minecraft.getInstance().player != null) {
            ChunkPos currentChunk = Minecraft.getInstance().player.chunkPosition();
            if (aechronisHitboxes$lastChunk == null || !aechronisHitboxes$lastChunk.equals(currentChunk)) {
                aechronisHitboxes$lastChunk = currentChunk;
                aechronisHitboxes$lastTerritory = RelationResolver.getTerritoryAtChunk(currentChunk.x(), currentChunk.z());
            }
        }

        if (aechronisHitboxes$lastTerritory == null) {
            return defaultColor;
        }

        NodeRelation relation = RelationResolver.getRelationToTerritory(aechronisHitboxes$lastTerritory);
        int hexColour = ColorUtil.getHexFromRelation(relation, defaultColor);

        return (0xFF << 24) | (hexColour & 0xFFFFFF);
    }

    @Redirect(
            method = "emitGizmos(DDDLnet/minecraft/util/debug/DebugValueAccess;Lnet/minecraft/client/renderer/culling/Frustum;F)V",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/client/renderer/debug/ChunkBorderRenderer;CELL_BORDER:I"
            )
    )
    private int aechronisHitboxes$overrideCellBorder() {
        return aechronisHitboxes$computeColor(0xFF009B9B);
    }

    @Redirect(
            method = "emitGizmos(DDDLnet/minecraft/util/debug/DebugValueAccess;Lnet/minecraft/client/renderer/culling/Frustum;F)V",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/client/renderer/debug/ChunkBorderRenderer;YELLOW:I"
            )
    )
    private int aechronisHitboxes$overrideYellow() {
        return aechronisHitboxes$computeColor(0xFFFFFF00);
    }
}
