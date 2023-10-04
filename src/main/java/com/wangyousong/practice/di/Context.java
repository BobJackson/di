package com.wangyousong.practice.di;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

public interface Context {
    <Type> Optional<Type> get(Class<Type> type);

    <T> Optional<T> get(ParameterizedType type);
}
