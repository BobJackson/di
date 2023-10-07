package com.wangyousong.practice.di;

@FunctionalInterface
interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> provider);
}
