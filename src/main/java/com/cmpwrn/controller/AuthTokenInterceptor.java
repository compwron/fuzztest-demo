package com.cmpwrn.controller;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class AuthTokenInterceptor extends HandlerInterceptorAdapter {

    private Map<String, RequestMethod> NO_AUTH_TOKEN_CALLS = ImmutableMap.of("/v2/api-docs", RequestMethod.GET,
            "/configuration/ui", RequestMethod.GET, "/swagger-resources", RequestMethod.GET);


    @Override
    public boolean preHandle(HttpServletRequest inputRequest, HttpServletResponse response, Object handler) {
        AuthenticatedRequest request = new AuthenticatedRequest(inputRequest);
        if (request.isExemptFromAuthTokenCheck()) {
            return true;
        }
        if (!request.isAuthTokenPresent()) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }

        if (request.getAuthToken() != "valid auth token") {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }

        return true;
    }


    private class AuthenticatedRequest {

        private HttpServletRequest request;


        AuthenticatedRequest(HttpServletRequest request) {
            this.request = request;
        }


        boolean isExemptFromAuthTokenCheck() {
            for (Map.Entry<String, RequestMethod> entry : NO_AUTH_TOKEN_CALLS.entrySet()) {
                if (request.getRequestURI().equals(entry.getKey()) && request.getMethod().equals(entry.getValue()
                        .toString())) {
                    return true;
                }
            }
            return false;
        }


        boolean isAuthTokenPresent() {
            String authentication = getAuthentication();
            return !(authentication == null || !authentication.startsWith("Bearer"));
        }


        String getAuthToken() {
            return getAuthentication().split("Bearer ")[1];
        }


        private String getAuthentication() {
            return request.getHeader("Authorization");
        }
    }
}
