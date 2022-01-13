package com.kuldiegor.java_time_swagger_extension;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    public Components getConfiguredComponents(String packageName,ZoneOffset zoneOffset){
        Components components = new Components();
        List<Class> classList = getClassesWithCustomSchemaInFields(packageName);
        for (Class<?> aClass: classList){
            Schema schema = getConfiguredSchemaByClass(aClass,zoneOffset);
            components.addSchemas(schema.getName(),schema);
        }
        return components;
    }

    private Schema getConfiguredSchemaByClass(Class<?> aClass, ZoneOffset zoneOffset){
        Schema schema = getSchemaByClass(aClass);
        for (Field field:aClass.getDeclaredFields()){
            CustomSchema customSchema = field.getAnnotation(CustomSchema.class);
            if (customSchema!=null){
                String description = customSchema.description();
                String formatPattern = null;
                JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
                if (jsonFormat!=null){
                    formatPattern = jsonFormat.pattern();
                }
                if (formatPattern!=null){
                    schema.getProperties().put(field.getName(),createDatetimeSchema(formatPattern,description,zoneOffset));
                }
            }
        }
        return schema;
    }

    private Schema createDatetimeSchema(String formatPattern,String description,ZoneOffset zoneOffset){
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(formatPattern);
        Schema<String> offsetDatetime = new Schema<>();
        offsetDatetime.setType("string");
        offsetDatetime.setDescription(description);
        offsetDatetime.setExample(dateTimeFormatter.format(OffsetDateTime.now(zoneOffset)));
        return offsetDatetime;
    }

    private <T> Schema getSchemaByClass(Class<T> classOfT){
        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(
                        new AnnotatedType(classOfT).resolveAsRef(false));
        return resolvedSchema.schema;
    }

    private List<Class> getClassesWithCustomSchemaInFields(String packageName){
        List<Class> classList = new ArrayList<>();
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        final Set<BeanDefinition> classes = provider.findCandidateComponents(packageName);
        for (BeanDefinition bean: classes) {
            try {
                Class<?> aClass = Class.forName(bean.getBeanClassName());
                boolean isThereACustomSchemaAnnotation = Arrays.stream(aClass.getDeclaredFields()).anyMatch(it -> it.getAnnotation(CustomSchema.class)!=null);
                if (isThereACustomSchemaAnnotation) {
                    classList.add(aClass);
                }
            } catch (ClassNotFoundException e) {

            }
        }
        return classList;
    }
}
