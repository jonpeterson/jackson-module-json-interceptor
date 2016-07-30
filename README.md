# Jackson JSON Interceptor Module

Jackson 2.x module for intercepting and manipulating raw JSON before deserialization and after serialization.

## Compatibility

Compiled for Java 6 and tested with Jackson 2.2 - 2.8.

## Getting Started with Gradle

```groovy
dependencies {
    compile 'com.github.jonpeterson:jackson-module-json-interceptor:1.0.0'
}
```

## Getting Started with Maven

```xml
<dependency>
    <groupId>com.github.jonpeterson</groupId>
    <artifactId>jackson-module-json-interceptor</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Example

#### Define the POJO models
```groovy
class CarsByType {
    String type
    List<Car> data
}
```

```groovy
@JsonInterceptors(beforeDeserialization = CarDeserializationInterceptor)
class Car {
    String make
    String model
    int year
}
```

#### Define the JSON interceptor
```groovy
class CarDeserializationInterceptor implements JsonInterceptor {

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
```

#### Test the interceptor
```groovy
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

// serialize and compare
assert mapper.writeValueAsString(cars) == '{"type":"sedans","data":[{"make":"Honda","model":"Civic","year":2016},{"make":"Toyota","model":"Camry","year":2017}]}'
```

See other test cases under `src/test/groovy`.

## [JavaDoc](https://jonpeterson.github.io/docs/jackson-module-json-interceptor/1.0.0/index.html)