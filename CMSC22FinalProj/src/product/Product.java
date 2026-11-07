package product;

public class Product {
	private String id;
	private ProductCategory category; // dairy, meat, cowhideLeather
	private String name;
	private String variant; // "Choco 500ml", "Cognac Full Hide"
	private double price;
	private int stockQuantity;
	private ProductStatus status; // In Stock, Expired, Defective/Faulty
	
	public Product(String id, ProductCategory category, String name, String variant, double price, int stockQuantity) {
        this(id, category, name, variant, price, stockQuantity, ProductStatus.IN_STOCK);
    }
	
	public Product(String id, ProductCategory category, String name, String variant, double price, int stockQuantity, ProductStatus status) {
		this.id = id;
		this.category = category;
		this.name = name;
		this.variant = variant;
		this.price = price;
		this.stockQuantity = stockQuantity;
		this.status = status;
	}

	// Getters and Setters
	public String getId() {
        return id;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

}

