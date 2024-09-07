package com.wizzardo.vulkan.ui.imgui.memo;

public class Memo<T, A> {
    protected T value;
    protected A arg;
    protected final Mapper<T, A> mapper;

    public Memo(Mapper<T, A> mapper) {
        this.mapper = mapper;
    }

    public interface Mapper<T, A> {
        T map(A arg);
    }

    public T get(A arg) {
        if (!arg.equals(this.arg)) {
            this.arg = arg;
            value = mapper.map(arg);
            System.out.println("memo updated: " + arg + " != " + this.arg);
        }

        return value;
    }
}
