package com.wangyousong.practice.di;

import org.junit.jupiter.api.Nested;

class ContainerTest {


    @Nested
    public class DependenciesSelection {

        @Nested
        public class ProviderType {

        }

        @Nested
        public class Qualifier {

        }

    }

    @Nested
    public class LifecycleManagement {

    }

}

interface TestComponent {
    default Dependency dependency() {
        return null;
    }
}

interface Dependency {

}

interface AnotherDependency {

}

