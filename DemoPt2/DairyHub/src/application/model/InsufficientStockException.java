package application.model;

/** Checked exception thrown when an operation needs more stock than is available. */
public class InsufficientStockException extends Exception {
    private static final long serialVersionUID = 1L;

    public InsufficientStockException(String productName, int available) {
        super("Not enough stock for \"" + productName + "\". Available: " + available + ".");
    }
}
