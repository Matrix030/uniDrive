package edu.nyu.unidrive.server.controller;

import edu.nyu.unidrive.common.dto.ApiResponse;
import edu.nyu.unidrive.common.dto.LoginRequest;
import edu.nyu.unidrive.common.dto.LoginResponse;
import edu.nyu.unidrive.server.model.UserRole;
import edu.nyu.unidrive.server.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("userId is required."));
        }
        if (!UserRole.isValid(request.role())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("role must be STUDENT or INSTRUCTOR."));
        }

        UserRepository.StoredUser user = userRepository.findById(request.userId())
            .orElseGet(() -> {
                userRepository.save(request.userId(), request.userId(), request.role());
                return new UserRepository.StoredUser(request.userId(), request.userId(), request.role());
            });

        LoginResponse response = new LoginResponse(user.id(), user.name(), user.role());
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful."));
    }
}
