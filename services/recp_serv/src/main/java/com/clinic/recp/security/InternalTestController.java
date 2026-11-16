package com.clinic.recp.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/test")
public class InternalTestController {

    @GetMapping
    public Map<String, String> test() {
        return Map.of("status", "internal access granted");
    }
}
