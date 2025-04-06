package com.example.products.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationService service;

  @CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(
      @RequestBody RegisterRequest request,
      HttpServletResponse response
  ) {
    //return ResponseEntity.ok(service.register(request));
    AuthenticationResponse registerResponse = service.register(request);

    // Create HTTP-only secure cookie
    Cookie accessTokenCookie = new Cookie("access_token", registerResponse.getAccessToken());
    accessTokenCookie.setHttpOnly(false); // Prevent XSS attacks
    accessTokenCookie.setSecure(false); // Only send over HTTPS
    accessTokenCookie.setPath("/"); // Available to all paths
    accessTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 1 week expiry

    // Add SameSite attribute (via response header)
    response.setHeader("Set-Cookie",
            "access_token=" + registerResponse.getAccessToken() +
                    "; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=" + (7 * 24 * 60 * 60));

    response.addCookie(accessTokenCookie);

    // Return response body without sensitive tokens
    return ResponseEntity.ok(registerResponse);
  }

  @CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
  @PostMapping("/authenticate")
  public ResponseEntity<AuthenticationResponse> authenticate(
      @RequestBody AuthenticationRequest request,
      HttpServletResponse response
  ) {
    //return ResponseEntity.ok(service.authenticate(request));
    AuthenticationResponse authResponse = service.authenticate(request);

    // Create HTTP-only secure cookie
    Cookie accessTokenCookie = new Cookie("access_token", authResponse.getAccessToken());
    accessTokenCookie.setHttpOnly(false); // Prevent XSS attacks
    accessTokenCookie.setSecure(false); // Only send over HTTPS
    accessTokenCookie.setPath("/"); // Available to all paths
    accessTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 1 week expiry

    // Add SameSite attribute (via response header)
    response.setHeader("Set-Cookie",
            "access_token=" + authResponse.getAccessToken() +
                    "; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=" + (7 * 24 * 60 * 60));

    response.addCookie(accessTokenCookie);

    // Return response body without sensitive tokens
    return ResponseEntity.ok(authResponse);
  }

  @CrossOrigin(origins = "http://localhost:3000")
  @PostMapping("/refresh-token")
  public void refreshToken(
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    service.refreshToken(request, response);
  }

}
