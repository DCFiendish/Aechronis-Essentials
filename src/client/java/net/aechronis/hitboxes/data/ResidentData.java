package net.aechronis.hitboxes.data;

/** One entry from towns.json's "residents" map: uuid -> {name, town, nation}. */
public class ResidentData {
    private final String uuid;
    private final String name;
    private final String town;
    private final String nation;

    public ResidentData(String uuid, String name, String town, String nation) {
        this.uuid = uuid;
        this.name = name;
        this.town = town;
        this.nation = nation;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    /** Nullable - the resident's town name, or null if unaffiliated. */
    public String getTown() {
        return town;
    }

    /** Nullable - the resident's nation name, or null if unaffiliated. */
    public String getNation() {
        return nation;
    }
}
