package com.wizzardo.vulkan.input;

import java.awt.event.KeyEvent;

public class GlfwKeyState extends KeyState {
    protected int[] awt2glfw = new int[65536];
    protected int[] glfw2awt = new int[512];

    {
        //todo init all keys
        awt2glfw[KeyEvent.VK_SHIFT] = GlfwKey.GLFW_KEY_LEFT_SHIFT;
        awt2glfw[KeyEvent.VK_CONTROL] = GlfwKey.GLFW_KEY_LEFT_CONTROL;
        awt2glfw[KeyEvent.VK_ALT] = GlfwKey.GLFW_KEY_LEFT_ALT;
        awt2glfw[KeyEvent.VK_META] = GlfwKey.GLFW_KEY_LEFT_SUPER;

        glfw2awt[GlfwKey.GLFW_KEY_ESCAPE] = KeyEvent.VK_ESCAPE;
        glfw2awt[GlfwKey.GLFW_KEY_1] = KeyEvent.VK_1;
        glfw2awt[GlfwKey.GLFW_KEY_2] = KeyEvent.VK_2;
        glfw2awt[GlfwKey.GLFW_KEY_3] = KeyEvent.VK_3;
        glfw2awt[GlfwKey.GLFW_KEY_4] = KeyEvent.VK_4;
        glfw2awt[GlfwKey.GLFW_KEY_5] = KeyEvent.VK_5;
        glfw2awt[GlfwKey.GLFW_KEY_6] = KeyEvent.VK_6;
        glfw2awt[GlfwKey.GLFW_KEY_7] = KeyEvent.VK_7;
        glfw2awt[GlfwKey.GLFW_KEY_8] = KeyEvent.VK_8;
        glfw2awt[GlfwKey.GLFW_KEY_9] = KeyEvent.VK_9;
        glfw2awt[GlfwKey.GLFW_KEY_0] = KeyEvent.VK_0;
        glfw2awt[GlfwKey.GLFW_KEY_MINUS] = KeyEvent.VK_MINUS;
        glfw2awt[GlfwKey.GLFW_KEY_EQUAL] = KeyEvent.VK_EQUALS;
        glfw2awt[GlfwKey.GLFW_KEY_BACKSPACE] = KeyEvent.VK_BACK_SPACE;
        glfw2awt[GlfwKey.GLFW_KEY_TAB] = KeyEvent.VK_TAB;
        glfw2awt[GlfwKey.GLFW_KEY_Q] = KeyEvent.VK_Q;
        glfw2awt[GlfwKey.GLFW_KEY_W] = KeyEvent.VK_W;
        glfw2awt[GlfwKey.GLFW_KEY_E] = KeyEvent.VK_E;
        glfw2awt[GlfwKey.GLFW_KEY_R] = KeyEvent.VK_R;
        glfw2awt[GlfwKey.GLFW_KEY_T] = KeyEvent.VK_T;
        glfw2awt[GlfwKey.GLFW_KEY_Y] = KeyEvent.VK_Y;
        glfw2awt[GlfwKey.GLFW_KEY_U] = KeyEvent.VK_U;
        glfw2awt[GlfwKey.GLFW_KEY_I] = KeyEvent.VK_I;
        glfw2awt[GlfwKey.GLFW_KEY_O] = KeyEvent.VK_O;
        glfw2awt[GlfwKey.GLFW_KEY_P] = KeyEvent.VK_P;
        glfw2awt[GlfwKey.GLFW_KEY_LEFT_BRACKET] = KeyEvent.VK_OPEN_BRACKET;
        glfw2awt[GlfwKey.GLFW_KEY_RIGHT_BRACKET] = KeyEvent.VK_CLOSE_BRACKET;
        glfw2awt[GlfwKey.GLFW_KEY_ENTER] = KeyEvent.VK_ENTER;
        glfw2awt[GlfwKey.GLFW_KEY_LEFT_CONTROL] = KeyEvent.VK_CONTROL;
        glfw2awt[GlfwKey.GLFW_KEY_A] = KeyEvent.VK_A;
        glfw2awt[GlfwKey.GLFW_KEY_S] = KeyEvent.VK_S;
        glfw2awt[GlfwKey.GLFW_KEY_D] = KeyEvent.VK_D;
        glfw2awt[GlfwKey.GLFW_KEY_F] = KeyEvent.VK_F;
        glfw2awt[GlfwKey.GLFW_KEY_G] = KeyEvent.VK_G;
        glfw2awt[GlfwKey.GLFW_KEY_H] = KeyEvent.VK_H;
        glfw2awt[GlfwKey.GLFW_KEY_J] = KeyEvent.VK_J;
        glfw2awt[GlfwKey.GLFW_KEY_K] = KeyEvent.VK_K;
        glfw2awt[GlfwKey.GLFW_KEY_L] = KeyEvent.VK_L;
        glfw2awt[GlfwKey.GLFW_KEY_SEMICOLON] = KeyEvent.VK_SEMICOLON;
        glfw2awt[GlfwKey.GLFW_KEY_APOSTROPHE] = KeyEvent.VK_QUOTE;
        glfw2awt[GlfwKey.GLFW_KEY_GRAVE_ACCENT] = KeyEvent.VK_DEAD_GRAVE;
        glfw2awt[GlfwKey.GLFW_KEY_LEFT_SHIFT] = KeyEvent.VK_SHIFT;
        glfw2awt[GlfwKey.GLFW_KEY_BACKSLASH] = KeyEvent.VK_BACK_SLASH;
        glfw2awt[GlfwKey.GLFW_KEY_Z] = KeyEvent.VK_Z;
        glfw2awt[GlfwKey.GLFW_KEY_X] = KeyEvent.VK_X;
        glfw2awt[GlfwKey.GLFW_KEY_C] = KeyEvent.VK_C;
        glfw2awt[GlfwKey.GLFW_KEY_V] = KeyEvent.VK_V;
        glfw2awt[GlfwKey.GLFW_KEY_B] = KeyEvent.VK_B;
        glfw2awt[GlfwKey.GLFW_KEY_N] = KeyEvent.VK_N;
        glfw2awt[GlfwKey.GLFW_KEY_M] = KeyEvent.VK_M;
        glfw2awt[GlfwKey.GLFW_KEY_COMMA] = KeyEvent.VK_COMMA;
        glfw2awt[GlfwKey.GLFW_KEY_PERIOD] = KeyEvent.VK_PERIOD;
        glfw2awt[GlfwKey.GLFW_KEY_SLASH] = KeyEvent.VK_SLASH;
        glfw2awt[GlfwKey.GLFW_KEY_RIGHT_SHIFT] = KeyEvent.VK_SHIFT;
        glfw2awt[GlfwKey.GLFW_KEY_KP_MULTIPLY] = KeyEvent.VK_MULTIPLY;
        glfw2awt[GlfwKey.GLFW_KEY_SPACE] = KeyEvent.VK_SPACE;
        glfw2awt[GlfwKey.GLFW_KEY_CAPS_LOCK] = KeyEvent.VK_CAPS_LOCK;
        glfw2awt[GlfwKey.GLFW_KEY_F1] = KeyEvent.VK_F1;
        glfw2awt[GlfwKey.GLFW_KEY_F2] = KeyEvent.VK_F2;
        glfw2awt[GlfwKey.GLFW_KEY_F3] = KeyEvent.VK_F3;
        glfw2awt[GlfwKey.GLFW_KEY_F4] = KeyEvent.VK_F4;
        glfw2awt[GlfwKey.GLFW_KEY_F5] = KeyEvent.VK_F5;
        glfw2awt[GlfwKey.GLFW_KEY_F6] = KeyEvent.VK_F6;
        glfw2awt[GlfwKey.GLFW_KEY_F7] = KeyEvent.VK_F7;
        glfw2awt[GlfwKey.GLFW_KEY_F8] = KeyEvent.VK_F8;
        glfw2awt[GlfwKey.GLFW_KEY_F9] = KeyEvent.VK_F9;
        glfw2awt[GlfwKey.GLFW_KEY_F10] = KeyEvent.VK_F10;
        glfw2awt[GlfwKey.GLFW_KEY_NUM_LOCK] = KeyEvent.VK_NUM_LOCK;
        glfw2awt[GlfwKey.GLFW_KEY_SCROLL_LOCK] = KeyEvent.VK_SCROLL_LOCK;
        glfw2awt[GlfwKey.GLFW_KEY_KP_7] = KeyEvent.VK_NUMPAD7;
        glfw2awt[GlfwKey.GLFW_KEY_KP_8] = KeyEvent.VK_NUMPAD8;
        glfw2awt[GlfwKey.GLFW_KEY_KP_9] = KeyEvent.VK_NUMPAD9;
        glfw2awt[GlfwKey.GLFW_KEY_KP_SUBTRACT] = KeyEvent.VK_SUBTRACT;
        glfw2awt[GlfwKey.GLFW_KEY_KP_4] = KeyEvent.VK_NUMPAD4;
        glfw2awt[GlfwKey.GLFW_KEY_KP_5] = KeyEvent.VK_NUMPAD5;
        glfw2awt[GlfwKey.GLFW_KEY_KP_6] = KeyEvent.VK_NUMPAD6;
        glfw2awt[GlfwKey.GLFW_KEY_KP_ADD] = KeyEvent.VK_ADD;
        glfw2awt[GlfwKey.GLFW_KEY_KP_1] = KeyEvent.VK_NUMPAD1;
        glfw2awt[GlfwKey.GLFW_KEY_KP_2] = KeyEvent.VK_NUMPAD2;
        glfw2awt[GlfwKey.GLFW_KEY_KP_3] = KeyEvent.VK_NUMPAD3;
        glfw2awt[GlfwKey.GLFW_KEY_KP_0] = KeyEvent.VK_NUMPAD0;
        glfw2awt[GlfwKey.GLFW_KEY_KP_DECIMAL] = KeyEvent.VK_DECIMAL;
        glfw2awt[GlfwKey.GLFW_KEY_F11] = KeyEvent.VK_F11;
        glfw2awt[GlfwKey.GLFW_KEY_F12] = KeyEvent.VK_F12;
        glfw2awt[GlfwKey.GLFW_KEY_F13] = KeyEvent.VK_F13;
        glfw2awt[GlfwKey.GLFW_KEY_F14] = KeyEvent.VK_F14;
        glfw2awt[GlfwKey.GLFW_KEY_F15] = KeyEvent.VK_F15;
//        glfw2awt[GlfwKey.GLFW_KEY_KANA]= KeyEvent.VK_KANA;
//        glfw2awt[GlfwKey.GLFW_KEY_CONVERT]= KeyEvent.VK_CONVERT;
//        glfw2awt[GlfwKey.GLFW_KEY_NOCONVERT]= KeyEvent.VK_NONCONVERT;
        glfw2awt[GlfwKey.GLFW_KEY_KP_EQUAL] = KeyEvent.VK_EQUALS;
//        glfw2awt[GlfwKey.GLFW_KEY_]= KeyEvent.VK_CIRCUMFLEX;
//        glfw2awt[GlfwKey.GLFW_KEY_AT]= KeyEvent.VK_AT;
//        glfw2awt[GlfwKey.GLFW_KEY_CO]= KeyEvent.VK_COLON;
//        glfw2awt[GlfwKey.GLFW_KEY_UNDERLINE]= KeyEvent.VK_UNDERSCORE;
//        glfw2awt[GlfwKey.GLFW_KEY_STOP]= KeyEvent.VK_STOP;
        glfw2awt[GlfwKey.GLFW_KEY_KP_ENTER] = KeyEvent.VK_ENTER;
        glfw2awt[GlfwKey.GLFW_KEY_RIGHT_CONTROL] = KeyEvent.VK_CONTROL;
        glfw2awt[GlfwKey.GLFW_KEY_KP_DIVIDE] = KeyEvent.VK_DIVIDE;
        glfw2awt[GlfwKey.GLFW_KEY_PAUSE] = KeyEvent.VK_PAUSE;
        glfw2awt[GlfwKey.GLFW_KEY_HOME] = KeyEvent.VK_HOME;
        glfw2awt[GlfwKey.GLFW_KEY_UP] = KeyEvent.VK_UP;
        glfw2awt[GlfwKey.GLFW_KEY_PAGE_UP] = KeyEvent.VK_PAGE_UP;
        glfw2awt[GlfwKey.GLFW_KEY_LEFT] = KeyEvent.VK_LEFT;
        glfw2awt[GlfwKey.GLFW_KEY_RIGHT] = KeyEvent.VK_RIGHT;
        glfw2awt[GlfwKey.GLFW_KEY_END] = KeyEvent.VK_END;
        glfw2awt[GlfwKey.GLFW_KEY_DOWN] = KeyEvent.VK_DOWN;
        glfw2awt[GlfwKey.GLFW_KEY_PAGE_DOWN] = KeyEvent.VK_PAGE_DOWN;
        glfw2awt[GlfwKey.GLFW_KEY_INSERT] = KeyEvent.VK_INSERT;
        glfw2awt[GlfwKey.GLFW_KEY_DELETE] = KeyEvent.VK_DELETE;
        glfw2awt[GlfwKey.GLFW_KEY_LEFT_ALT] = KeyEvent.VK_ALT;
        glfw2awt[GlfwKey.GLFW_KEY_RIGHT_ALT] = KeyEvent.VK_ALT_GRAPH;
        glfw2awt[GlfwKey.GLFW_KEY_PRINT_SCREEN] = KeyEvent.VK_PRINTSCREEN;

        for (int key = 0; key < glfw2awt.length; key++) {
            if (glfw2awt[key] != 0)
                awt2glfw[glfw2awt[key]] = key;
        }

    }


    @Override
    public int awtToNative(int awtKey) {
        if (awtKey < awt2glfw.length) {
            int key = awt2glfw[awtKey];
            if (key != 0)
                return key;
        }
        return GlfwKey.GLFW_KEY_UNKNOWN;
    }

    @Override
    public int nativeToAwt(int nativeKey) {
        if (nativeKey < glfw2awt.length) {
            int key = glfw2awt[nativeKey];
            if (key != 0)
                return key;
        }
        return KeyEvent.VK_UNDEFINED;
    }

    @Override
    public boolean isShiftPressed() {
        return isKeyPressed(GlfwKey.GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GlfwKey.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public boolean isCtrlPressed() {
        return isKeyPressed(GlfwKey.GLFW_KEY_LEFT_CONTROL) || isKeyPressed(GlfwKey.GLFW_KEY_RIGHT_CONTROL);
    }

    @Override
    public boolean isAltPressed() {
        return isKeyPressed(GlfwKey.GLFW_KEY_LEFT_ALT) || isKeyPressed(GlfwKey.GLFW_KEY_RIGHT_ALT);
    }

    @Override
    public boolean isMetaPressed() {
        return isKeyPressed(GlfwKey.GLFW_KEY_LEFT_SUPER) || isKeyPressed(GlfwKey.GLFW_KEY_RIGHT_SUPER);
    }
}
