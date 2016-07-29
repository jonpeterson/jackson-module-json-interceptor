package com.github.jonpeterson.jackson.module.interceptor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JsonInterceptingSerializer<T> extends StdSerializer<T> implements ResolvableSerializer {
    private final StdSerializer<T> delegate;
    private final JsonInterceptor[] interceptors;

    public JsonInterceptingSerializer(StdSerializer<T> delegate, Class<? extends JsonInterceptor>... interceptorClasses) {
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
            jsonNode = interceptor.intercept(jsonNode);

        // write node
        generator.writeTree(jsonNode);
    }
}
