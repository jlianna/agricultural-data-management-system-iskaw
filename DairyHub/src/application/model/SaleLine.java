package application.model;

import java.io.Serializable;

/** One line of a sale: a product, a quantity and the price at the time of sale. */
public class SaleLine implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String productId;
    private final String productName;
    private final int quantity;
    private final double unitPrice;

    public SaleLine(Product product, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        this.productId = product.getId();
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getSubtotal() { return unitPrice * quantity; }
}
