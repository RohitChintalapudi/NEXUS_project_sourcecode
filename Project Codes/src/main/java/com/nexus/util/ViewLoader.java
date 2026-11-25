package com.nexus.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;
import java.net.URL;

public class ViewLoader {

    public static <T extends Node> LoadedView<T> load(String fxmlPath) {
        try {
            URL resource = ViewLoader.class.getResource(fxmlPath);
            if (resource == null) {
                throw new IllegalStateException("FXML resource not found: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(resource);
            T root = loader.load();
            Object controller = loader.getController();
            return new LoadedView<>(root, controller);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    public static class LoadedView<T extends Node> {
        private final T node;
        private final Object controller;

        public LoadedView(T node, Object controller) {
            this.node = node;
            this.controller = controller;
        }

        public T getNode() {
            return node;
        }

        @SuppressWarnings("unchecked")
        public <C> C getController() {
            return (C) controller;
        }
    }
}

