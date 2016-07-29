package com.github.jonpeterson.jackson.module.interceptor;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonInterceptor {

    JsonNode intercept(JsonNode jsonNode);
}
