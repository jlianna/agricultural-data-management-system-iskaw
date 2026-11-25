package application.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// Represents a batch of donated items dispatched to a charity recipient
public class CharityDispatch implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private LocalDateTime timestamp;
    private final String dispatchedBy;
    private final String charityName;
    private final String notes;
    private final Map<String, Integer> items; // item label/name -> quantity sent

    public CharityDispatch(String id, String dispatchedBy, String charityName, String notes, Map<String, Integer> items) {
        this.id = id;
        this.timestamp = LocalDateTime.now();
        this.dispatchedBy = dispatchedBy;
        this.charityName = charityName.trim();
        this.notes = notes == null ? "" : notes.trim();
        this.items = Collections.unmodifiableMap(new LinkedHashMap<>(items));
    }

    public void restoreTimestamp(LocalDateTime timestamp) {
        if (timestamp != null) this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDispatchedBy() { return dispatchedBy; }
    public String getCharityName() { return charityName; }
    public String getNotes() { return notes; }
    public Map<String, Integer> getItems() { return items; }

    public int getTotalUnits() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }

    public String getItemsSummary() {
        return items.entrySet().stream()
                .map(e -> e.getValue() + " x " + e.getKey())
                .collect(Collectors.joining(", "));
    }
}