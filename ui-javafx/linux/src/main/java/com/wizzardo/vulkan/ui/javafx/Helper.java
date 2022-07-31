package com.wizzardo.vulkan.ui.javafx;
import com.sun.javafx.embed.EmbeddedSceneInterface;

public class Helper {
    public static void mouseEvent(
            EmbeddedSceneInterface sceneInterface,
            int x,
            int y,
            int screenX,
            int screenY,
            int type,
            int button,
            boolean primaryBtnDown,
            boolean middleBtnDown,
            boolean secondaryBtnDown,
            boolean shift,
            boolean ctrl,
            boolean alt,
            boolean meta
    ) {
        sceneInterface.mouseEvent(type, button, primaryBtnDown, middleBtnDown, secondaryBtnDown, false, false, x, y,
                screenX, screenY, shift, ctrl, alt, meta, false);
    }
}
