package net.aechronis.hitboxes.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.shedaniel.autoconfig.AutoConfig;
import net.aechronis.hitboxes.config.ModConfig;
import net.aechronis.hitboxes.data.NodeRelation;
import net.aechronis.hitboxes.data.RelationResolver;
import net.aechronis.hitboxes.data.TerritoryData;
import net.aechronis.hitboxes.hitbox.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.core.SectionPos;
import net.minecraft.util.ARGB;
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

    // MAJOR_LINES is the color emitGizmos actually uses for most of the grid (six call sites,
    // vs three each for CELL_BORDER/YELLOW above) - without this redirect the vast majority of
    // the F3+G grid stayed vanilla-colored. Default matches vanilla's own
    // ARGB.colorFromFloat(1F, 0.25F, 0.25F, 1F).
    @Redirect(
            method = "emitGizmos(DDDLnet/minecraft/util/debug/DebugValueAccess;Lnet/minecraft/client/renderer/culling/Frustum;F)V",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/client/renderer/debug/ChunkBorderRenderer;MAJOR_LINES:I"
            )
    )
    private int aechronisHitboxes$overrideMajorLines() {
        return aechronisHitboxes$computeColor(0xFF3F3FFF);
    }

    // The corner-pillar lines drawn around (not just at) the player's chunk aren't sourced from
    // any of the three static fields above - they're computed inline via a single
    // ARGB.colorFromFloat(0.5F, 1F, 0F, 0F) call per pillar, with no field to @Redirect a
    // GETSTATIC against. Redirecting that call instead, and - unlike the other three redirects,
    // which reuse the player's own current chunk - resolving each pillar's own chunk from its
    // local grid offset, since these pillars span several chunks around the player, not just one.
    // Capturing the method's single SectionPos local (unambiguous, no ordinal needed) rather than
    // the two double locals it derives its X/Z from directly - ordinal-based @Local capture of
    // consecutive doubles (each consuming two local-variable slots) landed on garbage data for
    // the second one in practice, even though the bytecode's slot layout looked correct.
    @Redirect(
            method = "emitGizmos(DDDLnet/minecraft/util/debug/DebugValueAccess;Lnet/minecraft/client/renderer/culling/Frustum;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/ARGB;colorFromFloat(FFFF)I"
            )
    )
    private int aechronisHitboxes$overrideCornerPillar(
            float alpha, float red, float green, float blue,
            @Local SectionPos origin,
            @Local(ordinal = 0) int offsetX,
            @Local(ordinal = 1) int offsetZ
    ) {
        int defaultColor = ARGB.colorFromFloat(alpha, red, green, blue);

        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (!config.autoChunkBorders) {
            return defaultColor;
        }

        int chunkX = Math.floorDiv(origin.minBlockX() + offsetX, 16);
        int chunkZ = Math.floorDiv(origin.minBlockZ() + offsetZ, 16);
        TerritoryData territory = RelationResolver.getTerritoryAtChunk(chunkX, chunkZ);

        if (territory == null) {
            return defaultColor;
        }

        NodeRelation relation = RelationResolver.getRelationToTerritory(territory);
        int hexColour = ColorUtil.getHexFromRelation(relation, defaultColor);

        return (0xFF << 24) | (hexColour & 0xFFFFFF);
    }
}
