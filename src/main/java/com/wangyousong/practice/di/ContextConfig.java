package com.wangyousong.practice.di;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.util.*;

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
            @SuppressWarnings("unchecked")
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get(this));
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> Optional<T> get(ParameterizedType type) {
                if (type.getRawType() != Provider.class) return Optional.empty();
                Class<T> componentType = (Class<T>) type.getActualTypeArguments()[0];
                return (Optional<T>) Optional.ofNullable(providers.get(componentType))
                        .map(provider -> (Provider<T>) () -> (T) provider.get(this));
            }
        };
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
            if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
            if (visiting.contains(dependency)) throw new CyclicDependenciesFoundException(visiting);
            visiting.push(dependency);
            checkDependency(dependency, visiting);
            visiting.pop();
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Class<?>> getDependencies() {
            return List.of();
        }
    }

}
