package com.wangyousong.practice.di;

import java.lang.reflect.Type;
import java.util.Optional;

public interface Context {

    <T> Optional<T> getType(Type type);
}
