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

package com.github.jonpeterson.jackson.module.interceptor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
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
        JsonNode intercept(JsonNode node, JsonNodeFactory nodeFactory) {
            def objectNode = node as ObjectNode
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
