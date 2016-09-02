package com.cmpwrn.controller;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.regex;


@EnableSwagger2
@Configuration
public class SwaggerConfiguration {

    @Bean
    public Docket docket() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(false)
                .apiInfo(apiInfo())
                .select()
                .paths(paths())
                .build();

        docket.globalOperationParameters(
                Lists.newArrayList(
                        new ParameterBuilder()
                                .name("authToken")
                                .modelRef(new ModelRef("string"))
                                .parameterType("header")
                                .required(true)
                                .build()
                )
        );
        return docket;
    }


    private Predicate<String> paths() {
        return regex("^/(foo|bar).*");
    }


    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("fuzzer demo").description("fuzzer demo").build();
    }
}
