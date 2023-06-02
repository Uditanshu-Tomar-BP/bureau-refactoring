package com.bharatpe.lending.bureaurefactoring.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class HealthCheckController {
    @GetMapping(value = "/")
    public ResponseEntity<Object> healthCheck() {
        return ResponseEntity.ok("Bureau Details service is running");
    }
}
