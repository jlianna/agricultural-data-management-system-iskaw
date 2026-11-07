package application.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
public class DairySystem {

    /** Everything that is written to disk. */
    private static class Store implements Serializable {
        private static final long serialVersionUID = 1L;
        final List<User> users = new ArrayList<>();
        final List<Product> products = new ArrayList<>();
        final Inventory inventory = new Inventory();
        final List<Transaction> transactions = new ArrayList<>();
        int transactionCounter = 0;
    }

    private final Path dataFile;
    private Store store;
    /** Set when a corrupted save file had to be set aside during start-up. */
    private String startupWarning;

    public DairySystem() {
        this(Paths.get("data", "dairyhub.dat"));
    }

    public DairySystem(Path dataFile) {
        this.dataFile = dataFile;
        load();
    }

    public String getStartupWarning() { return startupWarning; }

    // ------------------------------------------------------------------ persistence

    private void load() {
        if (Files.exists(dataFile)) {
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(dataFile))) {
                store = (Store) in.readObject();
                return;
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                try { // keep the unreadable file so nothing is silently lost
                    Files.move(dataFile, dataFile.resolveSibling(dataFile.getFileName() + ".corrupt"),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) { }
                startupWarning = "The saved data could not be read, so sample data was loaded instead.";
            }
        }
        store = new Store();
        seed();
        save();
    }

    /** Writes all data to disk (via a temp file so a crash cannot leave a half-written save). */
    public synchronized void save() {
        try {
            Path parent = dataFile.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Path tmp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(store);
            }
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save data: " + e.getMessage(), e);
        }
    }

    private void seed() {
        store.users.add(new Manager("manager", "Maria Santos", "manager123"));
        store.users.add(new Clerk("clerk1", "Juan Dela Cruz", "clerk123"));
        store.users.add(new Clerk("clerk2", "Ana Reyes", "clerk123"));

        addSeed("Strawberry Milk", "Rich, creamy strawberry goodness, pasteurized and chilled.", "Milk", "1 L", 160, 40);
        addSeed("Fresh Cow's Milk", "Farm-fresh whole cow's milk, pasteurized.", "Milk", "1 L", 85, 55);
        addSeed("Chocolate Milk", "Low-sugar chocolate drink made with fresh milk.", "Milk", "250 ml", 35, 70);
        addSeed("Kesong Puti", "Soft, lightly salted white cheese made from carabao milk.", "Cheese", "200 g", 120, 18);
        addSeed("Mozzarella", "Stretchy mozzarella, great for pizza and baking.", "Cheese", "200 g", 210, 9);
        addSeed("Cheddar Cheese", "Sharp, tangy, savory cheese.", "Cheese", "200 g", 200, 22);
        addSeed("Plain Yogurt", "Thick, unsweetened yogurt with live cultures.", "Yogurt", "500 g", 110, 30);
        addSeed("Strawberry Yogurt", "Flavored togurt blended with real strawberries.", "Yogurt", "250 ml", 45, 6);
        addSeed("Salted Butter", "Churned butter from fresh cream, lightly salted.", "Cheese", "200 g", 170, 14);
        addSeed("Unsalted Butter", "Churned butter from fresh cream, unsalted.", "Cheese", "200 g", 160, 14);
        addSeed("Ricotta", "Milk, sweet, and milky cheese.", "Cheese", "200g", 150, 45);
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

    /** @throws IllegalArgumentException when the credentials are wrong */
    public User login(String username, String password) {
        String key = username == null ? "" : username.trim().toLowerCase();
        return store.users.stream()
                .filter(u -> u.getUsername().equals(key) && u.checkPassword(password))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Incorrect username or password."));
    }

    public List<User> getUsers() { return Collections.unmodifiableList(store.users); }

    public List<Clerk> getClerks() {
        return store.users.stream().filter(u -> u instanceof Clerk).map(u -> (Clerk) u).collect(Collectors.toList());
    }

    public Optional<User> findUser(String username) {
        return store.users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    /** Display name for a username (falls back to the username itself). */
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

    private void requireManager(User actor, String action) {
        if (actor == null || !actor.isManager())
            throw new SecurityException("Only a manager can " + action + ".");
    }

    // ------------------------------------------------------------------ products & inventory

    public Inventory getInventory() { return store.inventory; }

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
        if (initialStock < 0) throw new IllegalArgumentException("Initial stock cannot be negative.");
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
        save();
    }

    private void ensureUnique(String name, String unit, Product ignore) {
        boolean dup = store.products.stream().anyMatch(o -> o != ignore
                && o.getName().equalsIgnoreCase(name) && o.getUnit().equalsIgnoreCase(unit));
        if (dup) throw new IllegalArgumentException("A product named \"" + name + "\" (" + unit + ") already exists.");
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
        if (actor == null) throw new IllegalStateException("Nobody is logged in.");
        SaleTransaction t = new SaleTransaction(nextTransactionId(), actor.getUsername(), customer, lines, cashTendered);
        t.applyTo(store.inventory);
        return commit(t);
    }

    public synchronized CashDonation recordCashDonation(User actor, String donor, double amount) {
        if (actor == null) throw new IllegalStateException("Nobody is logged in.");
        return commit(new CashDonation(nextTransactionId(), actor.getUsername(), donor, amount));
    }

    public synchronized ItemDonation recordItemDonation(User actor, String donor, Product product, int quantity) {
        if (actor == null) throw new IllegalStateException("Nobody is logged in.");
        ItemDonation t = new ItemDonation(nextTransactionId(), actor.getUsername(), donor, product, quantity);
        t.applyTo(store.inventory);
        return commit(t);
    }

    /** Voids any recorded transaction and reverses its effect on inventory. */
    public synchronized void voidTransaction(User actor, Transaction t) throws InsufficientStockException {
        if (actor == null) throw new IllegalStateException("Nobody is logged in.");
        t.voidBy(actor.getUsername(), store.inventory);
        save();
    }

    /** Newest first. */
    public List<Transaction> getTransactions() {
        List<Transaction> copy = new ArrayList<>(store.transactions);
        Collections.reverse(copy);
        return copy;
    }

    public List<Transaction> getTransactionsBy(String username) {
        return getTransactions().stream().filter(t -> t.getRecordedBy().equals(username)).collect(Collectors.toList());
    }

    /** Cash currently in the wallet: sales + cash donations, excluding voided ones. */
    public double getWalletBalance() {
        return store.transactions.stream().filter(t -> !t.isVoided()).mapToDouble(Transaction::getCashAmount).sum();
    }
}
