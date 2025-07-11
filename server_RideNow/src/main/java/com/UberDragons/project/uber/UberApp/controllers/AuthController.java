package com.UberDragons.project.uber.UberApp.controllers;

import com.UberDragons.project.uber.UberApp.dto.*;
import com.UberDragons.project.uber.UberApp.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signUp(@RequestBody SignupDto signupDto) {
        System.out.println("📝 AuthController: Signup request received for: " + signupDto.getEmail());
        try {
            UserDto user = authService.signup(signupDto);
            System.out.println("✅ AuthController: Signup successful");
            return new ResponseEntity<>(user, HttpStatus.CREATED);
        } catch (Exception e) {
            System.err.println("❌ AuthController: Signup failed: " + e.getMessage());
            throw e;
        }
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/onBoardNewDriver/{userId}")
    public ResponseEntity<DriverDto> onBoardNewDriver(@PathVariable Long userId, @RequestBody OnboardDriverDto onboardDriverDto) {
        return new ResponseEntity<>(authService.onboardNewDriver(userId,
                onboardDriverDto.getVehicleId()), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto,
                                   HttpServletRequest request, HttpServletResponse response) {
        try {
            System.out.println("🔐 AuthController: Login request received for: " + loginRequestDto.getEmail());

            String[] tokens = authService.login(loginRequestDto.getEmail(), loginRequestDto.getPassword());

            Cookie cookie = new Cookie("refreshToken", tokens[1]);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);

            System.out.println("✅ AuthController: Login successful, tokens generated");
            return ResponseEntity.ok(new LoginResponseDto(tokens[0]));
        } catch (Exception e) {
            System.err.println("❌ AuthController: Login error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/user")
    public ResponseEntity<UserDto> getUserByEmail(@RequestParam String email) {
        try {
            System.out.println("🔍 AuthController: Get user request for: " + email);
            UserDto user = authService.findByEmail(email);
            System.out.println("✅ AuthController: User found");
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            System.err.println("❌ AuthController: User not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(HttpServletRequest request) {
        String refreshToken = Arrays.stream(request.getCookies()).
                filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new AuthenticationServiceException("Refresh token not found inside the Cookies"));

        String accessToken = authService.refreshToken(refreshToken);

        return ResponseEntity.ok(new LoginResponseDto(accessToken));
    }

    @GetMapping("/roles")
    public ResponseEntity<String[]> getUserRoles(@RequestParam String email) {
        UserDto user = authService.findByEmail(email);
        if (user == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(user.getRoles().toArray(new String[0]));
    }
}