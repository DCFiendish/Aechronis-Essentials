package net.aechronis.hitboxes.config;

import net.aechronis.hitboxes.AechronisHitboxes;
import net.aechronis.hitboxes.hitbox.HitboxDetectionType;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Config surface for the hitbox-coloring feature only. Structure follows CrusalisUtils'
 * ModConfig (GPLv3, github.com/oreotrollturbo/Crusalis-utils-), trimmed to the fields this
 * port actually uses - no waypoint/navigation/scoreboard-rotation/chat-utility config.
 */
@Config(name = AechronisHitboxes.MOD_ID)
public class ModConfig implements ConfigData {

    @ConfigEntry.Category(value = "general")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.ColorPicker()
    public int color = 0xFFFFFF;

    @ConfigEntry.Category(value = "general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 10, min = 0)
    public int alpha = 10;

    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Category(value = "players")
    public boolean isPlayerConfigEnabled = true;

    @ConfigEntry.Category(value = "players")
    @ConfigEntry.Gui.CollapsibleObject
    public PlayerListConfig friend = new PlayerListConfig(0x20FF00);

    @ConfigEntry.Category(value = "players")
    @ConfigEntry.Gui.CollapsibleObject
    public PlayerListConfig enemy = new PlayerListConfig(0xD40000);

    @ConfigEntry.Category(value = "players")
    @ConfigEntry.Gui.CollapsibleObject
    public PlayerSingleConfig neutral = new PlayerSingleConfig();

    @ConfigEntry.Category(value = "players")
    @ConfigEntry.Gui.CollapsibleObject
    public PlayerSingleConfig self = new PlayerSingleConfig();

    @ConfigEntry.Category(value = "entity")
    @ConfigEntry.Gui.CollapsibleObject
    public Entity passive = new Entity();

    @ConfigEntry.Category(value = "entity")
    @ConfigEntry.Gui.CollapsibleObject
    public Entity hostile = new Entity();

    @ConfigEntry.Category(value = "entity")
    @ConfigEntry.Gui.CollapsibleObject
    public Entity decoration = new Entity();

    @ConfigEntry.Category(value = "entity")
    @ConfigEntry.Gui.CollapsibleObject
    public ProjectileEntity projectile = new ProjectileEntity();

    @ConfigEntry.Category(value = "entity")
    @ConfigEntry.Gui.CollapsibleObject
    public Entity vehicle = new Entity();

    @ConfigEntry.Category(value = "entity")
    @ConfigEntry.Gui.CollapsibleObject
    public EnderDragonEntity enderDragon = new EnderDragonEntity();

    @ConfigEntry.Category(value = "entity")
    @ConfigEntry.Gui.CollapsibleObject
    public MiscEntityDropdown misc = new MiscEntityDropdown();

    @ConfigEntry.Category(value = "teamColor")
    public boolean autoHitboxColour = true;

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @ConfigEntry.Category(value = "teamColor")
    public HitboxDetectionType hitboxDetectionType = HitboxDetectionType.MAP_DATA;

    @ConfigEntry.Category(value = "teamColor")
    @ConfigEntry.Gui.CollapsibleObject
    public TeamColourPicker teamColourPicker = new TeamColourPicker();

    @ConfigEntry.Category(value = "teamColor")
    public boolean autoChunkBorders = true;

    @ConfigEntry.Category(value = "aechronis")
    @ConfigEntry.Gui.Tooltip
    public String mapLink = "https://map.aechronis.net";

    public static class MiscEntityDropdown {
        public boolean isEnabled = false;

        @ConfigEntry.Gui.CollapsibleObject
        public MiscEntity areaEffectCloud = new MiscEntity();

        @ConfigEntry.Gui.CollapsibleObject
        public MiscEntity experienceOrb = new MiscEntity();

        @ConfigEntry.Gui.CollapsibleObject
        public MiscEntity eyeOfEnder = new MiscEntity();

        @ConfigEntry.Gui.CollapsibleObject
        public MiscEntity fallingBlock = new MiscEntity();

        @ConfigEntry.Gui.CollapsibleObject
        public MiscEntity item = new MiscEntity();

        @ConfigEntry.Gui.CollapsibleObject
        public MiscEntity tnt = new MiscEntity();

        @ConfigEntry.Gui.CollapsibleObject
        public MiscEntity endCrystalEntity = new MiscEntity();

        @ConfigEntry.Gui.CollapsibleObject
        public MiscEntity enderPearlEntity = new MiscEntity();

        @ConfigEntry.Gui.CollapsibleObject
        public MiscEntity tridentEntity = new MiscEntity();
    }

    public static class ProjectileEntity {
        public boolean isEnabled = false;

        @ConfigEntry.ColorPicker()
        public int color = 0xFFFFFF;

        @ConfigEntry.BoundedDiscrete(max = 10, min = 0)
        public int alpha = 10;

        public boolean renderStuck = false;
    }

    public static class Entity {
        public boolean isEnabled = false;

        @ConfigEntry.ColorPicker()
        public int color = 0xFFFFFF;

        @ConfigEntry.BoundedDiscrete(max = 10, min = 0)
        public int alpha = 10;
    }

    public static class MiscEntity {
        @ConfigEntry.ColorPicker()
        public int color = 0xFFFFFF;

        @ConfigEntry.BoundedDiscrete(max = 10, min = 0)
        public int alpha = 10;
    }

    public static class EnderDragonEntity {
        public boolean isEnabled = true;
        public boolean boxHitbox = false;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.ColorPicker()
        public int color = 0xFFFFFF;

        @ConfigEntry.BoundedDiscrete(max = 10, min = 0)
        public int alpha = 10;
    }

    public static class PlayerSingleConfig {
        @ConfigEntry.ColorPicker()
        public int color = 0xFFFFFF;

        @ConfigEntry.BoundedDiscrete(max = 10, min = 0)
        public int alpha = 10;
    }

    public static class PlayerListConfig {
        public PlayerListConfig() {
        }

        public PlayerListConfig(int color) {
            this.color = color;
        }

        public List<String> list = new ArrayList<>();

        @ConfigEntry.ColorPicker()
        public int color;

        @ConfigEntry.BoundedDiscrete(max = 10, min = 0)
        public int alpha = 10;
    }

    public static class TeamColourPicker {
        @ConfigEntry.ColorPicker()
        public int town = 0x55FF55;

        @ConfigEntry.ColorPicker()
        public int nation = 0x00AA00;

        @ConfigEntry.ColorPicker()
        public int ally = 0x00AAAA;

        @ConfigEntry.ColorPicker()
        public int enemy = 0xFF5555;

        @ConfigEntry.ColorPicker()
        public int neutral = 0xFFAA00;
    }
}
