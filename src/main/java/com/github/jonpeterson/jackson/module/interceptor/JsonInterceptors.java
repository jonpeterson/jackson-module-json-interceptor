package com.github.jonpeterson.jackson.module.interceptor;

import com.fasterxml.jackson.annotation.JacksonAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonInterceptors {

    Class<? extends JsonInterceptor>[] beforeDeserialization() default {};

    Class<? extends JsonInterceptor>[] afterSerialization() default {};
}
