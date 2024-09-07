package com.wizzardo.vulkan.ui.imgui.memo;

public class IntMemo<T> {
    protected T value;
    protected int arg = Integer.MIN_VALUE;
    protected final Mapper<T> mapper;

    public IntMemo(Mapper<T> mapper) {
        this.mapper = mapper;
    }

    public interface Mapper<T> {
        T map(int arg);
    }

    public T get(int arg) {
        if (arg != this.arg) {
            this.arg = arg;
            value = mapper.map(arg);
        }

        return value;
    }
}
