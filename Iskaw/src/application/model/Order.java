package application.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An order placed by a customer. Placing an order does not touch stock or the wallet:
 * when the customer picks it up and pays cash, a clerk fulfils it, which records a normal
 * {@link SaleTransaction} linked to the customer.
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        PENDING("Pending"), COMPLETED("Completed"), CANCELLED("Cancelled");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final String id;
    private final String customerUsername;
    private final String customerName;
    private final List<SaleLine> lines;
    private LocalDateTime timestamp = LocalDateTime.now();
    private Status status = Status.PENDING;
    private String handledBy;
    private LocalDateTime handledAt;
    private String saleId;

    public Order(String id, String customerUsername, String customerName, List<SaleLine> lines) {
        this.id = id;
        this.customerUsername = customerUsername;
        this.customerName = customerName;
        this.lines = new ArrayList<>(lines);
    }

    public String getId() { return id; }
    public String getCustomerUsername() { return customerUsername; }
    public String getCustomerName() { return customerName; }
    public List<SaleLine> getLines() { return Collections.unmodifiableList(lines); }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Status getStatus() { return status; }
    public String getHandledBy() { return handledBy; }
    public LocalDateTime getHandledAt() { return handledAt; }
    /** Id of the sale created when the order was fulfilled, or null. */
    public String getSaleId() { return saleId; }

    public double getTotal() { return lines.stream().mapToDouble(SaleLine::getSubtotal).sum(); }
    public int getItemCount() { return lines.stream().mapToInt(SaleLine::getQuantity).sum(); }

    public String getDetails() {
        return lines.stream().map(l -> l.getQuantity() + "x " + l.getProductName()).collect(Collectors.joining(", "));
    }

    /** Used when loading saved data. */
    public void restoreState(LocalDateTime timestamp, Status status, String handledBy, LocalDateTime handledAt, String saleId) {
        if (timestamp != null) this.timestamp = timestamp;
        if (status != null) this.status = status;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
        this.saleId = saleId;
    }

    void complete(String by, String saleId) {
        status = Status.COMPLETED;
        handledBy = by;
        handledAt = LocalDateTime.now();
        this.saleId = saleId;
    }

    void cancel(String by) {
        status = Status.CANCELLED;
        handledBy = by;
        handledAt = LocalDateTime.now();
    }
}
