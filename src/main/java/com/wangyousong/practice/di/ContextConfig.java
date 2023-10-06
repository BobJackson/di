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
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

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

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            Ref ref = Ref.of(dependency);
            if (!providers.containsKey(ref.getComponent()))
                throw new DependencyNotFoundException(component, ref.getComponent());
            if (!ref.isContainer()) {
                if (visiting.contains(ref.getComponent())) throw new CyclicDependenciesFoundException(visiting);
                visiting.push(ref.getComponent());
                checkDependencies(ref.getComponent(), visiting);
                visiting.pop();
            }
        }
    }


    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencies() {
            return of();
        }
    }

}
