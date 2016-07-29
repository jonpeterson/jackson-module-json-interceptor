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
