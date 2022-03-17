package com.wizzardo.vulkan.ui.javafx;

import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.HostInterface;

class JavaFxHostInterface implements HostInterface {

    private final JavaFxToTextureBridge bridge;

    public JavaFxHostInterface(JavaFxToTextureBridge javaFxToTextureBridge) {
        this.bridge = javaFxToTextureBridge;
    }

    @Override
    public void setEmbeddedStage(EmbeddedStageInterface embeddedStage) {
        System.out.println("setEmbeddedStage: " + embeddedStage);

        bridge.setEmbeddedStage(embeddedStage);

        if (embeddedStage == null) {
            return;
        }

        final int width = bridge.getTextureWidth();
        final int height = bridge.getTextureHeight();

        if (width > 0 && height > 0) {
            embeddedStage.setSize(width, height);
        }

        embeddedStage.setFocused(true, AbstractEvents.FOCUSEVENT_ACTIVATED);
    }

    @Override
    public void setEmbeddedScene(EmbeddedSceneInterface embeddedScene) {
        System.out.println("setEmbeddedScene: " + embeddedScene);

        bridge.setEmbeddedScene(embeddedScene);

        if (embeddedScene == null) {
            return;
        }


        embeddedScene.setPixelScaleFactors(1f, 1f);

        int width = bridge.getTextureWidth();
        int height = bridge.getTextureHeight();

        if (width > 0 && height > 0) {
            embeddedScene.setSize(width, height);
        }

//        embeddedScene.setDragStartListener(new JmeFxDnDHandler(bridge));
    }

    @Override
    public boolean requestFocus() {
        System.out.println("requestFocus");
        return false;
    }

    @Override
    public boolean traverseFocusOut(boolean forward) {
        System.out.println("traverseFocusOut: " + forward);
        return false;
    }

    @Override
    public void repaint() {
//        System.out.println("repaint");
        bridge.repaint();
    }

    @Override
    public void setPreferredSize(int width, int height) {
        System.out.println("setPreferredSize: " + width + "x" + height);
    }

    @Override
    public void setEnabled(boolean enabled) {
        System.out.println("setEnabled: " + enabled);
    }

    @Override
    public void setCursor(CursorFrame cursorFrame) {
//        System.out.println("setCursor: " + cursorFrame);
    }

    @Override
    public boolean grabFocus() {
        System.out.println("grabFocus");
        return false;
    }

    @Override
    public void ungrabFocus() {
        System.out.println("grabFocus");
    }
}
