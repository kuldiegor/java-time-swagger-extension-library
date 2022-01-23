package com.kuldiegor.java_time_swagger_extension;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

public class SwaggerHelper {

    public Components getConfiguredComponents(String packageName) {
        Components components = new Components();
        List<Class> classList = getClassesWithCustomSchemaInFields(packageName);
        for (Class<?> aClass : classList) {
            Schema schema = getConfiguredSchemaByClass(aClass);
            components.addSchemas(schema.getName(), schema);
        }
        return components;
    }

    private Schema getConfiguredSchemaByClass(Class<?> aClass) {
        Schema schema = getSchemaByClass(aClass);
        BeanDescription beanDescription = getBeanDescriptionByClass(aClass);
        for (BeanPropertyDefinition beanPropertyDefinition : beanDescription.findProperties()) {
            AnnotatedField field = beanPropertyDefinition.getField();
            CustomSchema customSchema = field.getAnnotation(CustomSchema.class);
            if (customSchema != null) {
                String description = customSchema.description();
                String formatPattern = null;
                JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
                if (jsonFormat != null) {
                    formatPattern = jsonFormat.pattern();
                }
                if (formatPattern != null) {
                    schema.getProperties().put(beanPropertyDefinition.getName(), createDatetimeSchema(formatPattern, description));
                }
            }
        }
        return schema;
    }

    private BeanDescription getBeanDescriptionByClass(Class<?> aClass) {
        ObjectMapper objectMapper = new ObjectMapper();
        BeanDescription recurBeanDesc = objectMapper.getSerializationConfig().introspect(objectMapper.constructType(aClass));

        HashSet<String> visited = new HashSet<>();
        JsonSerialize jsonSerialize = recurBeanDesc.getClassAnnotations().get(JsonSerialize.class);
        while (jsonSerialize != null && !Void.class.equals(jsonSerialize.as())) {
            String asName = jsonSerialize.as().getName();
            if (visited.contains(asName)) break;
            visited.add(asName);

            recurBeanDesc = objectMapper.getSerializationConfig().introspect(
                    objectMapper.constructType(jsonSerialize.as())
            );
            jsonSerialize = recurBeanDesc.getClassAnnotations().get(JsonSerialize.class);
        }
        return recurBeanDesc;
    }

    private Schema createDatetimeSchema(String formatPattern, String description) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(formatPattern);
        CustomStringSchema offsetDatetime = new CustomStringSchema();
        offsetDatetime.setDescription(description);
        offsetDatetime.setExample(dateTimeFormatter.format(OffsetDateTime.now()));
        return offsetDatetime;
    }

    private <T> Schema getSchemaByClass(Class<T> classOfT) {
        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(
                        new AnnotatedType(classOfT).resolveAsRef(false));
        return resolvedSchema.schema;
    }

    private List<Class> getClassesWithCustomSchemaInFields(String packageName) {
        List<Class> classList = new ArrayList<>();
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        final Set<BeanDefinition> classes = provider.findCandidateComponents(packageName);
        for (BeanDefinition bean : classes) {
            try {
                Class<?> aClass = Class.forName(bean.getBeanClassName());
                boolean isThereACustomSchemaAnnotation = Arrays.stream(aClass.getDeclaredFields()).anyMatch(it -> it.getAnnotation(CustomSchema.class) != null);
                if (isThereACustomSchemaAnnotation) {
                    classList.add(aClass);
                }
            } catch (ClassNotFoundException e) {

            }
        }
        return classList;
    }
}
