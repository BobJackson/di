package com.wangyousong.practice.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InjectionTest {
    ContextConfig config;

    @BeforeEach
    void setUp() {
        config = new ContextConfig();
    }

    private <T, R extends T> T getComponent(Class<T> type, Class<R> implementation) {
        config.bind(type, implementation);
        return config.getContext().get(type).get();
    }

    @Nested
    public class ConstructorInjection {

        @Test
        void should_bind_type_to_a_class_with_default_constructor() {
            Component instance = getComponent(Component.class, ComponentWithDefaultConstructor.class);

            assertNotNull(instance);
            assertTrue(instance instanceof ComponentWithDefaultConstructor);
        }

        @Test
        void should_bind_type_to_a_class_with_inject_constructor() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);

            Component instance = getComponent(Component.class, ComponentWithInjectConstructor.class);

            assertNotNull(instance);
            assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
        }

        @Test
        void should_bind_type_to_a_class_with_transitive_dependencies() {
            config.bind(Dependency.class, DependencyWithInjectConstructor.class);
            config.bind(String.class, "indirect dependency");

            Component instance = getComponent(Component.class, ComponentWithInjectConstructor.class);

            assertNotNull(instance);

            Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
            assertNotNull(dependency);

            assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
        }

        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent() {
            }
        }

        @Test
        void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(ConstructorInjection.AbstractComponent.class);
            });
        }

        @Test
        void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(Component.class);
            });
        }

        @Test
        void should_throw_exception_if_multi_inject_constructors_provided() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructors.class);
            });
        }

        @Test
        void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(ComponentWithNoInjectConstructorNorDefaultConstructor.class);
            });
        }

        @Test
        void should_include_dependency_from_inject_constructor() {
            ConstructorInjectionProvider<ComponentWithInjectConstructor> provider = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }
    }

    @Nested
    public class FieldInjection {

        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
        }

        @Test
        void should_inject_dependency_via_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);

            ComponentWithFieldInjection component = getComponent(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

            assertSame(dependency, component.dependency);
        }

        @Test
        void should_inject_dependency_via_superclass_inject_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            SubclassWithFieldInjection component = getComponent(SubclassWithFieldInjection.class, SubclassWithFieldInjection.class);

            assertSame(dependency, component.dependency);
        }

        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        void should_throw_exception_if_inject_field_is_final() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(FieldInjection.FinalInjectField.class);
            });
        }

        @Test
        void should_include_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install(Dependency dependency) {
            }
        }

        @Test
        void should_throw_exception_if_inject_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(FieldInjection.InjectMethodWithTypeParameter.class);
            });
        }
    }

    @Nested
    public class MethodInjection {
        static class InjectMethodWithNoDependency {
            boolean called = false;

            @Inject
            void install() {
                called = true;
            }
        }

        @Test
        void should_call_inject_method_even_if_no_dependency_declared() {
            InjectMethodWithNoDependency component = getComponent(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);

            assertTrue(component.called);
        }

        static class InjectMethodWithDependency {
            Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        @Test
        void should_inject_dependency_via_inject_method() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);

            InjectMethodWithDependency component = getComponent(InjectMethodWithDependency.class, InjectMethodWithDependency.class);

            assertSame(dependency, component.dependency);
        }

        static class SuperClassWithInjectMethod {
            int supperCalled = 0;

            @Inject
            void install() {
                supperCalled++;
            }
        }

        static class SubClassWithInjectMethod extends MethodInjection.SuperClassWithInjectMethod {
            int subCalled = 0;

            @Inject
            void installAnother() {
                subCalled = supperCalled + 1;
            }
        }

        @Test
        void should_inject_dependencies_via_inject_method_from_superclass() {
            SubClassWithInjectMethod component = getComponent(SubClassWithInjectMethod.class, SubClassWithInjectMethod.class);

            assertEquals(1, component.supperCalled);
            assertEquals(2, component.subCalled);
        }

        static class SubclassOverrideSuperclassWithInject extends MethodInjection.SuperClassWithInjectMethod {
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        void should_only_call_once_if_subclass_override_inject_method_with_inject() {
            SubclassOverrideSuperclassWithInject component = getComponent(SubclassOverrideSuperclassWithInject.class, SubclassOverrideSuperclassWithInject.class);

            assertEquals(1, component.supperCalled);
        }

        static class SubclassOverrideSuperclassWithNoInject extends MethodInjection.SuperClassWithInjectMethod {

            void install() {
                super.install();
            }
        }

        @Test
        void should_not_call_inject_method_if_override_with_no_inject() {
            SubclassOverrideSuperclassWithNoInject component = getComponent(SubclassOverrideSuperclassWithNoInject.class, SubclassOverrideSuperclassWithNoInject.class);

            assertEquals(0, component.supperCalled);
        }

        @Test
        void should_include_dependencies_from_inject_methods() {
            ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }
    }
}