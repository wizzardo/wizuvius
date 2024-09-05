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

    void addWindowFocusListener(WindowFocusListener listener);

    void removeWindowFocusListener(WindowFocusListener listener);

    void setCursorVisible(boolean visible);

    float getWindowScaleY();

    float getWindowScaleX();

    interface MouseMoveListener {
        void onMouseMove(double x, double y);
    }

    interface ScrollListener {
        /**
         * @return true if app should continue processing listeners, false - to stop
         */
        boolean onScroll(double x, double y, double scrollX, double scrollY);
    }

    interface MouseButtonListener {
        /**
         * @return true if app should continue processing listeners, false - to stop
         */
        boolean onMouseButtonEvent(double x, double y, int button, boolean pressed);
    }

    interface KeyTypedListener {
        /**
         * @return true if app should continue processing listeners, false - to stop
         */
        boolean onChar(int codepoint, char[] chars);
    }

    interface KeyListener {
        /**
         * @return true if app should continue processing listeners, false - to stop
         */
        boolean onKey(int key, boolean pressed, boolean repeat);
    }

    interface WindowFocusListener {
        void onFocusChanged(boolean focused);
    }

    KeyState getKeyState();
}
