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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class JsonInterceptingSerializer<T> extends StdSerializer<T> implements ResolvableSerializer {
    private static final JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);

    private final StdSerializer<T> delegate;
    private final JsonInterceptor[] interceptors;

    JsonInterceptingSerializer(StdSerializer<T> delegate, Class<? extends JsonInterceptor>... interceptorClasses) {
        super(delegate.handledType());

        this.delegate = delegate;

        interceptors = new JsonInterceptor[interceptorClasses.length];
        for(int i = 0; i < interceptorClasses.length; i++) {
            try {
                interceptors[i] = interceptorClasses[i].newInstance();
            } catch(Exception e) {
                throw new RuntimeException("unable to create instance of interceptor '" + interceptorClasses[i].getName() + "'", e);
            }
        }
    }

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException {
        if(delegate instanceof ResolvableSerializer)
            ((ResolvableSerializer)delegate).resolve(provider);
    }

    @Override
    public void serialize(T value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        // serialize the value into a byte array buffer then parse it back out into a JsonNode tree
        // TODO: find a better way to convert the value into a tree
        JsonFactory factory = generator.getCodec().getFactory();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
        JsonGenerator bufferGenerator = factory.createGenerator(buffer);
        try {
            delegate.serialize(value, bufferGenerator, provider);
        } finally {
            bufferGenerator.close();
        }
        JsonNode jsonNode = factory.createParser(buffer.toByteArray()).readValueAsTree();

        // execute interceptors on node
        for(JsonInterceptor interceptor: interceptors)
            jsonNode = interceptor.intercept(jsonNode, jsonNodeFactory);

        // write node
        generator.writeTree(jsonNode);
    }
}
