# java-time-swagger-extension-library
Library to solve problem with java time in swagger doc with spring boot

# How to use

## install this library by maven
1. clone this repo.
2. run maven goal "install"
3. ...
4. profit

## Add to project
In your project create a SwaggerConfig class

in SwaggerConfig write like this code
```
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI(@Value("${spring.application.name}")String appName, @Value("${spring.application.version}") String appVersion,SwaggerHelper swaggerHelper) {
        return new OpenAPI()
                .info(
                        new Info()
                                .title(appName)
                                .version(appVersion))
                .components(swaggerHelper.getConfiguredComponents("com.example.your_entities_package",ZoneId.of("Europe/Moscow").getRules().getOffset(Instant.now()))
                );
    }

    @Bean
    public SwaggerHelper swaggerHelper(){
        return new SwaggerHelper();
    }
}
```

## Add annotation
In your entity class on java time field add like this annotation

```
@JsonSerialize(using = OffsetDateTimeSerializer.class)
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
@CustomSchema(description = "Create time")
private OffsetDateTime createTime;
```
That is all!