package com.wangyousong.practice.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private final Injectable<Constructor<T>> injectConstructor;
    private final List<Injectable<Method>> injectMethods;
    private final List<Injectable<Field>> injectFields;


    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw new IllegalComponentException();

        this.injectConstructor = Injectable.of(getInjectConstructor(component));
        this.injectMethods = getInjectMethods(component).stream().map(Injectable::of).toList();
        this.injectFields = getInjectFields(component).stream().map(Injectable::of).toList();

        if (injectFields.stream().map(Injectable::element).anyMatch(f -> Modifier.isFinal(f.getModifiers())))
            throw new IllegalComponentException();
        if (injectMethods.stream().map(Injectable::element).anyMatch(m -> m.getTypeParameters().length != 0))
            throw new IllegalComponentException();
    }

    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructor.element.newInstance(injectConstructor.toDependencies(context));
            for (Injectable<Field> field : injectFields)
                field.element().set(instance, field.toDependencies(context)[0]);
            for (Injectable<Method> method : injectMethods)
                method.element().invoke(instance, method.toDependencies(context));
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return concat(
                concat(stream(injectConstructor.required()),
                        injectFields.stream().flatMap(f -> stream(f.required()))),
                injectMethods.stream().flatMap(m -> stream(m.required()))
        ).toList();
    }

    record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {
        private static <Element extends Executable> Injectable<Element> of(Element element) {
            return new Injectable<>(element, stream(element.getParameters()).map(InjectionProvider::toComponentRef).toArray(ComponentRef<?>[]::new));
        }

        static Injectable<Field> of(Field field) {
            return new Injectable<>(field, new ComponentRef<?>[]{toComponentRef(field)});
        }

        Object[] toDependencies(Context context) {
            return stream(required).map(context::get).map(Optional::get).toArray();
        }
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(methods, m))
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList());
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        return traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList());
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();
        return (Constructor<T>) injectConstructors
                .stream()
                .findFirst()
                .orElseGet(() -> defaultConstructor(implementation));
    }

    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> members = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            members.addAll(finder.apply(members, current));
            current = current.getSuperclass();
        }
        return members;
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] annotatedElements) {
        return stream(annotatedElements).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverride(Method m, Method o) {
        return o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static boolean isOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods())
                .filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                .noneMatch(o -> isOverride(m, o));
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameters())
                .map(p -> toDependency(context, toComponentRef(p)))
                .toArray(Object[]::new);
    }

    private static Object toDependency(Context context, Field field) {
        return toDependency(context, toComponentRef(field));
    }

    private static Object toDependency(Context context, ComponentRef of) {
        return context.get(of).get();
    }

    private static ComponentRef toComponentRef(Field field) {
        Annotation qualifier = getQualifier(field);
        return ComponentRef.of(field.getGenericType(), qualifier);
    }

    private static ComponentRef<?> toComponentRef(Parameter parameter) {
        return ComponentRef.of(parameter.getParameterizedType(), getQualifier(parameter));
    }

    private static Annotation getQualifier(AnnotatedElement element) {
        List<Annotation> qualifiers = stream(element.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .toList();
        if (qualifiers.size() > 1) throw new IllegalComponentException();
        return qualifiers.stream().findFirst().orElse(null);
    }
}
