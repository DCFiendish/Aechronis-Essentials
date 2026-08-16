package net.aechronis.hitboxes.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.aechronis.hitboxes.hitbox.ColorUtil;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.awt.Color;

/**
 * Minecraft 26.2 replaced the old vertex-consumer-based debug hitbox renderer with a "Gizmos"
 * system: EntityHitboxDebugRenderer.showHitboxes() draws each entity's box via
 * Gizmos.cuboid(box, GizmoStyle.stroke(argbColor)), where the color is a single packed ARGB
 * int (alpha included) instead of the old separate RGB-record + smuggled-alpha approach
 * CrusalisUtils used on 1.21.x. This replaces that whole mixin trio (EntityHitboxMixin /
 * EntityRenderDispatcherMixin / EntityRendererMixin) with a single color override here.
 * <p>
 * Only the first GizmoStyle.stroke(int) call in showHitboxes (the main entity box) is
 * targeted - the second one (the mount/vehicle marker box) keeps vanilla's fixed yellow,
 * matching upstream's unconfigurable mount-marker color.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {

    @ModifyArg(
            method = "showHitboxes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/gizmos/GizmoStyle;stroke(I)Lnet/minecraft/gizmos/GizmoStyle;",
                    ordinal = 0
            ),
            index = 0
    )
    private int aechronisHitboxes$modifyColor(int vanillaColor, @Local Entity entity) {
        Color color = ColorUtil.getEntityColor(entity);
        return ARGB.color(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
    }
}
