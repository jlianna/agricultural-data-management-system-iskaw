package application.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base class for every recorded transaction (sale, cash donation, item donation).
 * Subclasses define how they affect inventory and the cash wallet (polymorphism).
 */
public abstract class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final LocalDateTime timestamp;
    private final String recordedBy;
    private boolean voided;
    private String voidedBy;
    private LocalDateTime voidedAt;

    protected Transaction(String id, String recordedBy) {
        this.id = id;
        this.recordedBy = recordedBy;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getRecordedBy() { return recordedBy; }
    public boolean isVoided() { return voided; }
    public String getVoidedBy() { return voidedBy; }
    public LocalDateTime getVoidedAt() { return voidedAt; }
    public String getStatus() { return voided ? "Voided" : "Completed"; }

    /** Label such as "Sale" or "Cash Donation". */
    public abstract String getTypeLabel();

    /** Customer or donor name. */
    public abstract String getParty();

    /** Short description of what was sold / donated. */
    public abstract String getDetails();

    /** Cash that entered the wallet because of this transaction (ignores void state). */
    public abstract double getCashAmount();

    /** True for donations of items (no cash involved). */
    public boolean isInKind() { return false; }

    /** Applies the transaction's effect to inventory. */
    public abstract void applyTo(Inventory inventory) throws InsufficientStockException;

    /** Undoes the transaction's effect on inventory. */
    protected abstract void reverseFrom(Inventory inventory) throws InsufficientStockException;

    public final void voidBy(String username, Inventory inventory) throws InsufficientStockException {
        if (voided) throw new IllegalStateException("Transaction " + id + " is already voided.");
        reverseFrom(inventory);
        voided = true;
        voidedBy = username;
        voidedAt = LocalDateTime.now();
    }
}
