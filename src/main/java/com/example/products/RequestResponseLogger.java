package com.example.products;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Slf4j
@AllArgsConstructor
@Component
@Aspect
public class RequestResponseLogger {

    private final ObjectMapper objectMapper;

    // Log ALL HTTP requests (including unauthenticated and failed ones)
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logAllRequests(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        HttpServletResponse response = getCurrentResponse();

        // Log request details
        String requestBody = getRequestBody(joinPoint);
        log.info("""
            >>> INCOMING REQUEST:
            Method: {} 
            URI: {} 
            Headers: {}
            Parameters: {}
            Body: {}
            Client IP: {}""",
                request.getMethod(),
                request.getRequestURI(),
                getHeadersAsString(request),
                request.getParameterMap(),
                requestBody,
                request.getRemoteAddr()
        );

        long startTime = System.currentTimeMillis();
        Object apiResponse = null;
        try {
            apiResponse = joinPoint.proceed();
            return apiResponse;
        } catch (Exception e) {
            log.error("""
                !!! REQUEST FAILED:
                Method: {} 
                URI: {}
                Error: {}""",
                    request.getMethod(),
                    request.getRequestURI(),
                    e.getMessage()
            );
            throw e;
        } finally {
            // Log response (even for failed requests)
            long duration = System.currentTimeMillis() - startTime;
            log.info("""
                <<< RESPONSE:
                Method: {} 
                URI: {}
                Status: {} 
                Time: {} ms
                Body: {}""",
                    request.getMethod(),
                    request.getRequestURI(),
                    response != null ? response.getStatus() : "N/A",
                    duration,
                    convertObjectToJson(apiResponse)
            );
        }
    }

    // Helper methods
    private HttpServletRequest getCurrentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    private HttpServletResponse getCurrentResponse() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
    }

    private String getHeadersAsString(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        request.getHeaderNames().asIterator()
                .forEachRemaining(header -> headers.append(header)
                        .append("=")
                        .append(request.getHeader(header))
                        .append("; "));
        return headers.toString();
    }

    private String getRequestBody(ProceedingJoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getArgs())
                .map(this::convertObjectToJson)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private String convertObjectToJson(Object object) {
        try {
            return object != null ?
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object) :
                    "null";
        } catch (JsonProcessingException e) {
            return "[Non-serializable object: " + object + "]";
        }
    }
}