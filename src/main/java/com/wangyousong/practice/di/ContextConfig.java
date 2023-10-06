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

            @Override
            public <T> Optional<T> get(Type type) {
                return get(Ref.of(type));
            }

            @SuppressWarnings("unchecked")
            private <T> Optional<T> get(Ref ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<T>) Optional.ofNullable(providers.get(ref.getComponent()))
                            .map(provider -> (Provider<T>) () -> (T) provider.get(this));
                }
                return (Optional<T>) Optional.ofNullable(providers.get(ref.getComponent()))
                        .map(provider -> (Object) provider.get(this));
            }

            @SuppressWarnings("unchecked")
            private <T> Optional<T> getContainer(ParameterizedType type) {
                Ref ref = Ref.of(type);
                if (ref.getContainer() != Provider.class) return Optional.empty();
                return (Optional<T>) Optional.ofNullable(providers.get(ref.getComponent()))
                        .map(provider -> (Provider<T>) () -> (T) provider.get(this));
            }

            @SuppressWarnings("unchecked")
            private <T> Optional<T> getComponent(Class<T> type) {
                Ref ref = Ref.of(type);
                return Optional.ofNullable(providers.get(ref.getComponent())).map(provider -> (T) provider.get(this));
            }
        };
    }

    static class Ref {
        private Type container;
        private Class<?> component;

        Ref(ParameterizedType container) {
            this.container = container.getRawType();
            this.component = (Class<?>) container.getActualTypeArguments()[0];
        }

        Ref(Class<?> component) {
            this.component = component;
        }

        static <T> Ref of(Type type) {
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
        Class<?> componentType = dependency;
        if (!providers.containsKey(componentType)) throw new DependencyNotFoundException(component, componentType);
        if (visiting.contains(componentType)) throw new CyclicDependenciesFoundException(visiting);
        visiting.push(componentType);
        checkDependency(componentType, visiting);
        visiting.pop();
    }

    private void checkContainerTypeDependency(Class<?> component, Type dependency) {
        Class<Object> componentType = getComponentType(dependency);
        if (!providers.containsKey(componentType))
            throw new DependencyNotFoundException(component, componentType);
    }

    interface ComponentProvider<T> {
        T get(Context context);

        // Reference -> Provider<Service> | Service
        // List<Ref> getDependencies();
        default List<Type> getDependencies() {
            return of();
        }
    }

}
