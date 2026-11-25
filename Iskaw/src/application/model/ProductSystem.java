package application.model;

import application.storage.DataFiles;
import application.storage.OrderFileReader;
import application.storage.OrderFileWriter;
import application.storage.ProductFileReader;
import application.storage.ProductFileWriter;
import application.storage.SaleLineFileReader;
import application.storage.SaleLineFileWriter;
import application.storage.TransactionFileReader;
import application.storage.TransactionFileWriter;
import application.storage.UserFileReader;
import application.storage.UserFileWriter;
import java.io.IOException;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The "System" class of the project: the single entry point to all business logic.
 * It owns users, products, inventory and transactions, enforces role permissions
 * and persists everything to disk after each change.
 */
public class ProductSystem {

    //Everything that is written to disk.
    //In-memory data. Also the shape of the old binary save file (kept so old saves can still be imported).
    static class Store{
        private static final long serialVersionUID = 1L;
        final List<User> users = new ArrayList<>();
        final List<Product> products = new ArrayList<>();
        final Inventory inventory = new Inventory();
        final List<Transaction> transactions = new ArrayList<>();
        int transactionCounter = 0;
        List<Order> orders = new ArrayList<>();   // null when read from a save made before orders existed
        int orderCounter = 0;
    }

    private final DataFiles files;
    private Store store;
    
    // Set when damaged save files had to be set aside during start-up. 
    private String startupWarning;

    //Uses the folder {@code data/} next to where the program is run.
    public ProductSystem() {
        this(Paths.get("data"));
    }

    //@param dataDir folder that holds users.txt, products.txt, transactions.txt, orders.txt and images
    public ProductSystem(Path dataDir) {
    	
        this.files = new DataFiles(dataDir);
        load();
    }

    public String getStartupWarning() { 
    	return startupWarning; 
    }

    // ------------------------------------------------------------------ persistence

    private void load() {
        if (files.exists()) {
            try {
                store = loadFromFiles();
                // Add the new Iskaw products to older saved files as well
                // This will keep the products available even if the user already has saved data
                attachDefaultImages();
                save();
                return;
                
            } catch (IOException e) {
                try { 
                	files.setAsideDamagedFiles(); 
                } catch (IOException ignored) { }
                startupWarning = "The saved data could not be read (" + e.getMessage() + "). The damaged files were "
                        + "moved to data/corrupt-*/ and sample data was loaded instead.";
            }
        } 
        store = new Store();
        seed();
        attachDefaultImages();
        save();
    }

   
    //Loads every data file with the reader classes of the storage package.
    private Store loadFromFiles() throws IOException {
        Store s = new Store();
        s.users.addAll(UserFileReader.read(files.users()));
        s.products.addAll(ProductFileReader.read(files.products(), s.inventory));
        
        var lines = SaleLineFileReader.read(files.lines());
        
        s.transactions.addAll(TransactionFileReader.read(files.transactions(), lines));
        s.orders.addAll(OrderFileReader.read(files.orders(), lines));
        // new ids continue after the highest saved one (e.g. T-00017 -> 18)
        s.transactionCounter = highest(s.transactions.stream().map(Transaction::getId).collect(Collectors.toList()));
        s.orderCounter = highest(s.orders.stream().map(Order::getId).collect(Collectors.toList()));
        return s;
    }

    private static int highest(List<String> ids) {
        int max = ids.size();
        for (String id : ids) {
            try {
                max = Math.max(max, Integer.parseInt(id.substring(id.indexOf('-') + 1)));
            } catch (RuntimeException ignored) { }
        }
        return max;
    }

    // Writes all data to disk with the writer classes of the storage package. Called after every change.
    public synchronized void save() {
        try {
            UserFileWriter.write(files.users(), store.users);
            ProductFileWriter.write(files.products(), store.products, store.inventory);
            TransactionFileWriter.write(files.transactions(), store.transactions);
            OrderFileWriter.write(files.orders(), store.orders);
            SaleLineFileWriter.write(files.lines(), store.transactions, store.orders);
            java.nio.file.Files.createDirectories(files.images());
        } catch (IOException e) {
            throw new IllegalStateException("Could not save data: " + e.getMessage(), e);
        }
    }

    private void seed() {
        store.users.add(new Manager("manager", "Maria Santos", "manager123"));
        store.users.add(new Clerk("clerk1", "Juan Dela Cruz", "clerk123"));
        store.users.add(new Clerk("clerk2", "Ana Reyes", "clerk123"));
        store.users.add(new Customer("customer1", "Rosa Mendoza", "customer123"));

        //add dairy products
        addSeed("Fresh Cow's Milk", "Farm-fresh whole cow's milk, pasteurized.", "Milk", "1 L", 85, 55);
        addSeed("Chocolate Milk", "Low-sugar chocolate drink made with fresh milk.", "Milk", "250 ml", 35, 70);
        addSeed("Kesong Puti", "Soft, lightly salted white cheese made from carabao milk.", "Cheese", "200 g", 120, 18);
        addSeed("Mozzarella Cheese", "Stretchy mozzarella, great for pizza and baking.", "Cheese", "250 g", 210, 9);
        addSeed("Cream Cheese", "Smooth spreadable cream cheese.", "Cheese", "150 g", 130, 22);
        addSeed("Plain Yogurt", "Thick, unsweetened yogurt with live cultures.", "Yogurt", "500 g", 110, 30);
        addSeed("Strawberry Yogurt Drink", "Drinkable yogurt blended with real strawberries.", "Yogurt", "250 ml", 45, 6);
        addSeed("Salted Butter", "Churned butter from fresh cream, lightly salted.", "Butter & Cream", "200 g", 150, 14);
        addSeed("All-Purpose Cream", "Chilled table cream for desserts and sauces.", "Butter & Cream", "250 ml", 70, 0);
        addSeed("Ube Ice Cream", "Creamy purple-yam ice cream.", "Frozen", "1 L", 260, 12);
        addSeed("Pastillas de Leche", "Traditional milk candy, pack of 10 pieces.", "Sweets", "10 pcs", 60, 45);

        // Add the beef 
        addSeed("Chuck", "Beef cut, priced per kilogram.", "Beef", "1 kg", 420, 20);
    	addSeed("Brisket with Bones", "Beef cut, priced per kilogram.", "Beef", "1 kg", 380, 20);
    	addSeed("Brisket, Boneless", "Boneless beef brisket, priced per kilogram.", "Beef", "1 kg", 480, 20);
    	addSeed("Beef Cubes", "Beef cubes, priced per kilogram.", "Beef", "1 kg", 450, 20);
    	addSeed("Beef Shank", "Beef shank, priced per kilogram.", "Beef", "1 kg", 420, 20);
    	addSeed("Beef Ribs", "Beef ribs, priced per kilogram.", "Beef", "1 kg", 400, 20);
    	addSeed("Short Ribs", "Short ribs, priced per kilogram.", "Beef", "1 kg", 450, 20);
    	addSeed("Sirloin", "Sirloin, priced per kilogram.", "Beef", "1 kg", 550, 20);
    	addSeed("Tenderloin", "Tenderloin, priced per kilogram.", "Beef", "1 kg", 650, 20);
    	addSeed("Ribeye", "Ribeye, priced per kilogram.", "Beef", "1 kg", 600, 20);
    	addSeed("Ground Beef", "Ground beef, priced per kilogram.", "Beef", "1 kg", 420, 20);
    	addSeed("Beef Round", "Beef round, priced per kilogram.", "Beef", "1 kg", 450, 20);
    	addSeed("Flank Steak", "Flank steak, priced per kilogram.", "Beef", "1 kg", 480, 20);
    	addSeed("Beef Plate", "Beef plate, priced per kilogram.", "Beef", "1 kg", 420, 20);
    	addSeed("Beef Offal", "Edible beef offal, priced per kilogram.", "Beef", "1 kg", 280, 20);

    	//add cowhide products for the Iskaw shop
        // Each hide color is saved as its own product so the stock can be tracked separately
        String[] colors = {"Cognac", "Dark Brown", "Dark Charcoal Gray"};
        for (String color : colors) {
        	addSeed("Full Hide - " + color, "Cowhide color: " + color + ".", "Cowhide", "1 hide", 10800, 5);
        	addSeed("Half Hide - " + color, "Cowhide color: " + color + ".", "Cowhide", "1/2 hide", 5850, 5);
        	addSeed("Quarter Hide - " + color, "Cowhide color: " + color + ".", "Cowhide", "1/4 hide", 3150, 5);
        	addSeed("Coupon - " + color, "Cowhide color: " + color + ".", "Cowhide", "1 piece", 1500, 5);
        	addSeed("Double Panel - " + color, "Cowhide color: " + color + ".", "Cowhide", "1 piece", 650, 5);
        	addSeed("Panel - " + color, "Cowhide color: " + color + ".", "Cowhide", "1 piece", 350, 5);
        }
        
    }

   // add Images
    private void attachDefaultImages() {
        Path imgDir = files.images();
        if (!Files.isDirectory(imgDir)) 
        	return;
        
        for (Product p : store.products) {
            if (p.hasImage() && Files.isRegularFile(imgDir.resolve(p.getImageFile()))) 
            	continue;
            
            for (String ext : IMAGE_TYPES) {
                String candidate = p.getName() + "." + ext;
                if (Files.isRegularFile(imgDir.resolve(candidate))) {
                    p.setImageFile(candidate);
                    break;
                }
            }
        }
    }

    private void addSeed(String name, String desc, String category, String unit, double price, int stock) {
    	
        Product p = new Product(newProductId(), name, desc, category, unit, price);
        store.products.add(p);
        store.inventory.setStock(p.getId(), stock);
    }

    private String newProductId() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ------------------------------------------------------------------ users

    //@throws IllegalArgumentException when the credentials are wrong
    public User login(String username, String password) {
        String key = username == null ? "" : username.trim().toLowerCase();
        return store.users.stream()
                .filter(u -> u.getUsername().equals(key) && u.checkPassword(password))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Incorrect username or password."));
    }

    public List<User> getUsers() { 
    	return Collections.unmodifiableList(store.users); 
    }

    public List<Clerk> getClerks() {
        return store.users.stream().filter(u -> u instanceof Clerk).map(u -> (Clerk) u).collect(Collectors.toList());
    }

    public Optional<User> findUser(String username) {
        return store.users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    //Display name for a username (falls back to the username itself). 
    public String displayName(String username) {
        return findUser(username).map(User::getFullName).orElse(username);
    }

    public synchronized Clerk addClerk(User actor, String username, String fullName, String password) {
        requireManager(actor, "add staff accounts");
        
        if (findUser(username == null ? "" : username.trim().toLowerCase()).isPresent())
            throw new IllegalArgumentException("That username is already taken.");
        
        Clerk c = new Clerk(username, fullName, password);
        store.users.add(c);
        save();
        return c;
    }

    //Self sign-up: anyone can create a customer account. 
    public synchronized Customer registerCustomer(String username, String fullName, String password) {
        String key = username == null ? "" : username.trim().toLowerCase();
        if (findUser(key).isPresent()) throw new IllegalArgumentException("That username is already taken.");
        Customer c = new Customer(username, fullName, password); // validates input
        store.users.add(c);
        save();
        return c;
    }

    public List<Customer> getCustomers() {
        return store.users.stream().filter(u -> u instanceof Customer).map(u -> (Customer) u)
                .sorted(Comparator.comparing(Customer::getFullName)).collect(Collectors.toList());
    }

    // Sales linked to a customer account, newest first (voided ones included).
    public List<SaleTransaction> getPurchasesOf(String username) {
        return getTransactions().stream()
                .filter(t -> t instanceof SaleTransaction && username.equals(((SaleTransaction) t).getCustomerUsername()))
                .map(t -> (SaleTransaction) t).collect(Collectors.toList());
    }

    private void requireStaff(User actor, String action) {
        if (actor == null || !actor.isStaff()) 
        	throw new SecurityException("Only staff can " + action + ".");
    }

    private void requireManager(User actor, String action) {
        if (actor == null || !actor.isManager())
            throw new SecurityException("Only a manager can " + action + ".");
    }

    // ------------------------------------------------------------------ products & inventory

    public Inventory getInventory() { 
    	return store.inventory; 
    }

    public List<Product> getProducts() {
        List<Product> copy = new ArrayList<>(store.products);
        copy.sort(Comparator.comparing(Product::getCategory).thenComparing(Product::getName));
        return copy;
    }

    public List<String> getCategories() {
        return store.products.stream().map(Product::getCategory).distinct().sorted().collect(Collectors.toList());
    }

    public Optional<Product> findProduct(String id) {
        return store.products.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public synchronized Product addProduct(User actor, String name, String description, String category,
                                           String unit, double price, int initialStock) {
        requireManager(actor, "add products");
        if (initialStock < 0) 
        	throw new IllegalArgumentException("Initial stock cannot be negative.");
        Product p = new Product(newProductId(), name, description, category, unit, price); // validates input
        
        ensureUnique(p.getName(), p.getUnit(), null);
        
        store.products.add(p);
        store.inventory.setStock(p.getId(), initialStock);
        save();
        return p;
    }

    public synchronized void updateProduct(User actor, Product p, String name, String description,
                                           String category, String unit, double price) {
        requireManager(actor, "edit products");
        Product check = new Product(p.getId(), name, description, category, unit, price); // validates input first
        ensureUnique(check.getName(), check.getUnit(), p);
        
        p.setName(name);
        p.setDescription(description);
        p.setCategory(category);
        p.setUnit(unit);
        p.setPrice(price);
        save();
    }

    public synchronized void deleteProduct(User actor, Product p) {
        requireManager(actor, "delete products");
        store.products.remove(p);
        store.inventory.remove(p.getId());
        
        deleteImageFile(p.getImageFile());
        save();
    }

    // ------------------------------------------------------------------ product photos

    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;
    private static final List<String> IMAGE_TYPES = java.util.Arrays.asList("png", "jpg", "jpeg", "gif", "bmp");

    //Copies a picture chosen by the manager into the data/images folder and attaches it to the product.
    public synchronized void setProductImage(User actor, Product p, Path source) {
    	
        requireManager(actor, "change product photos");
        if (source == null || !Files.isRegularFile(source)) 
        	throw new IllegalArgumentException("That picture file could not be found.");
        String name = source.getFileName().toString().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        
        if (!IMAGE_TYPES.contains(ext))
            throw new IllegalArgumentException("Please choose a PNG, JPG, GIF or BMP picture.");
        try {
            if (Files.size(source) > MAX_IMAGE_BYTES)
                throw new IllegalArgumentException("That picture is larger than 8 MB. Please choose a smaller one.");
            Files.createDirectories(files.images());
            
            String stored = p.getId() + "-" + System.currentTimeMillis() + "." + ext;
            Files.copy(source, files.images().resolve(stored), StandardCopyOption.REPLACE_EXISTING);
            String old = p.getImageFile();
            p.setImageFile(stored);
            deleteImageFile(old);
            save();
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the picture: " + e.getMessage(), e);
        }
    }

    public synchronized void removeProductImage(User actor, Product p) {
        requireManager(actor, "change product photos");
        String old = p.getImageFile();
        p.setImageFile(null);
        
        deleteImageFile(old);
        save();
    }

    //The picture file of a product, or null when it has none (or the file went missing).
    public Path getImagePath(Product p) {
    	
        if (p == null || !p.hasImage()) 
        	return null;
        
        Path f = files.images().resolve(p.getImageFile());
        return Files.isRegularFile(f) ? f : null;
    }

    private void deleteImageFile(String file) {
        if (file == null || file.isEmpty()) 
        	return;
        try { 
        	Files.deleteIfExists(files.images().resolve(file)); 
        }  catch (IOException ignored) { }
    }

    private void ensureUnique(String name, String unit, Product ignore) {
        boolean dup = store.products.stream().anyMatch(o -> o != ignore
                && o.getName().equalsIgnoreCase(name) && o.getUnit().equalsIgnoreCase(unit));
        if (dup) 
        	throw new IllegalArgumentException("A product named \"" + name + "\" (" + unit + ") already exists.");
    }

    public synchronized void restock(User actor, Product p, int quantity) {
        requireManager(actor, "restock products");
        store.inventory.restock(p.getId(), quantity);
        save();
    }

    public List<Product> getLowStockProducts() {
        return getProducts().stream().filter(p -> store.inventory.isLow(p.getId())).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ transactions

    private String nextTransactionId() {
        return String.format("T-%05d", store.transactionCounter + 1);
    }

    private <T extends Transaction> T commit(T t) {
        store.transactionCounter++;
        store.transactions.add(t);
        save();
        return t;
    }

    public synchronized SaleTransaction recordSale(User actor, String customer, List<SaleLine> lines,
                                                   double cashTendered) throws InsufficientStockException {
        return recordSale(actor, customer, null, lines, cashTendered);
    }

    //@param customerUsername username of a registered customer to link the sale to, or null
    public synchronized SaleTransaction recordSale(User actor, String customer, String customerUsername,
                                                   List<SaleLine> lines, double cashTendered)
            throws InsufficientStockException {
        requireStaff(actor, "record sales");
        
        if (customerUsername != null && !findUser(customerUsername).filter(u -> u instanceof Customer).isPresent())
            throw new IllegalArgumentException("That customer account does not exist.");
        
        SaleTransaction t = new SaleTransaction(nextTransactionId(), actor.getUsername(), customer,
                customerUsername, lines, cashTendered);
        t.applyTo(store.inventory);
        return commit(t);
    }

    public synchronized CashDonation recordCashDonation(User actor, String donor, double amount) {
        requireStaff(actor, "record donations");
        return commit(new CashDonation(nextTransactionId(), actor.getUsername(), donor, amount));
    }

    public synchronized ItemDonation recordItemDonation(User actor, String donor, Product product, int quantity) {
        requireStaff(actor, "record donations");
        ItemDonation t = new ItemDonation(nextTransactionId(), actor.getUsername(), donor, product, quantity);
        t.applyTo(store.inventory);
        return commit(t);
    }

    //Voids any recorded transaction and reverses its effect on inventory.
    public synchronized void voidTransaction(User actor, Transaction t) throws InsufficientStockException {
        requireStaff(actor, "void transactions");
        t.voidBy(actor.getUsername(), store.inventory);
        save();
    }

    // Newest first.
    public List<Transaction> getTransactions() {
        List<Transaction> copy = new ArrayList<>(store.transactions);
        Collections.reverse(copy);
        return copy;
    }

    public List<Transaction> getTransactionsBy(String username) {
        return getTransactions().stream().filter(t -> t.getRecordedBy().equals(username)).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ customer orders

    //A customer places an order. Stock is checked now but only deducted when a clerk fulfils it.
    public synchronized Order placeOrder(User actor, List<SaleLine> lines) throws InsufficientStockException {
        if (!(actor instanceof Customer)) 
        	throw new SecurityException("Only customers can place orders.");
        
        if (lines == null || lines.isEmpty()) 
        	throw new IllegalArgumentException("Add at least one product to your order.");
        
        for (SaleLine l : lines) {
            int available = store.inventory.getStock(l.getProductId());
            if (available < l.getQuantity()) 
            	throw new InsufficientStockException(l.getProductName(), available);
        }
        Order o = new Order(String.format("O-%05d", store.orderCounter + 1), actor.getUsername(), actor.getFullName(), lines);
        
        store.orderCounter++;
        store.orders.add(o);
        save();
        return o;
    }

    //Newest first.
    public List<Order> getOrders() {
        List<Order> copy = new ArrayList<>(store.orders);
        Collections.reverse(copy);
        return copy;
    }

    public List<Order> getOrdersOf(String username) {
        return getOrders().stream().filter(o -> o.getCustomerUsername().equals(username)).collect(Collectors.toList());
    }

    public long getPendingOrderCount() {
        return store.orders.stream().filter(o -> o.getStatus() == Order.Status.PENDING).count();
    }

    /**
     * Staff confirm that the customer collected and paid for the order: records a sale linked to the
     * customer (deducting stock and adding the cash to the wallet) and marks the order completed.
     * If stock or cash is not enough the order stays pending.
     */
    public synchronized SaleTransaction fulfillOrder(User actor, Order order, double cashTendered)
            throws InsufficientStockException {
    	
        requireStaff(actor, "fulfil orders");
        requirePending(order);
        
        SaleTransaction sale = recordSale(actor, order.getCustomerName(), order.getCustomerUsername(),
                order.getLines(), cashTendered);
        order.complete(actor.getUsername(), sale.getId());
        save();
        return sale;
    }

    //Staff can cancel any pending order; a customer can cancel only their own.
    public synchronized void cancelOrder(User actor, Order order) {
        if (actor == null) 
        	throw new SecurityException("Nobody is logged in.");
        
        if (!actor.isStaff() && !actor.getUsername().equals(order.getCustomerUsername()))
            throw new SecurityException("You can only cancel your own orders.");
        
        requirePending(order);
        order.cancel(actor.getUsername());
        save();
    }

    private void requirePending(Order order) {
        if (order.getStatus() != Order.Status.PENDING)
            throw new IllegalStateException("Order " + order.getId() + " is already "
                    + order.getStatus().getLabel().toLowerCase() + ".");
    }

    //Cash currently in the wallet: sales + cash donations, excluding voided ones. 
    public double getWalletBalance() {
        return store.transactions.stream().filter(t -> !t.isVoided()).mapToDouble(Transaction::getCashAmount).sum();
    }
}
