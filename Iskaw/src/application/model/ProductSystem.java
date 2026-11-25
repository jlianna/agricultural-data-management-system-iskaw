package application.model;

import application.storage.CharityDispatchFileReader;
import application.storage.CharityDispatchFileWriter;
import application.storage.CharityDonationFileReader;
import application.storage.CharityDonationFileWriter;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Central facade managing all business logic, role permissions, and file persistence for Iskaw.
public class ProductSystem {

    // In-memory state container holding all application entities and ID counters.
    static class Store {
        // initialize
        final List<User> users = new ArrayList<>();
        final List<Product> products = new ArrayList<>();
        final Inventory inventory = new Inventory();
        final List<Transaction> transactions = new ArrayList<>();
        int transactionCounter = 0;
        List<Order> orders = new ArrayList<>();
        int orderCounter = 0;
        final List<ItemDonation> charityDonations = new ArrayList<>();
        final List<CharityDispatch> charityDispatches = new ArrayList<>();
        int dispatchCounter = 0;
    }

    private final DataFiles files;
    private Store store;
    
    // Stores a recovery notice if corrupted data files were backed up during startup.
    private String startupWarning;

    // Initializes the system using the default "data/" directory.
    public ProductSystem() {
        this(Paths.get("data"));
    }

    // Initializes the system with a custom data directory and loads persisted records.
    public ProductSystem(Path dataDir) {
        this.files = new DataFiles(dataDir);
        load();
    }

    // Getter for any startup recovery warning message.
    public String getStartupWarning() { 
        return startupWarning; 
    }

    // ------------------------------------------------------------------ persistence

    // Loads existing records from disk, or seeds default data if missing or corrupted.
    private void load() {
        if (files.exists()) {
            try {
                store = loadFromFiles();
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

    // Reads all entity records from storage text files into a new Store instance.
    private Store loadFromFiles() throws IOException {
        Store s = new Store();
        s.users.addAll(UserFileReader.read(files.users()));
        s.products.addAll(ProductFileReader.read(files.products(), s.inventory));
        
        var lines = SaleLineFileReader.read(files.lines());
        
        s.transactions.addAll(TransactionFileReader.read(files.transactions(), lines));
        s.orders.addAll(OrderFileReader.read(files.orders(), lines));
        s.charityDonations.addAll(CharityDonationFileReader.read(files.charityDonations()));
        s.charityDispatches.addAll(CharityDispatchFileReader.read(files.charityDispatches()));

        // Migrate any existing ItemDonation records from transactions.txt into charityDonations
        Set<String> existingCharityIds = s.charityDonations.stream()
                .map(ItemDonation::getId)
                .collect(Collectors.toSet());
        for (Transaction t : s.transactions) {
            if (t instanceof ItemDonation && !existingCharityIds.contains(t.getId())) {
                s.charityDonations.add((ItemDonation) t);
            }
        }
        
        // Resume ID counters above the highest existing numeric ID suffix.
        List<String> allTxIds = new ArrayList<>();
        s.transactions.forEach(t -> allTxIds.add(t.getId()));
        s.charityDonations.forEach(d -> allTxIds.add(d.getId()));
        s.transactionCounter = highest(allTxIds);
        s.orderCounter = highest(s.orders.stream().map(Order::getId).collect(Collectors.toList()));
        s.dispatchCounter = highest(s.charityDispatches.stream().map(CharityDispatch::getId).collect(Collectors.toList()));
        return s;
    }

    // Extracts the highest numeric suffix from a list of prefixed IDs (e.g., "T-00017").
    private static int highest(List<String> ids) {
        int max = ids.size();
        for (String id : ids) {
            try {
                max = Math.max(max, Integer.parseInt(id.substring(id.indexOf('-') + 1)));
            } catch (RuntimeException ignored) { }
        }
        return max;
    }

    // Persists all in-memory records and line items to disk.
    public synchronized void save() {
        try {
            UserFileWriter.write(files.users(), store.users);
            ProductFileWriter.write(files.products(), store.products, store.inventory);
            TransactionFileWriter.write(files.transactions(), store.transactions);
            OrderFileWriter.write(files.orders(), store.orders);
            SaleLineFileWriter.write(files.lines(), store.transactions, store.orders);
            CharityDonationFileWriter.write(files.charityDonations(), store.charityDonations);
            CharityDispatchFileWriter.write(files.charityDispatches(), store.charityDispatches);
            java.nio.file.Files.createDirectories(files.images());
        } catch (IOException e) {
            throw new IllegalStateException("Could not save data: " + e.getMessage(), e);
        }
    }

    // Populates the store with default staff/customer accounts and initial product catalog.
    private void seed() {
        store.users.add(new Manager("manager", "Maria Santos", "manager123"));
        store.users.add(new Clerk("clerk1", "Juan Dela Cruz", "clerk123"));
        store.users.add(new Clerk("clerk2", "Ana Reyes", "clerk123"));
        store.users.add(new Customer("customer1", "Rosa Mendoza", "customer123"));

        // Seed dairy products
        addSeed("Fresh Cow's Milk", "Farm-fresh whole cow's milk, pasteurized.", "Milk", "1 L", 85, 55);
        addSeed("Chocolate Milk", "Low-sugar chocolate drink made with fresh milk.", "Milk", "250 ml", 35, 70);
        addSeed("Kesong Puti", "Soft, lightly salted white cheese made from cow's milk.", "Cheese", "200 g", 120, 18);
        addSeed("Mozzarella Cheese", "Stretchy mozzarella, great for pizza and baking.", "Cheese", "250 g", 210, 9);
        addSeed("Cream Cheese", "Smooth spreadable cream cheese.", "Cheese", "150 g", 130, 22);
        addSeed("Plain Yogurt", "Thick, unsweetened yogurt with live cultures.", "Yogurt", "500 g", 110, 30);
        addSeed("Strawberry Yogurt Drink", "Drinkable yogurt blended with real strawberries.", "Yogurt", "250 ml", 45, 6);
        addSeed("Salted Butter", "Churned butter from fresh cream, lightly salted.", "Butter & Cream", "200 g", 150, 14);
        addSeed("All-Purpose Cream", "Chilled table cream for desserts and sauces.", "Butter & Cream", "250 ml", 70, 0);
        addSeed("Ube Ice Cream", "Creamy purple-yam ice cream.", "Frozen", "1 L", 260, 12);
        addSeed("Pastillas de Leche", "Traditional milk candy, pack of 10 pieces.", "Sweets", "10 pcs", 60, 45);

        // Seed beef cuts
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

        // Seed cowhide products per color variant
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

    // Matches products without valid images to matching filenames in data/images/.
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

    // Helper to create and register a seeded product with initial stock.
    private void addSeed(String name, String desc, String category, String unit, double price, int stock) {
        Product p = new Product(newProductId(), name, desc, category, unit, price);
        store.products.add(p);
        store.inventory.setStock(p.getId(), stock);
    }

    // Generates a unique 8-character product ID prefixed with "P-".
    private String newProductId() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ------------------------------------------------------------------ users

    // Authenticates a user by username and password.
    public User login(String username, String password) {
        String key = username == null ? "" : username.trim().toLowerCase();
        return store.users.stream()
                .filter(u -> u.getUsername().equals(key) && u.checkPassword(password))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Incorrect username or password."));
    }

    // Getter for an unmodifiable list of all registered users.
    public List<User> getUsers() { 
        return Collections.unmodifiableList(store.users); 
    }

    // Getter for all clerk accounts.
    public List<Clerk> getClerks() {
        return store.users.stream().filter(u -> u instanceof Clerk).map(u -> (Clerk) u).collect(Collectors.toList());
    }

    // Looks up a user by exact username.
    public Optional<User> findUser(String username) {
        return store.users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    // Returns a user's full name, or falls back to their username if not found.
    public String displayName(String username) {
        return findUser(username).map(User::getFullName).orElse(username);
    }

    // Allows a manager to create and persist a new clerk account.
    public synchronized Clerk addClerk(User actor, String username, String fullName, String password) {
        requireManager(actor, "add staff accounts");
        
        if (findUser(username == null ? "" : username.trim().toLowerCase()).isPresent())
            throw new IllegalArgumentException("That username is already taken.");
        
        Clerk c = new Clerk(username, fullName, password);
        store.users.add(c);
        save();
        return c;
    }

    // Registers and persists a new customer account via self sign-up.
    public synchronized Customer registerCustomer(String username, String fullName, String password) {
        String key = username == null ? "" : username.trim().toLowerCase();
        if (findUser(key).isPresent()) throw new IllegalArgumentException("That username is already taken.");
        Customer c = new Customer(username, fullName, password);
        store.users.add(c);
        save();
        return c;
    }

    // Getter for all customer accounts sorted alphabetically by full name.
    public List<Customer> getCustomers() {
        return store.users.stream().filter(u -> u instanceof Customer).map(u -> (Customer) u)
                .sorted(Comparator.comparing(Customer::getFullName)).collect(Collectors.toList());
    }

    // Getter for all sale transactions linked to a specific customer, newest first.
    public List<SaleTransaction> getPurchasesOf(String username) {
        return getTransactions().stream()
                .filter(t -> t instanceof SaleTransaction && username.equals(((SaleTransaction) t).getCustomerUsername()))
                .map(t -> (SaleTransaction) t).collect(Collectors.toList());
    }

    // Ensures the acting user is a staff member (manager or clerk).
    private void requireStaff(User actor, String action) {
        if (actor == null || !actor.isStaff()) 
            throw new SecurityException("Only staff can " + action + ".");
    }

    // Ensures the acting user has manager privileges.
    private void requireManager(User actor, String action) {
        if (actor == null || !actor.isManager())
            throw new SecurityException("Only a manager can " + action + ".");
    }

    // ------------------------------------------------------------------ products & inventory

    // Getter for the system inventory tracker.
    public Inventory getInventory() { 
        return store.inventory; 
    }

    // Getter for all products sorted by category and name.
    public List<Product> getProducts() {
        List<Product> copy = new ArrayList<>(store.products);
        copy.sort(Comparator.comparing(Product::getCategory).thenComparing(Product::getName));
        return copy;
    }

    // Getter for all distinct product categories sorted alphabetically.
    public List<String> getCategories() {
        return store.products.stream().map(Product::getCategory).distinct().sorted().collect(Collectors.toList());
    }

    // Looks up a product by its unique ID.
    public Optional<Product> findProduct(String id) {
        return store.products.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    // Allows a manager to add a new product with initial stock and save to disk.
    public synchronized Product addProduct(User actor, String name, String description, String category,
                                           String unit, double price, int initialStock) {
        requireManager(actor, "add products");
        if (initialStock < 0) 
            throw new IllegalArgumentException("Initial stock cannot be negative.");
        Product p = new Product(newProductId(), name, description, category, unit, price);
        
        ensureUnique(p.getName(), p.getUnit(), null);
        
        store.products.add(p);
        store.inventory.setStock(p.getId(), initialStock);
        save();
        return p;
    }

    // Allows a manager to update an existing product's details after validating uniqueness.
    public synchronized void updateProduct(User actor, Product p, String name, String description,
                                           String category, String unit, double price) {
        requireManager(actor, "edit products");
        Product check = new Product(p.getId(), name, description, category, unit, price);
        ensureUnique(check.getName(), check.getUnit(), p);
        
        p.setName(name);
        p.setDescription(description);
        p.setCategory(category);
        p.setUnit(unit);
        p.setPrice(price);
        save();
    }

    // Allows a manager to delete a product, its inventory entry, and its image file.
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

    // Validates, copies, and attaches a product image file into data/images/.
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

    // Detaches and deletes a product's assigned image file.
    public synchronized void removeProductImage(User actor, Product p) {
        requireManager(actor, "change product photos");
        String old = p.getImageFile();
        p.setImageFile(null);
        
        deleteImageFile(old);
        save();
    }

    // Getter for the resolved path to a product's image file, or null if absent.
    public Path getImagePath(Product p) {
        if (p == null || !p.hasImage()) 
            return null;
        
        Path f = files.images().resolve(p.getImageFile());
        return Files.isRegularFile(f) ? f : null;
    }

    // Safely deletes an image file from data/images/ if it exists.
    private void deleteImageFile(String file) {
        if (file == null || file.isEmpty()) 
            return;
        try { 
            Files.deleteIfExists(files.images().resolve(file)); 
        } catch (IOException ignored) { }
    }

    // Verifies no other product shares the same name and unit combination.
    private void ensureUnique(String name, String unit, Product ignore) {
        boolean dup = store.products.stream().anyMatch(o -> o != ignore
                && o.getName().equalsIgnoreCase(name) && o.getUnit().equalsIgnoreCase(unit));
        if (dup) 
            throw new IllegalArgumentException("A product named \"" + name + "\" (" + unit + ") already exists.");
    }

    // Allows a manager to add stock units to a product and save changes.
    public synchronized void restock(User actor, Product p, int quantity) {
        requireManager(actor, "restock products");
        store.inventory.restock(p.getId(), quantity);
        save();
    }

    // Getter for products currently below the low-stock threshold.
    public List<Product> getLowStockProducts() {
        return getProducts().stream().filter(p -> store.inventory.isLow(p.getId())).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ transactions

    // Formats the next sequential transaction ID (e.g., "T-00001").
    private String nextTransactionId() {
        return String.format("T-%05d", store.transactionCounter + 1);
    }

    // Increments the transaction counter, appends the transaction, and saves to disk.
    private <T extends Transaction> T commit(T t) {
        store.transactionCounter++;
        store.transactions.add(t);
        save();
        return t;
    }

    // Records a walk-in sale transaction without a linked customer account.
    public synchronized SaleTransaction recordSale(User actor, String customer, List<SaleLine> lines,
                                                   double cashTendered) throws InsufficientStockException {
        return recordSale(actor, customer, null, lines, cashTendered);
    }

    // Records a sale transaction, deducts inventory stock, and optionally links it to a customer account.
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

    // Records a monetary donation and adds it to the cash balance.
    public synchronized CashDonation recordCashDonation(User actor, String donor, double amount) {
        requireStaff(actor, "record donations");
        return commit(new CashDonation(nextTransactionId(), actor.getUsername(), donor, amount));
    }

    // Records a custom in-kind donation earmarked for charity without modifying business stock.
    public synchronized ItemDonation recordItemDonation(User actor, String donor, String itemName,
                                                        int quantity, String notes) {
        requireStaff(actor, "record donations");
        ItemDonation donation = new ItemDonation(nextTransactionId(), actor.getUsername(), donor, itemName, quantity, notes);
        store.charityDonations.add(donation);
        store.transactionCounter++;
        store.transactions.add(donation);
        save();
        return donation;
    }

    // Getter for all donated charity items ordered newest first.
    public List<ItemDonation> getCharityDonations() {
        List<ItemDonation> copy = new ArrayList<>(store.charityDonations);
        Collections.reverse(copy);
        return copy;
    }

    // Allows a manager to dispatch selected donated items and quantities to a charity with notes.
    public synchronized CharityDispatch donateItemsToCharity(User actor, Map<ItemDonation, Integer> itemsAndQuantities,
                                                       String charityName, String notes) {
        requireManager(actor, "donate items to charity");
        if (itemsAndQuantities == null || itemsAndQuantities.isEmpty()) {
            throw new IllegalArgumentException("Select at least one donated item to send to charity.");
        }
        if (charityName == null || charityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter the recipient charity or organization.");
        }

        String noteEntry = "To " + charityName.trim()
                + ((notes != null && !notes.trim().isEmpty()) ? " (" + notes.trim() + ")" : "");

        Map<String, Integer> dispatchedSummary = new LinkedHashMap<>();

        for (Map.Entry<ItemDonation, Integer> entry : itemsAndQuantities.entrySet()) {
            ItemDonation item = entry.getKey();
            int qty = entry.getValue();
            if (item.isVoided()) {
                throw new IllegalArgumentException("Cannot donate voided item " + item.getId() + ".");
            }
            if (qty <= 0 || qty > item.getRemainingQuantity()) {
                throw new IllegalArgumentException("Invalid quantity for \"" + item.getItemName()
                        + "\" (available: " + item.getRemainingQuantity() + ").");
            }
            dispatchedSummary.merge(item.getItemName(), qty, Integer::sum);
        }

        for (Map.Entry<ItemDonation, Integer> entry : itemsAndQuantities.entrySet()) {
            entry.getKey().donateToCharity(entry.getValue(), noteEntry);
        }

        store.dispatchCounter++;
        String dispatchId = String.format("D-%05d", store.dispatchCounter);
        CharityDispatch dispatch = new CharityDispatch(dispatchId, actor.getUsername(), charityName, notes, dispatchedSummary);
        store.charityDispatches.add(dispatch);
        save();
        return dispatch;
    }

    // Getter for all charity dispatch records ordered newest first.
    public List<CharityDispatch> getCharityDispatches() {
        List<CharityDispatch> copy = new ArrayList<>(store.charityDispatches);
        Collections.reverse(copy);
        return copy;
    }

    // Voids a transaction, reverses its inventory adjustments, and saves changes.
    public synchronized void voidTransaction(User actor, Transaction t) throws InsufficientStockException {
        requireStaff(actor, "void transactions");
        t.voidBy(actor.getUsername(), store.inventory);
        save();
    }

    // Getter for all transactions ordered newest first.
    public List<Transaction> getTransactions() {
        List<Transaction> copy = new ArrayList<>(store.transactions);
        Collections.reverse(copy);
        return copy;
    }

    // Getter for transactions recorded by a specific staff username.
    public List<Transaction> getTransactionsBy(String username) {
        return getTransactions().stream().filter(t -> t.getRecordedBy().equals(username)).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ customer orders

    // Creates a pending customer order after verifying available stock.
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

    // Getter for all customer orders ordered newest first.
    public List<Order> getOrders() {
        List<Order> copy = new ArrayList<>(store.orders);
        Collections.reverse(copy);
        return copy;
    }

    // Getter for all orders placed by a specific customer username.
    public List<Order> getOrdersOf(String username) {
        return getOrders().stream().filter(o -> o.getCustomerUsername().equals(username)).collect(Collectors.toList());
    }

    // Getter for the total count of orders currently awaiting fulfillment.
    public long getPendingOrderCount() {
        return store.orders.stream().filter(o -> o.getStatus() == Order.Status.PENDING).count();
    }

    // Completes a pending order by recording a paid sale, deducting stock, and marking it fulfilled.
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

    // Cancels a pending order (allowed for staff or the customer who placed it).
    public synchronized void cancelOrder(User actor, Order order) {
        if (actor == null) 
            throw new SecurityException("Nobody is logged in.");
        
        if (!actor.isStaff() && !actor.getUsername().equals(order.getCustomerUsername()))
            throw new SecurityException("You can only cancel your own orders.");
        
        requirePending(order);
        order.cancel(actor.getUsername());
        save();
    }

    // Verifies that an order is still in PENDING status before fulfilling or canceling.
    private void requirePending(Order order) {
        if (order.getStatus() != Order.Status.PENDING)
            throw new IllegalStateException("Order " + order.getId() + " is already "
                    + order.getStatus().getLabel().toLowerCase() + ".");
    }

    // Getter for total net cash in the wallet from non-voided sales and cash donations.
    public double getWalletBalance() {
        return store.transactions.stream().filter(t -> !t.isVoided()).mapToDouble(Transaction::getCashAmount).sum();
    }
    
    // Deletes a clerk or customer account (managers cannot delete themselves or other managers).
    public synchronized void deleteUser(User actor, User target) {
        requireManager(actor, "delete accounts");
        if (target == null) {
            throw new IllegalArgumentException("Select an account to delete.");
        }
        if (target.getUsername().equals(actor.getUsername())) {
            throw new IllegalArgumentException("You cannot delete your own account while logged in.");
        }
        if (target.isManager()) {
            throw new IllegalArgumentException("Manager accounts cannot be deleted.");
        }

        store.users.remove(target);
        save();
    }
}