package com.wizzardo.vulkan.input;

import com.wizzardo.tools.cache.Cache;
import com.wizzardo.vulkan.DesktopVulkanApplication;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.system.MemoryStack.stackPush;

public class GlfwInputsManager implements InputsManager {
    protected DesktopVulkanApplication application;
    protected List<MouseMoveListener> mouseMoveListeners = new CopyOnWriteArrayList<>();
    protected List<MouseButtonListener> mouseButtonListeners = new CopyOnWriteArrayList<>();
    protected List<ScrollListener> scrollListeners = new CopyOnWriteArrayList<>();
    protected List<KeyTypedListener> keyTypedListeners = new CopyOnWriteArrayList<>();
    protected List<KeyListener> keyListeners = new CopyOnWriteArrayList<>();
    protected List<WindowFocusListener> windowFocusListenerListeners = new CopyOnWriteArrayList<>();
    protected KeyState keyState = new GlfwKeyState();
    protected double mousePositionX;
    protected double mousePositionY;
    protected float windowScaleX = 1;
    protected float windowScaleY = 1;
    protected Cache<Cursor, Long> cursors = new Cache<>(-1, cursor -> {
        if (application.isInMainThread()) {
            return glfwCreateStandardCursor(cursor.shape);
        } else {
            CompletableFuture<Long> future = new CompletableFuture<>();
            application.addTask(() -> future.complete(glfwCreateStandardCursor(cursor.shape)));
            return future.get();
        }
    });

    public enum Cursor {
        ARROW(GLFW_ARROW_CURSOR),
        IBEAM(GLFW_IBEAM_CURSOR),
        CROSSHAIR(GLFW_CROSSHAIR_CURSOR),
        POINTING_HAND(GLFW_POINTING_HAND_CURSOR),
        RESIZE_EW(GLFW_RESIZE_EW_CURSOR),
        RESIZE_NS(GLFW_RESIZE_NS_CURSOR),
        RESIZE_ALL(GLFW_RESIZE_ALL_CURSOR),
        ;

        final int shape;

        Cursor(int shape) {
            this.shape = shape;
        }
    }

    public GlfwInputsManager(DesktopVulkanApplication application) {
        this.application = application;

        application.addTask(() -> {

            try (MemoryStack stack = stackPush()) {
                FloatBuffer scaleX = stack.callocFloat(1);
                FloatBuffer scaleY = stack.callocFloat(1);
                glfwGetWindowContentScale(application.getWindow(), scaleX, scaleY);
                windowScaleX = scaleX.get();
                windowScaleY = scaleY.get();
            }

            glfwSetWindowFocusCallback(application.getWindow(), (window, focused) -> {
                if (focused) {
                    try (MemoryStack stack = stackPush()) {
                        DoubleBuffer posX = stack.callocDouble(1);
                        DoubleBuffer posY = stack.callocDouble(1);
                        glfwGetCursorPos(window, posX, posY);
                        mousePositionX = posX.get();
                        mousePositionY = posY.get();
                    }
                }
                try {
                    List<WindowFocusListener> list = this.windowFocusListenerListeners;
                    for (int i = 0; i < list.size(); i++) {
                        try {
                            list.get(i).onFocusChanged(focused);
                        } catch (IndexOutOfBoundsException ignored) {
                        }
                    }
                } catch (Exception e) {
                    application.logE(e::getMessage, e);
                }
            });

            glfwSetKeyCallback(application.getWindow(), (window, key, scancode, action, mods) -> {
//                System.out.println(key + " " + scancode + " " + action + " " + mods);
                if (key == -1)
                    return;

                boolean pressed = action == GLFW_PRESS;
                boolean repeat = action == GLFW_REPEAT;
                if (pressed || action == GLFW_RELEASE) {
                    keyState.setPressed(key, pressed);
                }

                try {
                    List<KeyListener> list = this.keyListeners;
                    for (int i = 0; i < list.size(); i++) {
                        try {
                            if(!list.get(i).onKey(key, pressed, repeat)){
                                break;
                            }
                        } catch (IndexOutOfBoundsException ignored) {
                        }
                    }
                } catch (Exception e) {
                    application.logE(e::getMessage, e);
                }
            });

            glfwSetCursorPosCallback(application.getWindow(), (window, xpos, ypos) -> {
                mousePositionX = xpos;
                mousePositionY = ypos;
                try {
                    List<MouseMoveListener> list = this.mouseMoveListeners;
                    for (int i = 0; i < list.size(); i++) {
                        try {
                            list.get(i).onMouseMove(xpos, ypos);
                        } catch (IndexOutOfBoundsException ignored) {
                        }
                    }
                } catch (Exception e) {
                    application.logE(e::getMessage, e);
                }
            });

            glfwSetScrollCallback(application.getWindow(), (window, xoffset, yoffset) -> {
                try {
                    List<ScrollListener> list = this.scrollListeners;
                    for (int i = 0; i < list.size(); i++) {
                        try {
                            if(!list.get(i).onScroll(mousePositionX, mousePositionY, xoffset, yoffset)){
                                break;
                            }
                        } catch (IndexOutOfBoundsException ignored) {
                        }
                    }
                } catch (Exception e) {
                    application.logE(e::getMessage, e);
                }
            });

            glfwSetMouseButtonCallback(application.getWindow(), (window, button, action, mods) -> {
                try {
                    if (action == GLFW_PRESS || action == GLFW_RELEASE) {
                        boolean pressed = action == GLFW_PRESS;
                        keyState.setMouseButtonPressed(button, pressed);

                        List<MouseButtonListener> list = this.mouseButtonListeners;
                        for (int i = 0; i < list.size(); i++) {
                            try {
                                if (!list.get(i).onMouseButtonEvent(mousePositionX, mousePositionY, button, pressed)) {
                                    break;
                                }
                            } catch (IndexOutOfBoundsException ignored) {
                            }
                        }
                    }
                } catch (Exception e) {
                    application.logE(e::getMessage, e);
                }
            });

            glfwSetCharCallback(application.getWindow(), (window, codepoint) -> {
                try {
                    List<KeyTypedListener> list = this.keyTypedListeners;
                    char[] chars = Character.toChars(codepoint);
                    for (int i = 0; i < list.size(); i++) {
                        try {
                            if (!list.get(i).onChar(codepoint, chars)) {
                                break;
                            }
                        } catch (IndexOutOfBoundsException ignored) {
                        }
                    }
                } catch (Exception e) {
                    application.logE(e::getMessage, e);
                }
            });
            glfwSetErrorCallback((error, description) -> {
                application.logE(() -> "GLFW.ERROR: " + error + " " + description, new RuntimeException("GLFW.ERROR: " + error + " " + description));
            });
        });
    }

    public void setCursor(Cursor cursor) {
        if (application.isInMainThread()) {
            Long c = cursors.get(cursor);
            if (c == null || c == 0L)
                return;
            glfwSetCursor(application.getWindow(), c);
        } else
            application.addTask(() -> {
                Long c = cursors.get(cursor);
                if (c == null || c == 0L)
                    return;
                glfwSetCursor(application.getWindow(), c);
            });
    }

    @Override
    public void addMouseMoveListener(MouseMoveListener listener) {
        mouseMoveListeners.add(listener);
    }

    @Override
    public void removeMouseMoveListener(MouseMoveListener listener) {
        mouseMoveListeners.remove(listener);
    }

    @Override
    public void addMouseButtonListener(MouseButtonListener listener) {
        mouseButtonListeners.add(listener);
    }

    @Override
    public void removeMouseClickListener(MouseButtonListener listener) {
        mouseButtonListeners.remove(listener);
    }

    @Override
    public void addScrollListener(ScrollListener listener) {
        scrollListeners.add(listener);
    }

    @Override
    public void removeScrollListener(ScrollListener listener) {
        scrollListeners.remove(listener);
    }

    @Override
    public void addKeyTypedListener(KeyTypedListener listener) {
        keyTypedListeners.add(listener);
    }

    @Override
    public void removeKeyTypedListener(KeyTypedListener listener) {
        keyTypedListeners.remove(listener);
    }

    @Override
    public void addKeyListener(KeyListener listener) {
        keyListeners.add(listener);
    }

    @Override
    public void removeKeyListener(KeyListener listener) {
        keyListeners.remove(listener);
    }

    @Override
    public void addWindowFocusListener(WindowFocusListener listener) {
        windowFocusListenerListeners.add(listener);
    }

    @Override
    public void removeWindowFocusListener(WindowFocusListener listener) {
        windowFocusListenerListeners.remove(listener);
    }

    @Override
    public void setCursorVisible(boolean visible) {
        application.addTask(() -> {
            if (visible)
                glfwSetInputMode(application.getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            else
                glfwSetInputMode(application.getWindow(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        });
    }

    @Override
    public KeyState getKeyState() {
        return keyState;
    }
}
