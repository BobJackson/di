package com.wangyousong.practice.di;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.List.of;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (ComponentProvider<Type>) context -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependency(component, new Stack<>()));

        return new Context() {
            @Override
            public <T> Optional<T> getType(Type type) {
                if (type instanceof ParameterizedType) return get((ParameterizedType) type);
                return (Optional<T>) get((Class<?>) type);
            }

            @SuppressWarnings("unchecked")
            private <T> Optional<T> get(ParameterizedType type) {
                if (type.getRawType() != Provider.class) return Optional.empty();
                Class<T> componentType = (Class<T>) type.getActualTypeArguments()[0];
                return (Optional<T>) Optional.ofNullable(providers.get(componentType))
                        .map(provider -> (Provider<T>) () -> (T) provider.get(this));
            }

            @SuppressWarnings("unchecked")
            private <T> Optional<T> get(Class<T> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (T) provider.get(this));
            }
        };
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            if (dependency instanceof Class<?>)
                checkDependency(component, visiting, (Class<?>) dependency);
            if (dependency instanceof ParameterizedType) {
                Class<?> type = (Class<?>) ((ParameterizedType) dependency).getActualTypeArguments()[0];
                if (!providers.containsKey(type)) throw new DependencyNotFoundException(component, type);
            }
        }
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
        if (visiting.contains(dependency)) throw new CyclicDependenciesFoundException(visiting);
        visiting.push(dependency);
        checkDependency(dependency, visiting);
        visiting.pop();
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencies() {
            return of();
        }
    }

}
