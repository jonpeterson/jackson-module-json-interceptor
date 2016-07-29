package com.github.jonpeterson.jackson.module.interceptor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import spock.lang.Specification

class JsonInterceptorModuleTest extends Specification {

    @JsonInterceptors(beforeDeserialization = [IntegerDoublerInterceptor, IntegerIncrementerInterceptor], afterSerialization = [IntegerIncrementerInterceptor, IntegerDoublerInterceptor])
    static class SomeModel {
        String a
        int b
        List<OtherModel> c
    }

    @JsonInterceptors(beforeDeserialization = [IntegerDoublerInterceptor])
    static class OtherModel {
        int d
        int e
        YetAnotherModel f
    }

    @JsonInterceptors()
    static class YetAnotherModel {
        int g
    }

    static abstract class FieldClosureInterceptor implements JsonInterceptor {

        @Override
        JsonNode intercept(JsonNode jsonNode) {
            def objectNode = jsonNode as ObjectNode
            objectNode.fields().each {
                def newNode = updateField(it.value)
                if(newNode != null)
                    objectNode.set(it.key, newNode)
            }
            return objectNode
        }

        abstract JsonNode updateField(JsonNode jsonNode)
    }

    static class IntegerDoublerInterceptor extends FieldClosureInterceptor {

        @Override
        JsonNode updateField(JsonNode jsonNode) {
            return jsonNode.isInt() ? IntNode.valueOf(jsonNode.asInt() * 2) : null
        }
    }

    static class IntegerIncrementerInterceptor extends FieldClosureInterceptor {

        @Override
        JsonNode updateField(JsonNode jsonNode) {
            return jsonNode.isInt() ? IntNode.valueOf(jsonNode.asInt() + 1) : null
        }
    }


    def 'deserialize and re-serialize'() {
        given:
        def mapper = new ObjectMapper()
            .registerModule(new JsonInterceptorModule())
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)

        when:
        def deserialized = mapper.readValue('{"a":"abc","b":125,"c":[{"d":33,"e":11,"f":{"g":5}},{"d":50,"e":10}]}', SomeModel)
        def reserialized = mapper.writeValueAsString(deserialized)

        then:
        with(deserialized) {
            a == 'abc'
            b == 251
            c.size() == 2
            with(c[0]) {
                d == 66
                e == 22
                with(f) {
                    g == 5
                }
            }
            with(c[1]) {
                d == 100
                e == 20
                f == null
            }
        }
        reserialized == '{"a":"abc","b":504,"c":[{"d":66,"e":22,"f":{"g":5}},{"d":100,"e":20,"f":null}]}'
    }
}
