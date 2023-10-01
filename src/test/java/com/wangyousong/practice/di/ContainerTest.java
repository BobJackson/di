package com.wangyousong.practice.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContainerTest {

    Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @Nested
    public class ComponentConstruction {
        @Test
        void should_bind_type_to_a_specific_instance() {
            var instance = new Component() {
            };
            context.bind(Component.class, instance);

            assertSame(instance, context.get(Component.class).get());
        }


        // TODO: abstract class
        // TODO: interface


        @Test
        void should_return_empty_if_component_not_defined() {
            Optional<Component> component = context.get(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        public class ConstructorInjection {

            @Test
            void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                var instance = context.get(Component.class).get();

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            @Test
            void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };

                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);

                Component instance = context.get(Component.class).get();

                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            @Test
            void should_bind_type_to_a_class_with_transitive_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                context.bind(String.class, "indirect dependency");

                Component instance = context.get(Component.class).get();

                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);

                assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            @Test
            void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            @Test
            void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            @Test
            void should_throw_exception_if_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);

                DependencyNotFoundException e = assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class));

                assertEquals(Dependency.class, e.getDependency());
            }

            @Test
            void should_throw_exception_if_transitive_dependency_found() {
                context.bind(Component.class,ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFoundException e = assertThrows(DependencyNotFoundException.class, () -> context.get(Component.class));

                assertEquals(String.class, e.getDependency());
                assertEquals(Dependency.class, e.getComponent());
            }

            @Test
            void should_throw_exception_if_cyclic_dependencies_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesFoundException e = assertThrows(CyclicDependenciesFoundException.class, () -> context.get(Component.class));

                Set<Class<?>> classes = Set.of(e.getComponents());

                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }

            @Test
            void should_throw_exception_if_transitive_cyclic_dependencies_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                context.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

                CyclicDependenciesFoundException e = assertThrows(CyclicDependenciesFoundException.class, () -> context.get(Component.class));

                Set<Class<?>> classes = Set.of(e.getComponents());

                assertEquals(3, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
                assertTrue(classes.contains(AnotherDependency.class));
            }
        }

    }

    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }

}

interface Component {

}

interface Dependency {

}

interface AnotherDependency {

}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithInjectConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectConstructors implements Component {
    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }
}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {
    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}

class DependencyWithInjectConstructor implements Dependency {
    String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class DependencyDependedOnComponent implements Dependency {
    Component component;

    @Inject
    public DependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}

class AnotherDependencyDependedOnComponent implements AnotherDependency {
    Component component;

    @Inject
    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}

class DependencyDependedOnAnotherDependency implements Dependency {
    AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}
