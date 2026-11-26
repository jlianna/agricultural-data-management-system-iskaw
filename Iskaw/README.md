# Iskaw — Agricultural Data Management System

**Iskaw** is a JavaFX desktop Point-of-Sale (POS), Inventory, Order Fulfillment, and Charity Donation Management System built for agricultural enterprises handling **Dairy**, **Beef**, and **Cowhide** products.

> **Important Repository Note:**  
> The active and complete application codebase is located exclusively on the **`demo-dairyhub`** branch inside the **`Iskaw/`** folder (`agricultural-data-management-system-iskaw/Iskaw`). Please ignore older prototype folders (`DairyHub`, `DemoPt2/DairyHub`, and `DemoPt2.zip`) when importing or running the project.

---

## Project Team

**University of the Philippines Los Baños (UPLB)**  
**Course & Section:** CMSC 22 – CD-4L

* **Julliana Imperial**
* **Pauleen Buenaventura**
* **Aaron Cohen Cale**

---

## Table of Contents
1. [Features Overview](#features-overview)
2. [How to Run in Eclipse](#how-to-run-in-eclipse)
3. [Default Login Credentials](#default-login-credentials)
4. [User Manual](#user-manual)
   * [Customer Guide](#1-customer-guide)
   * [Clerk Guide](#2-clerk-guide)
   * [Manager Guide](#3-manager-guide)
5. [Project Structure](#project-structure)

---

## Features Overview

* **Role-Based Access Control (RBAC):** Dedicated interfaces and permissions for **Customers**, **Clerks**, and **Managers**.
* **Point-of-Sale (POS) & Order Fulfillment:** Walk-in retail sales and customer pre-orders with automatic stock validation, cash change calculation, and receipt/transaction logging.
* **Inventory & Product Catalog Management:** Real-time stock tracking across Dairy, Beef, and Cowhide categories, low-stock alerts, product image uploads, and restocking controls.
* **Dual-Track Cash & Charity Management:**
  * **Cash Donations:** Added directly to the store's net wallet balance.
  * **In-Kind Charity Donations:** Stored in a separate charity pool (`charity_donations.txt`) so community donations never interfere with commercial stock, complete with a **Charity Dispatch** workflow and audit history.
* **Resilient Local Persistence:** Pipe-delimited flat-file storage (`Iskaw/data/`) with automatic backup recovery if data files are damaged.

---

## How to Run in Eclipse

### Prerequisites
* **Java Development Kit (JDK):** Java 11 or higher (Java 17 recommended).
* **IDE:** Eclipse IDE for Java Developers.
* **JavaFX SDK:** JavaFX SDK matching your JDK version (e.g., JavaFX 17 or 21), or the **e(fx)clipse** plugin installed in Eclipse.

### Step 1: Clone the Repository and Switch to `demo-dairyhub`
Open a terminal (or Git Bash) and clone the `demo-dairyhub` branch directly:

    git clone -b demo-dairyhub https://github.com/jlianna/agricultural-data-management-system-iskaw.git

*(If you already cloned the repository on another branch, switch to it using `git checkout demo-dairyhub`.)*

### Step 2: Import Only the `Iskaw` Folder into Eclipse
1. Open **Eclipse**.
2. Go to **File → Import... → General → Existing Projects into Workspace** and click **Next**.
3. Next to **Select root directory**, click **Browse...** and select the **`Iskaw`** subfolder inside the cloned repository (`.../agricultural-data-management-system-iskaw/Iskaw`).
   > **Note:** Make sure only **`Iskaw`** is checked under *Projects*—do **not** import `DairyHub` or `DemoPt2`.
4. Click **Finish**.

### Step 3: Configure JavaFX Build Path & VM Arguments
1. Right-click the **`Iskaw`** project in **Package Explorer** → **Build Path → Configure Build Path...**.
2. Under the **Libraries** tab, select **Modulepath** (or **Classpath**) and click **Add External JARs...** (or add your JavaFX User Library), then select all `.jar` files inside your JavaFX SDK's `lib/` folder (`javafx.controls.jar`, `javafx.fxml.jar`, `javafx.graphics.jar`, `javafx.base.jar`, etc.).
3. Click **Apply and Close**.
4. Right-click **`Main.java`** (inside `src/application/`) → **Run As → Run Configurations...**.
5. Select your Java Application configuration for `Main`, open the **Arguments** tab, and paste the following into **VM arguments** (replacing `/path/to/javafx-sdk/lib` with your actual JavaFX `lib` folder path):

       --module-path "/path/to/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics

6. Ensure the **Working directory** under the **Arguments** tab is set to **Default** (`${workspace_loc:Iskaw}`) so the app loads `Iskaw/data/` properly.
7. Click **Apply** and **Run**.

---

## Default Login Credentials

When running with the seeded sample data, use any of the following accounts on the login screen:

| Role | Username | Password |
| :--- | :--- | :--- |
| **Manager** | `manager` | `manager123` |
| **Clerk 1** | `clerk1` | `clerk123` |
| **Clerk 2** | `clerk2` | `clerk123` |
| **Customer** | `customer1` | `customer123` |

*(New customers can also register their own accounts directly from the **Create an account** option on the login screen.)*

---

## User Manual

### 1. Customer Guide
Customers have access to **Dashboard**, **Shop**, and **My Orders**:
* **Dashboard:** View a summary of your pending orders, completed purchases, and quick shortcuts to the shop.
* **Shop:**
  * Filter products by category (**Milk**, **Cheese**, **Yogurt**, **Butter & Cream**, **Frozen**, **Sweets**, **Beef**, **Cowhide**) or search by product name.
  * Enter the desired quantity for in-stock items and add them to your cart.
  * Click **Place Order** to submit a pre-order for staff fulfillment.
* **My Orders:** Track the real-time status of your orders (`Pending`, `Completed`, or `Cancelled`). You may cancel an order while it is still `Pending`.

---

### 2. Clerk Guide
Clerks handle front-counter operations across **Dashboard**, **New Sale**, **Customer Orders**, **Inventory**, and **Cash & Transactions**:
* **Dashboard:** View your personal shift sales summary, pending customer orders count, and low-stock alerts.
* **New Sale (POS):**
  * Select products and quantities to add to the transaction cart.
  * Optionally link the sale to a registered customer account or record it as a walk-in customer.
  * Enter the **Cash Tendered** amount to automatically compute change and complete the sale (deducting stock immediately).
* **Customer Orders:**
  * Review incoming `Pending` orders placed by customers.
  * Click **Fulfill Order**, enter cash tendered, and confirm to convert the order into a completed sale and deduct inventory.
* **Inventory:** View live stock levels and identify items running low on stock.
* **Cash & Transactions:**
  * View the store's net cash balance and full transaction ledger.
  * Click **Record Cash Donation** to log monetary donations into the store wallet.
  * Click **Record Item Donation** to log custom in-kind donations (donor name, item name, quantity, and notes) earmarked for charity without affecting store stock.
  * Void erroneous transactions if needed (automatically restoring deducted product stock).

---

### 3. Manager Guide
Managers have full access to all Clerk screens plus administrative control over **Products**, **Charity Donations**, and **Accounts**:
* **Dashboard:** Monitor overall store revenue, net wallet balance, low-stock warnings, pending orders, and individual clerk performance.
* **Products:**
  * **Add / Edit Products:** Set product names, descriptions, categories, units (e.g., `1 L`, `1 kg`, `250 g`), prices, and initial stock.
  * **Product Photos:** Upload (`PNG`, `JPG`, `GIF`, `BMP` up to 8 MB) or remove product images displayed in the shop and catalog.
  * **Restock / Delete:** Add new stock shipments to existing items or remove discontinued products.
* **Charity Donations:**
  * View all in-kind items donated by the community in the **Donated Items Pool** alongside remaining available quantities.
  * Click **Donate items to charity**, check the items to dispatch, specify the quantity for each item, enter the **Charity / Recipient** name and notes, and confirm.
  * Review all past dispatches in the **Charity Dispatch History** table below.
* **Accounts:**
  * Create new **Clerk** accounts using the right-hand registration form.
  * Select a clerk or customer row in the table and click **Delete selected account** to remove inactive accounts (manager accounts are protected against accidental deletion).

---

## Project Structure

    agricultural-data-management-system-iskaw/   (Branch: demo-dairyhub)
    └── Iskaw/
        ├── src/
        │   └── application/
        │       ├── Main.java                 # JavaFX application entry point
        │       ├── style.css                 # Application theme and button styles
        │       ├── model/                    # Domain models (ProductSystem, User, Product, Transaction, ItemDonation, CharityDispatch, Order, Inventory)
        │       ├── storage/                  # Pipe-delimited file readers/writers & DataFiles manager
        │       └── ui/                       # JavaFX views (Shell, LoginView, SalesView, ShopView, InventoryView, ProductsView, WalletView, CharityView, StaffView)
        └── data/                             # Persisted text records & product images (auto-seeded if missing)
            ├── users.txt
            ├── products.txt
            ├── transactions.txt
            ├── orders.txt
            ├── lines.txt
            ├── charity_donations.txt
            ├── charity_dispatches.txt
            └── images/