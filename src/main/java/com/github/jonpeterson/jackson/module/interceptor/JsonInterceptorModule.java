package com.github.jonpeterson.jackson.module.interceptor;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class JsonInterceptorModule extends SimpleModule {

    public JsonInterceptorModule() {
        super("JsonInterceptor");

        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDescription, JsonDeserializer<?> deserializer) {
                if(deserializer instanceof StdDeserializer) {
                    JsonInterceptors jsonInterceptors = beanDescription.getClassAnnotations().get(JsonInterceptors.class);
                    if(jsonInterceptors != null) {
                        Class<? extends JsonInterceptor>[] interceptors = jsonInterceptors.beforeDeserialization();
                        if(interceptors.length > 0)
                            return createInterceptingDeserializer((StdDeserializer)deserializer, interceptors);
                    }
                }

                return deserializer;
            }

            private <T> JsonInterceptingDeserializer<T> createInterceptingDeserializer(StdDeserializer<T> deserializer, Class<? extends JsonInterceptor>[] interceptors) {
                return new JsonInterceptingDeserializer<T>(deserializer, interceptors);
            }
        });

        setSerializerModifier(new BeanSerializerModifier() {

            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDescription, JsonSerializer<?> serializer) {
                if(serializer instanceof StdSerializer) {
                    JsonInterceptors jsonInterceptors = beanDescription.getClassAnnotations().get(JsonInterceptors.class);
                    if(jsonInterceptors != null) {
                        Class<? extends JsonInterceptor>[] interceptors = jsonInterceptors.afterSerialization();
                        if(interceptors.length > 0)
                            return createInterceptingSerializer((StdSerializer)serializer, interceptors);
                    }
                }

                return serializer;
            }

            private <T> JsonInterceptingSerializer<T> createInterceptingSerializer(StdSerializer<T> serializer, Class<? extends JsonInterceptor>[] interceptors) {
                return new JsonInterceptingSerializer<T>(serializer, interceptors);
            }
        });
    }
}
