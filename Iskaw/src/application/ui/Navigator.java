package application.ui;

// Interface allowing screens to request navigation or window switching from the main shell
public interface Navigator {
    // Switches to a primary screen identified by its navigation key (e.g., dashboard, sales, inventory, products, wallet, charity, staff)
    void go(String key);

    // Displays a temporary or one-off screen (such as a clerk's dashboard opened by a manager) highlighted under the given navigation key
    void open(View view, String navKey);
}