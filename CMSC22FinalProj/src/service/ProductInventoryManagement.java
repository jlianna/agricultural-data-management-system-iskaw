package service;
import java.util.ArrayList;
import java.util.List;

import product.Product;
import product.ProductCategory;
import product.ProductStatus;

public class ProductInventoryManagement {
	private static ProductInventoryManagement instance;
	private final List<Product> inventory = new ArrayList<>();
	
	private ProductInventoryManagement() {
		seedInitialProducts();
	}
	
	// seeds the inventory with all required products
	private void seedInitialProducts() {
        int idCounter = 1000;

        // --- 1. DAIRY CATEGORY ---
        String[] dairySizes = {"2L", "1L", "500ml", "200ml"};

        // Fresh Milk
        double[] freshMilkPrices = {275.0, 140.0, 75.0, 40.0};
        for (int i = 0; i < dairySizes.length; i++) {
            inventory.add(new Product("P" + (++idCounter), ProductCategory.DAIRY, "Fresh Milk", dairySizes[i], freshMilkPrices[i], 50));
        }

        // Flavored Milk (Choco, Strawberry, Ube)
        String[] milkFlavors = {"Choco", "Strawberry", "Ube"};
        double[] flavoredMilkPrices = {315.0, 160.0, 85.0, 50.0};
        for (String flavor : milkFlavors) {
            for (int i = 0; i < dairySizes.length; i++) {
                inventory.add(new Product("P" + (++idCounter), ProductCategory.DAIRY, "Flavored Milk", flavor + " - " + dairySizes[i], flavoredMilkPrices[i], 30));
            }
        }

        // Cheese (200g fixed size)
        inventory.add(new Product("P" + (++idCounter), ProductCategory.DAIRY, "Kesong Puti", "200g", 120.0, 40));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.DAIRY, "Mozzarella", "200g", 160.0, 40));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.DAIRY, "Cheddar", "200g", 200.0, 40));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.DAIRY, "Ricotta", "200g", 150.0, 30));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.DAIRY, "Unsalted Butter", "200g", 160.0, 50));

        // Yogurt
        String[] yogurtSizes = {"1L", "500ml", "200ml"};
        double[] plainYogurtPrices = {160.0, 80.0, 45.0};
        double[] flavoredYogurtPrices = {245.0, 125.0, 55.0};

        for (int i = 0; i < yogurtSizes.length; i++) {
            inventory.add(new Product("P" + (++idCounter), ProductCategory.DAIRY, "Plain Yogurt", yogurtSizes[i], plainYogurtPrices[i], 30));
            inventory.add(new Product("P" + (++idCounter), ProductCategory.DAIRY, "Flavored Yogurt (Seasonal)", yogurtSizes[i], flavoredYogurtPrices[i], 30));
        }

        // --- 2. BEEF CUTS CATEGORY (All packed per 1kg) ---
        String unit = "1kg pack";
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Chuck", unit, 420.0, 25));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Brisket with bones", unit, 380.0, 25));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Brisket, boneless", unit, 480.0, 25));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Beef Cubes", unit, 450.0, 30));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Beef Shank", unit, 420.0, 20));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Beef Ribs", unit, 400.0, 20));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Short Ribs", unit, 450.0, 20));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Sirloin", unit, 550.0, 25));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Tenderloin", unit, 650.0, 15));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Ribeye", unit, 600.0, 15));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Ground Beef", unit, 420.0, 40));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Beef Round", unit, 450.0, 25));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Flank Steak", unit, 480.0, 20));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Beef Plate", unit, 420.0, 20));
        inventory.add(new Product("P" + (++idCounter), ProductCategory.BEEF_CUTS, "Beef Offal", unit + " (Subject to availability)", 280.0, 15));

        // --- 3. COWHIDE LEATHER CATEGORY ---
        String[] colors = {"Cognac", "Dark Brown", "Dark Charcoal Gray"};
        String[] leatherSizes = {"Full Hide", "Half Hide", "Quarter Hide", "Coupon", "Double Panel", "Panel"};
        double[] leatherPrices = {10800.0, 5850.0, 3150.0, 1500.0, 650.0, 350.0};

        for (String color : colors) {
            for (int i = 0; i < leatherSizes.length; i++) {
                inventory.add(new Product(
                    "P" + (++idCounter), 
                    ProductCategory.COWHIDE_LEATHER, 
                    "Cowhide Leather (" + color + ")", 
                    leatherSizes[i], 
                    leatherPrices[i], 
                    10
                ));
            }
        }
    }
	
}
