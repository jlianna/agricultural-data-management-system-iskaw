package product;

public enum ProductCategory {
    DAIRY("Dairy"),
    BEEF_CUTS("Beef Cuts"),
    COWHIDE_LEATHER("Cowhide Leather");

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}