/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jon Peterson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
