package application.model;

import java.io.Serializable;

//A dairy product offered for sale (name, description, price, etc.).
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private String description;
    private String category;
    private String unit;
    private double price;
    
    //File name of the product photo inside data/images, or null when it has no photo.
    private String imageFile;

    //Constructor for product
    public Product(String id, String name, String description, String category, String unit, double price) {
        this.id = id;
        setName(name);
        setDescription(description);
        setCategory(category);
        setUnit(unit);
        setPrice(price);
    }

    //getters
    public String getId() { 
    	return id; 
    }
    public String getName() { 
    	return name; 
    }
    public String getDescription() { 
    	return description; 
    }
    public String getCategory() { 
    	return category; 
    }
    public String getUnit() { 
    	return unit; 
    }
    public double getPrice() { 
    	return price; 
    }
    public String getImageFile() { 
    	return imageFile; 
    }
    public boolean hasImage() { 
    	return imageFile != null && !imageFile.isEmpty(); 
    }

    //set image file
    public void setImageFile(String imageFile) { 
    	this.imageFile = (imageFile == null || imageFile.isEmpty()) ? null : imageFile; 
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) 
        	throw new IllegalArgumentException("Product name is required.");
        this.name = name.trim();
    }
 // Save a trimmed description, or use an empty string if null
    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }
    // to set the category after checking it it not blank and trim extra spaces
    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) 
        	throw new IllegalArgumentException("Category is required.");
        this.category = category.trim();
    }
    // to set the unit or size after checking it is not blank and trim extra spaces
    public void setUnit(String unit) {
        if (unit == null || unit.trim().isEmpty()) 
        	throw new IllegalArgumentException("Unit / size is required.");
        this.unit = unit.trim();
    }
    
    //to make sure price is valid number above 0 before saving it
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
