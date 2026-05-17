package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "userId", u.getId().toString(),
                        "email", u.getEmail(),
                        "role", u.getRole().name(),
                        "emailVerified", u.isEmailVerified()
                ))
                .collect(Collectors.toList());
    }
}
