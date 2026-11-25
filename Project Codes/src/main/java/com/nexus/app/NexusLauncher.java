package com.nexus.app;

import javafx.application.Application;

/**
 * Standard JavaFX bootstrap entry point that does not extend Application directly,
 * preventing module path issues when running packaged shaded JARs.
 */
public class NexusLauncher {

    public static void main(String[] args) {
        Application.launch(NexusApplication.class, args);
    }
}

