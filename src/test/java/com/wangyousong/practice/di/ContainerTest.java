package com.wangyousong.practice.di;

import org.junit.jupiter.api.Nested;

class ContainerTest {


    @Nested
    public class DependenciesSelection {

        @Nested
        public class ProviderType {
            // Context
            // TODO: could get Provider<T> from context

            // Inject Provider
            // TODO: support inject constructor
            // TODO: support inject field
            // TODO: support inject method
        }

        @Nested
        public class Qualifier {

        }

    }

    @Nested
    public class LifecycleManagement {

    }

}

interface Component {
    default Dependency dependency() {
        return null;
    }
}

interface Dependency {

}

interface AnotherDependency {

}

