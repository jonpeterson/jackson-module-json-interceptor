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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

public class JsonInterceptingDeserializer<T> extends StdDeserializer<T> implements ResolvableDeserializer{
    private final StdDeserializer<T> delegate;
    private final JsonInterceptor[] interceptors;

    public JsonInterceptingDeserializer(StdDeserializer<T> delegate, Class<? extends JsonInterceptor>... interceptorClasses) {
        super(delegate.getValueType());

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
    public void resolve(DeserializationContext context) throws JsonMappingException {
        if(delegate instanceof ResolvableDeserializer)
            ((ResolvableDeserializer)delegate).resolve(context);
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode jsonNode = parser.readValueAsTree();

        for(JsonInterceptor interceptor: interceptors)
            jsonNode = interceptor.intercept(jsonNode);

        JsonParser postInterceptionParser = new TreeTraversingParser(jsonNode, parser.getCodec());
        postInterceptionParser.nextToken();
        return delegate.deserialize(postInterceptionParser, context);
    }
}
