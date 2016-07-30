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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import spock.lang.Specification

class ReadmeExampleTest extends Specification {

    static class CarsByType {
        String type
        List<Car> data
    }

    @JsonInterceptors(beforeDeserialization = CarDeserializationInterceptor)
    static class Car {
        String make
        String model
        int year
    }

    static class CarDeserializationInterceptor implements JsonInterceptor {

        @Override
        JsonNode intercept(JsonNode node, JsonNodeFactory nodeFactory) {
            if(node.isTextual()) {
                def parts = node.asText().split(':')

                def objectNode = nodeFactory.objectNode()
                objectNode.put('make', parts[0])
                objectNode.put('model', parts[1])
                objectNode.put('year', parts[2] as int)
                node = objectNode
            }

            return node
        }
    }

    def 'example'() {
        expect:

        // create the ObjectMapper and load the JSON Interceptor Module
        def mapper = new ObjectMapper().registerModule(new JsonInterceptorModule())

        // deserialize the data
        def cars = mapper.readValue(

            // note that Honda Civic is expressed in a packed string and Toyota Camry is expressed in an exploded object
            '''{
              |  "type": "sedans",
              |  "data": [
              |    "Honda:Civic:2016",
              |    {
              |      "make":"Toyota",
              |      "model":"Camry",
              |      "year":2017
              |    }
              |  ]
              |}'''.stripMargin(),

            // class to deserialize as
            CarsByType
        )

        // write {"type":"sedans","data":[{"make":"Honda","model":"Civic","year":2016},{"make":"Toyota","model":"Camry","year":2017}]}
        mapper.writeValueAsString(cars) == '{"type":"sedans","data":[{"make":"Honda","model":"Civic","year":2016},{"make":"Toyota","model":"Camry","year":2017}]}'
    }
}
