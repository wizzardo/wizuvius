package com.wizzardo.vulkan.input;

public interface InputsManager {

    void addMouseMoveListener(MouseMoveListener listener);

    void removeMouseMoveListener(MouseMoveListener listener);

    void addMouseButtonListener(MouseButtonListener listener);

    void removeMouseClickListener(MouseButtonListener listener);

    void addScrollListener(ScrollListener listener);

    void removeScrollListener(ScrollListener listener);

    void addKeyTypedListener(KeyTypedListener listener);

    void removeKeyTypedListener(KeyTypedListener listener);

    void addKeyListener(KeyListener listener);

    void removeKeyListener(KeyListener listener);

    void setCursorVisible(boolean visible);

    interface MouseMoveListener {
        void onMouseMove(double x, double y);
    }

    interface ScrollListener {
        void onScroll(double x, double y, double scrollX, double scrollY);
    }

    interface MouseButtonListener {
        void onMouseButtonEvent(double x, double y, int button, boolean pressed);
    }

    interface KeyTypedListener {
        void onChar(int codepoint, char[] chars);
    }

    interface KeyListener {
        void onKey(int key, boolean pressed, boolean repeat);
    }

    KeyState getKeyState();
}
