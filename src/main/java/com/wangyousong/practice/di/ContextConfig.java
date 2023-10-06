package com.wangyousong.practice.di;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.List.of;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, (ComponentProvider<T>) context -> instance);
    }

    public <T> void bind(Class<T> type, T instance, Annotation qualifier) {
        components.put(new Component(type, qualifier), context -> instance);
    }

    record Component(Class<?> type, Annotation qualifier) {

    }

    public <T, Implementation extends T> void bind(Class<T> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {
            @SuppressWarnings("unchecked")
            @Override
            public <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref) {
                if (ref.getQualifier() != null) {
                    return Optional.ofNullable(components.get(new Component(ref.getComponent(), ref.getQualifier())))
                            .map(provider -> (ComponentType) provider.get(this));
                }
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(providers.get(ref.getComponent()))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent()))
                        .map(provider -> (ComponentType) provider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Context.Ref<?> dependency : providers.get(component).getDependencies()) {
            if (!providers.containsKey(dependency.getComponent()))
                throw new DependencyNotFoundException(component, dependency.getComponent());
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponent())) throw new CyclicDependenciesFoundException(visiting);
                visiting.push(dependency.getComponent());
                checkDependencies(dependency.getComponent(), visiting);
                visiting.pop();
            }
        }
    }


    interface ComponentProvider<T> {
        T get(Context context);

        default List<Context.Ref> getDependencies() {
            return of();
        }
    }

}
