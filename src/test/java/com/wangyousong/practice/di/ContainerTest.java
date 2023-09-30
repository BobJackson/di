package com.wangyousong.practice.di;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ContainerTest {

    interface Component {

    }

    @Nested
    public class ComponentConstruction {
        // TODO: instance
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
            // TODO: No args constructor
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
