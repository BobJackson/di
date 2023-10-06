package com.wangyousong.practice.di;

public class DependencyNotFoundException extends RuntimeException {
    private final Component component;
    private final Component dependency;

    public DependencyNotFoundException(Component component, Component dependency) {
        this.component = component;
        this.dependency = dependency;
    }

    public Component getComponent() {
        return component;
    }

    public Component getDependency() {
        return dependency;
    }
}
