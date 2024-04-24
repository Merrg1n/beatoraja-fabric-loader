package io.github.merrg1n.beatorajafabric.api;

import bms.player.beatoraja.MainController;
import net.fabricmc.loader.api.FabricLoader;

public class BeatorajaGame {
    private String versionString;

    private String newTitle;

    private String version;

    private MainController controller;

    private static final BeatorajaGame instance = new BeatorajaGame();

    public static BeatorajaGame getInstance() {
        return instance;
    }

    public MainController getController() {
        if (controller == null)
            throw new IllegalStateException("Cannot get MainController before it created.");
        return controller;
    }

    public void setController(MainController controller) {
        this.controller = controller;
        this.setVersionString(MainController.getVersion());
    }

    public boolean isGameStarted() {
        return controller != null;
    }

    public String getVersion() {
        return version;
    }

    public void setVersionString(String vstr) {
        this.version = vstr.split(" ")[1];
        this.versionString = vstr;
        this.newTitle = vstr + " with fabric";
    }

    public String getTitle() {
        return this.newTitle;
    }
}
