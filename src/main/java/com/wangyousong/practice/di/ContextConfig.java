package com.wangyousong.practice.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, new ComponentProvider<Type>() {
            @Override
            public Type get(Context context) {
                return instance;
            }

            @Override
            public List<Class<?>> getDependencies() {
                return List.of();
            }
        });
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new ConstructorInjectionProvider<>(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependency(component, new Stack<>()));

        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get(this));
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

        List<Class<?>> getDependencies();
    }

    static class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
        private final Constructor<T> injectConstructor;

        public ConstructorInjectionProvider(Class<T> component) {
            this.injectConstructor = getInjectConstructor(component);
        }

        private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
            List<Constructor<?>> injectConstructors = stream(implementation.getConstructors())
                    .filter(c -> c.isAnnotationPresent(Inject.class))
                    .toList();

            if (injectConstructors.size() > 1) throw new IllegalComponentException();

            return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
                try {
                    return implementation.getConstructor();
                } catch (NoSuchMethodException e) {
                    throw new IllegalComponentException();
                }
            });
        }

        @Override
        public T get(Context context) {
            try {
                Object[] dependencies = stream(injectConstructor.getParameters())
                        .map(p -> context.get(p.getType()).get())
                        .toArray(Object[]::new);
                return injectConstructor.newInstance(dependencies);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<Class<?>> getDependencies() {
            return stream(injectConstructor.getParameters()).map(Parameter::getType).collect(Collectors.toList());
        }
    }
}
