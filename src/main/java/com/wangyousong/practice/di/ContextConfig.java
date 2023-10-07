package com.wangyousong.practice.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.Arrays.stream;
import static java.util.List.of;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        components.put(new Component(type, null), (ComponentProvider<T>) context -> instance);
    }

    public <T> void bind(Class<T> type, T instance, Annotation... qualifiers) {
        if (stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class)))
            throw new IllegalComponentException();
        for (Annotation qualifier : qualifiers)
            components.put(new Component(type, qualifier), context -> instance);
    }

    public <T, Implementation extends T> void bind(Class<T> type, Class<Implementation> implementation) {
        bind(type, implementation, type.getAnnotations());
    }

    public <T, Implementation extends T> void bind(Class<T> type, Class<Implementation> implementation, Annotation... annotations) {
        if (stream(annotations).map(Annotation::annotationType)
                .anyMatch(t -> !t.isAnnotationPresent(Qualifier.class) && !t.isAnnotationPresent(Scope.class)))
            throw new IllegalComponentException();
        List<Annotation> qualifiers = stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        Optional<Annotation> scope = stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();
        ComponentProvider<Implementation> injectionProvider = new InjectionProvider<>(implementation);
        ComponentProvider<Implementation> provider = scope.map(s -> (ComponentProvider<Implementation>) new SingletonProvider<>(injectionProvider)).orElse(injectionProvider);
        if (qualifiers.isEmpty()) components.put(new Component(type, null), provider);
        for (Annotation qualifier : qualifiers)
            components.put(new Component(type, qualifier), provider);
    }

    static class SingletonProvider<T> implements ComponentProvider<T> {
        private T singleton;
        private final ComponentProvider<T> provider;

        public SingletonProvider(ComponentProvider<T> provider) {
            this.provider = provider;
        }

        @Override
        public T get(Context context) {
            if (singleton == null) singleton = provider.get(context);
            return singleton;
        }
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

    private void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef<?> dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component()))
                throw new DependencyNotFoundException(component, dependency.component());
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component()))
                    throw new CyclicDependenciesFoundException(visiting);
                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }


    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef<?>> getDependencies() {
            return of();
        }
    }

}
