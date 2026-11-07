package product;

public enum ProductStatus {
    IN_STOCK("In Stock"),
    EXPIRED("Expired"),
    DEFECTIVE("With Defects");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}