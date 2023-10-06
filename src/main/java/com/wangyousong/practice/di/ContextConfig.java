package com.wangyousong.practice.di;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.List.of;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        components.put(new Component(type, null), (ComponentProvider<T>) context -> instance);
    }

    public <T> void bind(Class<T> type, T instance, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers)
            components.put(new Component(type, qualifier), context -> instance);
    }

    public <T, Implementation extends T> void bind(Class<T> type, Class<Implementation> implementation) {
        components.put(new Component(type, null), new InjectionProvider<>(implementation));
    }

    public <T, Implementation extends T> void bind(Class<T> type, Class<Implementation> implementation, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers)
            components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {
            @SuppressWarnings("unchecked")
            @Override
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(getProvider(ref))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(getProvider(ref))
                        .map(provider -> (ComponentType) provider.get(this));
            }
        };
    }

    private <ComponentType> ComponentProvider<?> getProvider(ComponentRef<ComponentType> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Stack<Class<?>> visiting) {
        for (ComponentRef<?> dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component()))
                throw new DependencyNotFoundException(component.type(), dependency.getComponentType());
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponentType()))
                    throw new CyclicDependenciesFoundException(visiting);
                visiting.push(dependency.getComponentType());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }


    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef> getDependencies() {
            return of();
        }
    }

}
