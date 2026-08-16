package net.aechronis.hitboxes.hitbox;

import me.shedaniel.autoconfig.AutoConfig;
import net.aechronis.hitboxes.config.ModConfig;
import net.aechronis.hitboxes.data.NodeRelation;
import net.aechronis.hitboxes.data.RelationResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import java.awt.Color;

/**
 * Decides the color/alpha for an entity's hitbox. Priority chain and entity-type dispatch
 * follow CrusalisUtils' ColorUtil (GPLv3): self -> friend list -> enemy list -> auto
 * map-data relation -> vanilla scoreboard teammate -> neutral. The nametag-color-code
 * detection modes and manual team-registration branches from upstream are cut per this
 * port's scope (see README) - only MAP_DATA auto-detection remains.
 * <p>
 * Class/method names here are Mojang's official mappings, not Yarn's - Minecraft 26.2 ships
 * unobfuscated with no Yarn layer available, so e.g. "PlayerEntity" becomes "Player",
 * "isTeammate" becomes "isAlliedTo", etc.
 */
public final class ColorUtil {
    private ColorUtil() {
    }

    public static Color getEntityColor(Entity entity) {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        LocalPlayer clientPlayer = Minecraft.getInstance().player;

        if (entity instanceof Player && config.isPlayerConfigEnabled) {
            if (entity instanceof LocalPlayer) {
                return decode(config.self.color, config.self.alpha);
            }

            if (entity instanceof RemotePlayer otherPlayer && clientPlayer != null) {
                String username = otherPlayer.getName().getString();

                if (config.friend.list.contains(username)) {
                    return decode(config.friend.color, config.friend.alpha);
                }
                if (config.enemy.list.contains(username)) {
                    return decode(config.enemy.color, config.enemy.alpha);
                }

                if (config.autoHitboxColour) {
                    NodeRelation relation = RelationResolver.getRelationToClientByName(otherPlayer.getGameProfile().name());
                    int hex = getHexFromRelation(relation, 0xFFFFFF);
                    return decode(hex, config.friend.alpha);
                }

                if (clientPlayer.isAlliedTo(entity)) {
                    return decode(config.friend.color, config.friend.alpha);
                }

                return decode(config.neutral.color, config.neutral.alpha);
            }
        } else if (entity instanceof EnderDragon && config.enderDragon.isEnabled && config.enderDragon.boxHitbox) {
            return decode(config.enderDragon.color, config.enderDragon.alpha);
        } else if (entity instanceof Monster && config.hostile.isEnabled) {
            return decode(config.hostile.color, config.hostile.alpha);
        } else if ((entity instanceof Animal || entity instanceof Allay || entity instanceof AmbientCreature) && config.passive.isEnabled) {
            return decode(config.passive.color, config.passive.alpha);
        } else if (entity instanceof Projectile && config.projectile.isEnabled) {
            if (entity instanceof AbstractArrow arrow
                    && !config.projectile.renderStuck
                    && arrow.pickup == AbstractArrow.Pickup.DISALLOWED) {
                return transparent();
            } else if (entity instanceof ThrownEnderpearl) {
                return decode(config.misc.enderPearlEntity.color, config.misc.enderPearlEntity.alpha);
            } else if (entity instanceof ThrownTrident) {
                return decode(config.misc.tridentEntity.color, config.misc.tridentEntity.alpha);
            }
            return decode(config.projectile.color, config.projectile.alpha);
        } else if ((entity instanceof HangingEntity || entity instanceof ArmorStand) && config.decoration.isEnabled) {
            return decode(config.decoration.color, config.decoration.alpha);
        } else if ((entity instanceof AbstractMinecart || entity instanceof Boat) && config.vehicle.isEnabled) {
            return decode(config.vehicle.color, config.vehicle.alpha);
        } else if (isMiscEntity(entity) && config.misc.isEnabled) {
            if (entity instanceof AreaEffectCloud) {
                return decode(config.misc.areaEffectCloud.color, config.misc.areaEffectCloud.alpha);
            } else if (entity instanceof ExperienceOrb) {
                return decode(config.misc.experienceOrb.color, config.misc.experienceOrb.alpha);
            } else if (entity instanceof EyeOfEnder) {
                return decode(config.misc.eyeOfEnder.color, config.misc.eyeOfEnder.alpha);
            } else if (entity instanceof FallingBlockEntity) {
                return decode(config.misc.fallingBlock.color, config.misc.fallingBlock.alpha);
            } else if (entity instanceof ItemEntity) {
                return decode(config.misc.item.color, config.misc.item.alpha);
            } else if (entity instanceof PrimedTnt) {
                return decode(config.misc.tnt.color, config.misc.tnt.alpha);
            } else if (entity instanceof EndCrystal) {
                return decode(config.misc.endCrystalEntity.color, config.misc.endCrystalEntity.alpha);
            }
        }

        return decode(config.color, config.alpha);
    }

    public static int getHexFromRelation(NodeRelation relation, int defaultTo) {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        return switch (relation) {
            case NEUTRAL -> config.teamColourPicker.neutral;
            case TOWN -> config.teamColourPicker.town;
            case NATION -> config.teamColourPicker.nation;
            case ALLIED -> config.teamColourPicker.ally;
            case ENEMIES -> config.teamColourPicker.enemy;
        };
    }

    public static Color decode(int hex, int transparency) {
        int alpha = ((100 - (transparency * 10)) * 25) / 10;
        if (alpha <= 0) {
            alpha = 1;
        }
        Color rgb = new Color(hex);
        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha);
    }

    private static Color transparent() {
        return new Color(0, 0, 0, 0);
    }

    private static boolean isMiscEntity(Entity entity) {
        return entity instanceof AreaEffectCloud || entity instanceof ExperienceOrb
                || entity instanceof EyeOfEnder || entity instanceof FallingBlockEntity
                || entity instanceof ItemEntity || entity instanceof PrimedTnt
                || entity instanceof EndCrystal;
    }
}
