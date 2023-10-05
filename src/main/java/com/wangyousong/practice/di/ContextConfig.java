package com.wangyousong.practice.di;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.List.of;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, (ComponentProvider<T>) context -> instance);
    }

    public <T, Implementation extends T> void bind(Class<T> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependency(component, new Stack<>()));

        return new Context() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Optional<T> get(Type type) {
                if (isContainerType(type)) return getContainer((ParameterizedType) type);
                return (Optional<T>) getComponent((Class<?>) type);
            }

            @SuppressWarnings("unchecked")
            private <T> Optional<T> getContainer(ParameterizedType type) {
                if (type.getRawType() != Provider.class) return Optional.empty();
                return (Optional<T>) Optional.ofNullable(providers.get(getComponentType(type)))
                        .map(provider -> (Provider<T>) () -> (T) provider.get(this));
            }

            @SuppressWarnings("unchecked")
            private <T> Optional<T> getComponent(Class<T> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (T) provider.get(this));
            }
        };
    }

    private boolean isContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> getComponentType(Type type) {
        return (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            if (dependency instanceof Class<?>) checkComponentDependency(component, visiting, (Class<?>) dependency);
            else checkContainerTypeDependency(component, dependency);
        }
    }

    private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
        if (visiting.contains(dependency)) throw new CyclicDependenciesFoundException(visiting);
        visiting.push(dependency);
        checkDependency(dependency, visiting);
        visiting.pop();
    }

    private void checkContainerTypeDependency(Class<?> component, Type dependency) {
        if (!providers.containsKey(getComponentType(dependency)))
            throw new DependencyNotFoundException(component, getComponentType(dependency));
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencies() {
            return of();
        }
    }

}
