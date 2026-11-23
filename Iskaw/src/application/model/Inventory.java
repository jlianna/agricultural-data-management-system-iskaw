package application.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/** Tracks the on-hand quantity of every product. */
public class Inventory implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Stock at or below this level is flagged as "low". */
    public static final int LOW_STOCK_THRESHOLD = 10;

    private final Map<String, Integer> stock = new HashMap<>();

    public int getStock(String productId) {
        return stock.getOrDefault(productId, 0);
    }

    public void setStock(String productId, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        stock.put(productId, quantity);
    }

    public void restock(String productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        stock.merge(productId, quantity, Integer::sum);
    }

    public void deduct(String productId, String productName, int quantity) throws InsufficientStockException {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        int available = getStock(productId);
        if (available < quantity) throw new InsufficientStockException(productName, available);
        stock.put(productId, available - quantity);
    }

    public void remove(String productId) {
        stock.remove(productId);
    }

    public boolean isLow(String productId) {
        return getStock(productId) <= LOW_STOCK_THRESHOLD;
    }

    public boolean isOut(String productId) {
        return getStock(productId) == 0;
    }

    public String statusOf(String productId) {
        if (isOut(productId)) return "Out of stock";
        if (isLow(productId)) return "Low stock";
        return "In stock";
    }
}
