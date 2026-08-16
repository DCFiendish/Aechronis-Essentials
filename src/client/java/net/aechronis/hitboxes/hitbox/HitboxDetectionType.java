package net.aechronis.hitboxes.hitbox;

/**
 * How a player's auto/relation-based hitbox color is derived. CrusalisUtils also had
 * PREFIX/PREFIX_AND_NAME/NAME/ALL_OF_THEM modes that parse nametag/tab-list color codes;
 * this port only keeps MAP_DATA (pure Aechronis Nodes relation lookup) since Aechronis's
 * nametag/tab color convention isn't confirmed the way the map API is.
 */
public enum HitboxDetectionType {
    MAP_DATA
}
