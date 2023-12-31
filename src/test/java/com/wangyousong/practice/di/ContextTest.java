package com.wangyousong.practice.di;

import com.wangyousong.practice.di.InjectionTest.ConstructorInjection.Injection.InjectConstructor;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ContextTest {

    ContextConfig config;
    Dependency dependency;
    TestComponent instance;

    @BeforeEach
    void setUp() {
        config = new ContextConfig();
        dependency = new Dependency() {
        };
        instance = new TestComponent() {
        };
    }

    @Nested
    class TypeBinding {

        @Test
        void should_bind_type_to_a_specific_instance() {
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();

            assertSame(instance, context.get(ComponentRef.of(TestComponent.class)).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        void should_bind_type_to_an_injectable_component(Class<? extends TestComponent> componentType) {
            config.bind(Dependency.class, dependency);
            config.bind(TestComponent.class, componentType);

            Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class))
            );
        }

        static class ConstructorInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements TestComponent {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements TestComponent {
            Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        @Test
        void should_return_empty_if_component_not_defined() {
            Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        @Test
        void should_retrieve_bind_type_as_provider() {
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();

            Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>() {
            }).get();
            assertSame(instance, provider.get());
        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();

            assertFalse(context.get(new ComponentRef<List<TestComponent>>() {
            }).isPresent());
        }

        @Nested
        public class WithQualifier {
            @Test
            void should_bind_instance_with_multi_qualifiers() {
                config.bind(TestComponent.class, instance, new Utils.NamedLiteral("ChosenOne"), new Utils.SkywalkerLiteral());

                Context context = config.getContext();
                TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, new Utils.NamedLiteral("ChosenOne"))).get();
                TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, new Utils.SkywalkerLiteral())).get();

                assertSame(instance, chosenOne);
                assertSame(instance, skywalker);
            }

            @Test
            void should_bind_component_with_multi_qualifiers() {
                config.bind(Dependency.class, dependency);
                config.bind(InjectConstructor.class,
                        InjectConstructor.class,
                        new Utils.NamedLiteral("ChosenOne"), new Utils.SkywalkerLiteral());

                Context context = config.getContext();
                InjectConstructor chosenOne = context.get(ComponentRef.of(InjectConstructor.class, new Utils.NamedLiteral("ChosenOne"))).get();
                InjectConstructor skywalker = context.get(ComponentRef.of(InjectConstructor.class, new Utils.SkywalkerLiteral())).get();

                assertSame(dependency, chosenOne.dependency);
                assertSame(dependency, skywalker.dependency);
            }

            @Test
            void should_retrieve_bind_type_as_provider() {
                config.bind(TestComponent.class, instance, new Utils.NamedLiteral("ChosenOne"), new Utils.SkywalkerLiteral());

                Optional<Provider<TestComponent>> provider = config.getContext().get(new ComponentRef<>(new Utils.SkywalkerLiteral()) {
                });

                assertTrue(provider.isPresent());
            }

            @Test
            void should_retrieve_empty_if_no_matched_qualifiers() {
                config.bind(TestComponent.class, instance);
                Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class, new Utils.SkywalkerLiteral()));

                assertTrue(component.isEmpty());
            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                TestComponent instance = new TestComponent() {
                };
                assertThrows(IllegalComponentException.class, () -> config.bind(TestComponent.class, instance, new Utils.TestLiteral()));
            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class, () -> config.bind(InjectConstructor.class, InjectConstructor.class, new Utils.TestLiteral()));
            }
        }

        @Nested
        public class WithScope {

            static class NotSingleton {

            }

            @Test
            void should_not_be_singleton_scope_by_default() {
                config.bind(NotSingleton.class, NotSingleton.class);
                Context context = config.getContext();

                assertNotSame(context.get(ComponentRef.of(NotSingleton.class)).get(), context.get(ComponentRef.of(NotSingleton.class)).get());
            }

            @Test
            void should_bind_component_as_singleton_scope() {
                config.bind(NotSingleton.class, NotSingleton.class, new SingletonLiteral());
                Context context = config.getContext();

                assertSame(context.get(ComponentRef.of(NotSingleton.class)).get(), context.get(ComponentRef.of(NotSingleton.class)).get());
            }

            @Singleton
            static class SingletonAnnotated implements Dependency {

            }

            @Test
            void should_retrieve_scope_annotation_from_component() {
                config.bind(Dependency.class, SingletonAnnotated.class);
                Context context = config.getContext();

                assertSame(context.get(ComponentRef.of(Dependency.class)).get(), context.get(ComponentRef.of(Dependency.class)).get());
            }

            @Test
            void should_bind_component_as_customized_scope() {
                config.scope(Pooled.class, PooledProvider::new);
                config.bind(NotSingleton.class, NotSingleton.class, new PooledLiteral());
                Context context = config.getContext();

                Set<NotSingleton> instances = IntStream.range(0, 5).mapToObj(i -> context.get(ComponentRef.of(NotSingleton.class)).get()).collect(Collectors.toSet());

                assertEquals(PooledProvider.MAX, instances.size());
            }

            @Test
            void should_throw_exception_if_multi_scope_provided() {
                assertThrows(IllegalComponentException.class, () -> config.bind(NotSingleton.class, NotSingleton.class, new SingletonLiteral(), new PooledLiteral()));
            }

            @Singleton
            @Pooled
            static class MultiScopeAnnotated {

            }

            @Test
            void should_throw_exception_if_multi_scope_annotated() {
                assertThrows(IllegalComponentException.class, () -> config.bind(MultiScopeAnnotated.class, MultiScopeAnnotated.class));
            }

            @Test
            void should_throw_exception_if_scope_undefined() {
                assertThrows(IllegalComponentException.class, () -> config.bind(NotSingleton.class, NotSingleton.class, new PooledLiteral()));
            }

            @Nested
            public class WithQualifier {
                @Test
                void should_not_be_singleton_scope_by_default() {
                    config.bind(NotSingleton.class, NotSingleton.class, new Utils.SkywalkerLiteral());
                    Context context = config.getContext();

                    assertNotSame(context.get(ComponentRef.of(NotSingleton.class, new Utils.SkywalkerLiteral())).get(), context.get(ComponentRef.of(NotSingleton.class, new Utils.SkywalkerLiteral())).get());
                }

                @Test
                void should_bind_component_as_singleton_scope() {
                    config.bind(NotSingleton.class, NotSingleton.class, new SingletonLiteral(), new Utils.SkywalkerLiteral());
                    Context context = config.getContext();

                    assertSame(context.get(ComponentRef.of(NotSingleton.class, new Utils.SkywalkerLiteral())).get(), context.get(ComponentRef.of(NotSingleton.class, new Utils.SkywalkerLiteral())).get());
                }

                @Test
                void should_retrieve_scope_annotation_from_component() {
                    config.bind(Dependency.class, SingletonAnnotated.class, new Utils.SkywalkerLiteral());
                    Context context = config.getContext();

                    assertSame(context.get(ComponentRef.of(Dependency.class, new Utils.SkywalkerLiteral())).get(), context.get(ComponentRef.of(Dependency.class, new Utils.SkywalkerLiteral())).get());
                }
            }
        }

    }

    @Nested
    public class DependencyCheck {
        @ParameterizedTest
        @MethodSource
        void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            config.bind(TestComponent.class, component);

            DependencyNotFoundException e = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

            assertEquals(Dependency.class, e.getDependency().type());
            assertEquals(TestComponent.class, e.getComponent().type());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Field Injection", MissingDependencyField.class)),
                    Arguments.of(Named.of("Method Injection", MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in Injection Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Injection Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in Injection Method", MissingDependencyProviderMethod.class)),
                    Arguments.of(Named.of("Scoped", MissingDependencyScoped.class)),
                    Arguments.of(Named.of("Scoped Provider", MissingDependencyProviderScoped.class))
            );
        }

        static class MissingDependencyConstructor implements TestComponent {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {

            }
        }

        static class MissingDependencyProviderField implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependency) {

            }
        }

        @Singleton
        static class MissingDependencyScoped implements TestComponent {
            @Inject
            Dependency dependency;
        }

        @Singleton
        static class MissingDependencyProviderScoped implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }


        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        void should_throw_exception_if_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                 Class<? extends Dependency> dependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);

            CyclicDependenciesFoundException e = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext().get(ComponentRef.of(TestComponent.class)));

            Set<Class<?>> classes = Set.of(e.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue(classes.contains(Dependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named<?> component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field ", CyclicComponentInjectField.class),
                    Named.of("Inject Method ", CyclicComponentInjectMethod.class)))
                for (Named<?> dependency : List.of(Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field ", CyclicDependencyInjectField.class),
                        Named.of("Inject Method ", CyclicDependencyInjectMethod.class)))
                    arguments.add(Arguments.of(component, dependency));
            return arguments.stream();
        }

        static class CyclicComponentInjectConstructor implements TestComponent {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class CyclicDependencyInjectField implements TestComponent {
            @Inject
            TestComponent component;
        }

        static class CyclicDependencyInjectMethod implements TestComponent {
            @Inject
            void install(TestComponent component) {
            }
        }

        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                            Class<? extends Dependency> dependency,
                                                                            Class<? extends AnotherDependency> anotherDependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);

            CyclicDependenciesFoundException e = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext().get(ComponentRef.of(TestComponent.class)));

            Set<Class<?>> classes = Set.of(e.getComponents());

            assertEquals(3, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue(classes.contains(Dependency.class));
            assertTrue(classes.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named<?> component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field ", CyclicComponentInjectField.class),
                    Named.of("Inject Method ", CyclicComponentInjectMethod.class)))
                for (Named<?> dependency : List.of(Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field ", IndirectCyclicDependencyInjectField.class),
                        Named.of("Inject Method ", IndirectCyclicDependencyInjectMethod.class)))
                    for (Named<?> anotherDependency : List.of(Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                            Named.of("Inject Field ", IndirectCyclicAnotherDependencyInjectField.class),
                            Named.of("Inject Method ", IndirectCyclicAnotherDependencyInjectMethod.class)))
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
            return arguments.stream();
        }

        static class IndirectCyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicDependencyInjectField implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        static class IndirectCyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public IndirectCyclicAnotherDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements TestComponent {
            @Inject
            TestComponent component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements TestComponent {
            @Inject
            void install(TestComponent component) {
            }
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {
            }
        }

        @Test
        void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
        }

        @Nested
        public class WithQualifier {
            @ParameterizedTest
            @MethodSource
            void should_throw_exception_if_dependency_with_qualifier_not_found(Class<? extends TestComponent> component) {
                config.bind(Dependency.class, dependency);
                config.bind(TestComponent.class, component, new Utils.NamedLiteral("Owner"));

                DependencyNotFoundException e = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

                assertEquals(new Component(TestComponent.class, new Utils.NamedLiteral("Owner")), e.getComponent());
                assertEquals(new Component(Dependency.class, new Utils.SkywalkerLiteral()), e.getDependency());
            }

            public static Stream<Arguments> should_throw_exception_if_dependency_with_qualifier_not_found() {
                return Stream.of(
                        Named.of("Inject Constructor with Qualifier", InjectConstructor.class),
                        Named.of("Inject Field  with Qualifier", InjectField.class),
                        Named.of("Inject Method with Qualifier", InjectMethod.class),
                        Named.of("Provider in Inject Constructor with Qualifier", InjectConstructorProvider.class),
                        Named.of("Provider in Inject Field  with Qualifier", InjectFieldProvider.class),
                        Named.of("Provider in Inject Method with Qualifier", InjectMethodProvider.class)
                ).map(Arguments::of);
            }

            static class InjectConstructor {
                @Inject
                public InjectConstructor(@Utils.Skywalker Dependency dependency) {
                }
            }

            static class InjectField {
                @Inject
                @Utils.Skywalker
                Dependency dependency;
            }

            static class InjectMethod {
                private Dependency dependency;

                @Inject
                void install(@Utils.Skywalker Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            static class InjectConstructorProvider {
                @Inject
                public InjectConstructorProvider(@Utils.Skywalker Provider<Dependency> dependency) {

                }
            }

            static class InjectFieldProvider {
                @Inject
                @Utils.Skywalker
                Provider<Dependency> dependency;
            }

            static class InjectMethodProvider {
                @Inject
                void install(@Utils.Skywalker Provider<Dependency> dependency) {

                }
            }

            // A -> @Skywalker A -> @Named A
            static class SkywalkerDependency implements Dependency {
                @Inject
                public SkywalkerDependency(@jakarta.inject.Named("ChosenOne") Dependency dependency) {

                }
            }

            static class NotCyclicDependency implements Dependency {
                @Inject
                public NotCyclicDependency(@Utils.Skywalker Dependency dependency) {

                }
            }

            @ParameterizedTest(name = "{1} -> @Skywalker({0} -> @Named(\"ChosenOne\") not cyclic dependencies")
            @MethodSource
            void should_not_throw_cyclic_exception_if_component_with_same_type_tagged_with_different_qualifiers(Class<? extends Dependency> notCyclic,
                                                                                                                Class<? extends Dependency> skywalker) {
                config.bind(Dependency.class, dependency, new Utils.NamedLiteral("ChosenOne"));
                config.bind(Dependency.class, skywalker, new Utils.SkywalkerLiteral());
                config.bind(Dependency.class, notCyclic);

                assertDoesNotThrow(() -> config.getContext());
            }

            public static Stream<Arguments> should_not_throw_cyclic_exception_if_component_with_same_type_tagged_with_different_qualifiers() {
                List<Arguments> arguments = new ArrayList<>();
                for (Named<?> skywalker : List.of(Named.of("Inject Constructor", SkywalkerInjectConstructor.class),
                        Named.of("Inject Field", SkywalkerInjectField.class),
                        Named.of("Inject Method", SkywalkerInjectMethod.class)))
                    for (Named<?> notCyclic : List.of(Named.of("Inject Constructor", NotCyclicInjectConstructor.class),
                            Named.of("Inject Field", NotCyclicInjectField.class),
                            Named.of("Inject Method", NotCyclicInjectMethod.class)))
                        arguments.add(Arguments.of(skywalker, notCyclic));
                return arguments.stream();
            }

            static class SkywalkerInjectConstructor implements Dependency {
                @Inject
                public SkywalkerInjectConstructor(@Utils.Skywalker Dependency dependency) {

                }
            }

            static class SkywalkerInjectField implements Dependency {
                @Inject
                @Utils.Skywalker
                Dependency dependency;
            }

            static class SkywalkerInjectMethod implements Dependency {
                @Inject
                void install(@Utils.Skywalker Dependency dependency) {
                }
            }

            static class NotCyclicInjectConstructor implements Dependency {
                @Inject
                public NotCyclicInjectConstructor(@jakarta.inject.Named("ChosenOne") Dependency dependency) {
                }
            }

            static class NotCyclicInjectField implements Dependency {
                @Inject
                @jakarta.inject.Named("ChosenOne")
                Dependency dependency;
            }

            static class NotCyclicInjectMethod implements Dependency {
                @Inject
                void install(@jakarta.inject.Named("ChosenOne") Dependency dependency) {
                }
            }

        }
    }
}

