package application.model;

import java.io.Serializable;

/** A dairy product offered for sale (name, description, price, etc.). */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private String description;
    private String category;
    private String unit;
    private double price;

    public Product(String id, String name, String description, String category, String unit, double price) {
        this.id = id;
        setName(name);
        setDescription(description);
        setCategory(category);
        setUnit(unit);
        setPrice(price);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public double getPrice() { return price; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Product name is required.");
        this.name = name.trim();
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) throw new IllegalArgumentException("Category is required.");
        this.category = category.trim();
    }

    public void setUnit(String unit) {
        if (unit == null || unit.trim().isEmpty()) throw new IllegalArgumentException("Unit / size is required.");
        this.unit = unit.trim();
    }

    public void setPrice(double price) {
        if (Double.isNaN(price) || Double.isInfinite(price) || price <= 0)
            throw new IllegalArgumentException("Price must be greater than zero.");
        this.price = price;
    }

    @Override
    public String toString() {
        return name + " (" + unit + ")";
    }
}
