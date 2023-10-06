package com.wangyousong.practice.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

class Ref {
    private Type container;
    private final Class<?> component;

    Ref(ParameterizedType container) {
        this.container = container.getRawType();
        this.component = (Class<?>) container.getActualTypeArguments()[0];
    }

    Ref(Class<?> component) {
        this.component = component;
    }

    public static <T> Ref of(Type type) {
        if (type instanceof ParameterizedType container) return new Ref(container);
        return new Ref((Class<?>) type);
    }

    public Type getContainer() {
        return container;
    }

    public Class<?> getComponent() {
        return component;
    }

    public boolean isContainer() {
        return Objects.nonNull(container);
    }
}
