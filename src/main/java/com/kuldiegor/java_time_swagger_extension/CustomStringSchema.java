package com.kuldiegor.java_time_swagger_extension;

import io.swagger.v3.oas.models.media.Schema;

public class CustomStringSchema extends Schema<String> {
    public CustomStringSchema() {
        super("string", "date-time");
    }
}
