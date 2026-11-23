# Iskaw – Cattle, Dairy, Beef and Cowhide Products

JavaFX application for the **Iskaw** (a cattle-based business that sells dairy products, beef cuts, and cowhide products,
butter, ice cream and milk sweets). It covers the four required modules in one program:

| Module | Where |
|---|---|
| Sales management | **New Sale / Record Sale** screen (point-of-sale with cart, cash, change, receipt) |
| Inventory management | **Inventory** screen (stock levels, low/out-of-stock badges, manager restock) |
| Product management | **Products** screen (manager: add / edit / delete products) |
| Cash / donation transactions | **Cash & Transactions** screen (wallet balance, cash donations, item donations, history, void) |

## Run it in Eclipse (same setup as the lab template)

1. **File → Import → General → Existing Projects into Workspace**, pick this folder (or the zip), **Finish**.
2. The project uses the same `JavaFX21` user library as the lab template. If Eclipse shows red errors about
   `javafx`, open **Project → Properties → Java Build Path → Libraries** and make sure `JavaFX21` is present
   (Window → Preferences → Java → Build Path → User Libraries if it is missing).
3. Right-click `src/application/Main.java` → **Run As → Java Application**.
4. To rename the project to your team name: right-click it → **Refactor → Rename** (F2).

## Demo accounts

| Role | Username | Password |
|---|---|---|
| Manager | `manager` | `manager123` |
| Clerk | `clerk1` / `clerk2` | `clerk123` |
| Customer | `customer1` | `customer123` |

New customers can also sign up themselves with **"New customer? Create an account"** on the login screen.

## What each user can do

**Manager** – full read/write: add/edit/delete products, restock, record sales and donations, void transactions,
create clerk accounts (**Accounts** screen), see the transaction history, monitor every clerk's sale history on the Manager Dashboard
and open **each clerk's own dashboard**.

**Clerk** – record customer purchases, check inventory, record cash and item donations, and void any recorded
transaction (stock and wallet are corrected automatically). When selling, a clerk can link the sale to a registered customer.
Clerks cannot add products, restock or manage accounts.

**Customer** – signs up on the login screen, then can:
* browse the **Shop** (price and availability for every product), add items to a **cart** and **place an order**;
* follow and cancel their orders under **My Orders**;
* see their **Customer Dashboard**: total spent, purchases, items bought, pending orders, favourite products and
  purchase history with receipts.

Customers cannot record transactions or change any data. Payment is cash at pick-up: a placed order does not change
stock or the wallet. When the customer collects it, a clerk (or the manager) opens **Customer Orders** and fulfils it,
which records a normal sale linked to the customer (stock goes down, the wallet goes up, the purchase appears on the
customer's dashboard). Staff can also cancel pending orders; fulfilling fails safely (the order stays pending) if the
stock has run out in the meantime.

## Persistence

All data is saved automatically after every change to `data/dairyhub.dat` (created next to where the program is
run). Delete that file to reset to the sample data. If the file is ever unreadable it is renamed
`dairyhub.dat.corrupt` and sample data is loaded instead.

## Project structure & OOP design

```
application/Main.java           JavaFX entry point
application/model/                    Business logic (no JavaFX)
  DairySystem                   "System" class – users, products, transactions, permissions, persistence
  User  (abstract) ─ Manager, Clerk, Customer   inheritance + polymorphic permissions (isManager, isStaff, getRole)
  Product, Inventory, SaleLine
  Order                         customer order (pending / completed / cancelled), turned into a sale on pick-up
  Transaction (abstract) ─ SaleTransaction, CashDonation, ItemDonation
                                polymorphism: each subtype defines its own cash amount and inventory effect
  Dashboard                     read-only summaries for one user or for everyone
  InsufficientStockException    custom checked exception
application/ui/                       JavaFX screens
  SceneManager                  window switching (Login <-> Shell)
  Shell (+ Navigator)           sidebar frame; swaps View screens; menu depends on role (manager / clerk / customer)
  View (abstract) ─ ClerkDashboardView, ManagerDashboardView, CustomerDashboardView,
                    ShopView, CustomerOrdersView, StaffOrdersView, SalesView, InventoryView,
                    ProductsView, WalletView, StaffView (Accounts)
  Reusable components           StatCard, TransactionTable, LowStockPanel, FormDialog, Logo, Ui helpers
```

* **Encapsulation / validation:** model classes validate their own input and throw `IllegalArgumentException`;
  passwords are stored as SHA-256 hashes.
* **Error handling:** the UI never crashes on bad input – every failure (bad number, short cash, oversell,
  duplicate product, wrong password, unreadable save file) becomes a readable message.
* **Reusability:** `TransactionTable`, `StatCard`, `LowStockPanel`, `FormDialog` and the `Ui` helpers are shared by
  several screens; the clerk dashboard class is reused for the manager's view of a clerk.

## Product list (Milestone 1)

| Product | Size | Price (₱) | Description |
|---|---|---|---|
| Fresh Carabao Milk | 1 L | 95 | Rich, creamy whole carabao milk, pasteurized and chilled. |
| Fresh Cow's Milk | 1 L | 85 | Farm-fresh whole cow's milk, pasteurized. |
| Chocolate Milk | 250 ml | 35 | Low-sugar chocolate drink made with fresh milk. |
| Kesong Puti | 200 g | 120 | Soft, lightly salted white cheese made from carabao milk. |
| Carabao Mozzarella | 250 g | 210 | Stretchy mozzarella, great for pizza and baking. |
| Cream Cheese | 150 g | 130 | Smooth spreadable cream cheese. |
| Plain Yogurt | 500 g | 110 | Thick, unsweetened yogurt with live cultures. |
| Strawberry Yogurt Drink | 250 ml | 45 | Drinkable yogurt blended with real strawberries. |
| Salted Butter | 200 g | 150 | Churned butter from fresh cream, lightly salted. |
| All-Purpose Cream | 250 ml | 70 | Chilled table cream for desserts and sauces. |
| Ube Ice Cream | 1 L | 260 | Creamy purple-yam ice cream. |
| Pastillas de Leche | 10 pcs | 60 | Traditional milk candy, pack of 10 pieces. |

The cooperative and its products are fictional sample data for this project; replace the name, logo and product
list with your group's own business for Milestone 1 (the logo is drawn in `ui/Logo.java`).


## Iskaw product categories

The sample shop includes dairy products, beef cuts, and cowhide products. Beef prices are stored per 1 kg. Cowhide products are listed separately for Cognac, Dark Brown, and Dark Charcoal Gray so each color can have its own stock count. The cowhide prices are Full Hide ₱10,800, Half Hide ₱5,850, Quarter Hide ₱3,150, Coupon ₱1,500, Double Panel ₱650, and Panel ₱350.

The product banner and cow logo are temporary placeholders. Replace `src/application/images/iskaw-products-placeholder.png` and `ui/Logo.java` when the actual Iskaw product photos and logo are ready.
