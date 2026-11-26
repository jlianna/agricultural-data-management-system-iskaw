package application.model;


//One line of a sale: a product, a quantity and the price at the time of sale.
public class SaleLine{

    private final String productId;
    private final String productName;
    private final int quantity;
    private final double unitPrice;

    //Constructor for Saleline
    public SaleLine(Product product, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        this.productId = product.getId();
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }

    //Used when loading saved data (keeps the price the customer actually paid).
    public SaleLine(String productId, String productName, int quantity, double unitPrice) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
    
    //getters
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getSubtotal() { return unitPrice * quantity; }
}
