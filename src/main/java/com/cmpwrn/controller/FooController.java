package com.cmpwrn.controller;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@RestController
public class FooController {

    @ResponseStatus(OK)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found")
    })
    @RequestMapping(value = "/foo", method = POST)
    public ResponseEntity<String> foo(@RequestHeader("authToken") String authToken, @RequestBody AA aa) throws Exception {
        if (authToken != "valid auth token") {
            throw new Exception();
        }
        return new ResponseEntity<>("", OK);
    }


    @ResponseStatus(OK)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 403, message = "Forbidden")
    })
    @RequestMapping(value = "/bar", method = GET)
    public ResponseEntity<String> bar(@RequestHeader("authToken") String authToken, @RequestBody BB bb) throws Exception {
        if (authToken != "valid auth token") {
            return new ResponseEntity<>("you need a valid auth token", FORBIDDEN);
        }
        return new ResponseEntity<>("", OK);
    }
}

