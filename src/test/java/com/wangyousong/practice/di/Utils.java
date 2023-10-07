package com.wangyousong.practice.di;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Objects;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

record Utils() {

    record NamedLiteral(String value) implements jakarta.inject.Named {

        @Override
        public Class<? extends Annotation> annotationType() {
            return jakarta.inject.Named.class;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof jakarta.inject.Named named) {
                return Objects.equals(value, named.value());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return "value".hashCode() * 127 ^ value.hashCode();
        }
    }

    @java.lang.annotation.Documented
    @java.lang.annotation.Retention(RUNTIME)
    @jakarta.inject.Qualifier
    @interface Skywalker {
    }

    record SkywalkerLiteral() implements Skywalker {

        @Override
        public Class<? extends Annotation> annotationType() {
            return Skywalker.class;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Skywalker;
        }
    }

    record TestLiteral() implements Test {

        @Override
        public Class<? extends Annotation> annotationType() {
            return Test.class;
        }
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
