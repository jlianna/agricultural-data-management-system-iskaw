package application.ui;

/** Lets a screen ask the shell to switch to another screen (window switching). */
public interface Navigator {
    /** Switch to a main screen by key: dashboard, sales, inventory, products, wallet, staff. */
    void go(String key);

    /** Show a one-off screen (e.g. a clerk's dashboard opened by the manager) under the given nav key. */
    void open(View view, String navKey);
}
