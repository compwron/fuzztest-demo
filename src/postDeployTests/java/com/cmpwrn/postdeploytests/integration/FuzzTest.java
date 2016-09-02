package com.cmpwrn.postdeploytests.integration;

import com.google.common.collect.Lists;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.SwaggerParser;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.*;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;


public class FuzzTest {

    private String authToken = "valid auth token";
    private RestTemplate restTemplate = new RestTemplate();
    private String endpoint = endpoint();
    private String swaggerLocation = endpoint + "/v2/api-docs";
    private Swagger swagger = new SwaggerParser().read(swaggerLocation);
    private Map<String, Path> paths = swagger.getPaths();
    private Matcher<Integer> matcher = lessThan(HttpStatus.INTERNAL_SERVER_ERROR.value());

    @Before
    public void setup() {
        restTemplate.setErrorHandler(errorHandler());
        paths = swagger.getPaths();
    }

    @Test
    public void allEndpointsMustRequireAuthToken() {
        HttpEntity httpEntity = new HttpEntity(invalidHeaders());
        assertReturnCode(paths, httpEntity, is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    public void noEndpointShouldEverReturnServerErrorWhenPassedEmptyArguments() {
        HttpEntity httpEntity = new HttpEntity(validHeaders());
        assertReturnCode(paths, httpEntity, lessThan(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }


    @Test
    public void noEndpointShouldEverReturnServerErrorWhenPassedInvalidArguments() {
        List<String> wipPathsToIgnore = Lists.newArrayList(); // Put endpoints for cards that are in progress here temporarily

        paths.forEach((pathName, path) -> {
            if (!wipPathsToIgnore.contains(pathName)) {
                httpMethodsToCheck(path).forEach((httpMethod) -> {
                    fuzzingStrategies().forEach((strategyName, strategy) -> {
                        int responseCode = apiResponseCode(interpolatedPathName(pathName), httpMethod, httpEntity(path, httpMethod, strategy));
                        String errorInfo = errorInfo(pathName, httpMethod, strategyName, responseCode);
                        assertThat(errorInfo, responseCode, matcher);
                    });
                });
            }
        });
    }

    private String interpolatedPathName(String pathName) {
        return pathName.replaceAll("\\{.*\\}", "testValue");
    }

    private HttpEntity httpEntity(Path path, HttpMethod httpMethod, Object strategy) {
        HttpEntity httpEntity;
        if (takesParameters(path, httpMethod)) {
            JSONObject body = new JSONObject();
            for (Parameter parameter : parameters(path, httpMethod)) {
                body.put(parameter.getName(), strategy);
            }
            httpEntity = new HttpEntity(body.toString(), validHeaders());
        } else {
            httpEntity = new HttpEntity(validHeaders());
        }
        return httpEntity;
    }

    private int apiResponseCode(String interpolatedPathName, HttpMethod httpMethod, HttpEntity httpEntity) {
        ResponseEntity<String> response = restTemplate.exchange(endpoint + interpolatedPathName, httpMethod, httpEntity, String.class);
        return response.getStatusCode().value();
    }

    private String errorInfo(String pathName, HttpMethod httpMethod, String strategyName, int value) {
        return "ERROR: path " +
                pathName +
                "\n with http method: " +
                httpMethod +
                "\nwith fields all using strategy: " +
                strategyName +
                "\n returned " +
                value;
    }

    private Map<String, Object> fuzzingStrategies() {
        String hamburgerEmoji = "\uD83C\uDF54";
        String weirdCharacters = "0~!@#$%^&*()_+`';:,.></?|%n%s\"";
        String emptyString = "";

        return new HashMap<String, Object>() {
            {
                put("hamburger emojii", hamburgerEmoji);
                put("weirdCharacters", weirdCharacters);
                put("true", true);
                put("false", false);
                put("negative one", -1);
                put("long string", longString());
                put("empty string", emptyString);
                put("null", null);
            }
        };
    }

    private String longString() {
        StringBuffer longString = new StringBuffer();
        Random random = new Random();
        int longStringLength = 10000;
        while (longString.length() < longStringLength) {
            int asciiNum = random.nextInt(25) + 97;
            longString.append("a" + asciiNum);
        }
        return longString.toString();
    }

    private boolean takesParameters(Path path, HttpMethod httpMethod) {
        List<Parameter> params = parameters(path, httpMethod);
        return params.size() > 0;
    }

    private List<Parameter> parameters(Path path, HttpMethod httpMethod) {
        return operationMap(path, httpMethod).getParameters()
                .stream()
                .filter(param -> param.getClass().equals(BodyParameter.class))
                .collect(Collectors.toList());
    }

    private void assertReturnCode(Map<String, Path> paths, HttpEntity httpEntity, Matcher<Integer> integerMatcher) {
        paths.forEach((pathName, path) -> {
            httpMethodsToCheck(path).forEach((httpMethod) -> {
                int value = apiCallResponseCode(httpEntity, interpolatedPathName(pathName), httpMethod);
                assertThat(errorInfo(pathName, httpMethod, "empty fields", value), value, integerMatcher);
            });
        });
    }

    private int apiCallResponseCode(HttpEntity httpEntity, String interpolatedPathName, HttpMethod httpMethod) {
        ResponseEntity<String> response = restTemplate.exchange(endpoint + interpolatedPathName, httpMethod, httpEntity, String.class);
        return response.getStatusCode().value();
    }

    private String endpoint() {
        String endpoint = System.getenv("INTEGRATION_TEST_API_ENDPOINT");
        if (endpoint == null) {
            return "http://localhost:8080";
        }
        return endpoint;
    }


    private Operation operationMap(Path path, HttpMethod httpMethod) {
        switch (httpMethod) {
            case GET:
                return path.getGet();
            case POST:
                return path.getPost();
            case DELETE:
                return path.getDelete();
            case PUT:
                return path.getPut();
            default:
                return null;
        }
    }


    private List<HttpMethod> httpMethodsToCheck(Path path) {
        List<HttpMethod> httpMethods = new ArrayList<>();
        if (path.getDelete() != null) {
            httpMethods.add(HttpMethod.DELETE);
        }
        if (path.getPost() != null) {
            httpMethods.add(HttpMethod.POST);
        }
        if (path.getGet() != null) {
            httpMethods.add(HttpMethod.GET);
        }
        if (path.getPut() != null) {
            httpMethods.add(HttpMethod.PUT);
        }
        return httpMethods;
    }


    private HttpHeaders validHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private HttpHeaders invalidHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid auth token");
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private ResponseErrorHandler errorHandler() {
        return new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }


            @Override
            public void handleError(ClientHttpResponse response) throws IOException {

            }
        };
    }
}
