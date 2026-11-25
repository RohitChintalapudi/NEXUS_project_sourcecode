package com.nexus.service;

import com.nexus.model.NavItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Coordinates central workspace navigation across all application views.
 */
public class NavigationService {
    private static final NavigationService INSTANCE = new NavigationService();

    private final ObjectProperty<NavItem> currentNavItem = new SimpleObjectProperty<>(NavItem.HOME);
    private final List<Consumer<NavItem>> navigationListeners = new ArrayList<>();

    private NavigationService() {}

    public static NavigationService getInstance() {
        return INSTANCE;
    }

    public ObjectProperty<NavItem> currentNavItemProperty() {
        return currentNavItem;
    }

    public NavItem getCurrentNavItem() {
        return currentNavItem.get();
    }

    public void navigateTo(NavItem navItem) {
        if (navItem != null && currentNavItem.get() != navItem) {
            currentNavItem.set(navItem);
            for (Consumer<NavItem> listener : navigationListeners) {
                listener.accept(navItem);
            }
        }
    }

    public void addNavigationListener(Consumer<NavItem> listener) {
        if (!navigationListeners.contains(listener)) {
            navigationListeners.add(listener);
        }
    }

    public void removeNavigationListener(Consumer<NavItem> listener) {
        navigationListeners.remove(listener);
    }
}

