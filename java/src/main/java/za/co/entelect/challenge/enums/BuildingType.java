package za.co.entelect.challenge.enums;

public enum BuildingType {
    DEFENSE("0"),
    ATTACK("1"),
    ENERGY("2"),
    DECONSTRUCT("3"),
    TESLA("4"),
    IRONCURTAIN("5");
    private final String type;

    BuildingType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
