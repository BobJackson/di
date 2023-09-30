package com.wangyousong.practice.di;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerTest {

    interface Component {

    }

    static class ComponentWithDefaultConstructor implements Component {
        public ComponentWithDefaultConstructor() {
        }
    }

    @Nested
    public class ComponentConstruction {
        @Test
        void should_bind_type_to_a_specific_instance() {
            var context = new Context();
            var instance = new Component() {
            };
            context.bind(Component.class, instance);

            assertSame(instance, context.get(Component.class));
        }


        // TODO: abstract class
        // TODO: interface

        @Nested
        public class ConstructorInjection {

            @Test
            void should_bind_type_to_a_class_with_default_constructor() {
                var context = new Context();
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                var instance = context.get(Component.class);

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }


            // TODO: with dependencies
            // TODO: A -> B -> C
        }

    }

    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }

}
