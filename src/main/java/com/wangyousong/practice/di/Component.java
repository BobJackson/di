package com.wangyousong.practice.di;

import java.lang.annotation.Annotation;

public record Component(Class<?> type, Annotation qualifiers) {

}
