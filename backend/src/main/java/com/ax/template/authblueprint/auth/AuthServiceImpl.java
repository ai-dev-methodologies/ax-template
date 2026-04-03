package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import com.ax.template.authblueprint.user.UserRepository;
import com.ax.template.authblueprint.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public SignupResponse signup(SignupRequest request) {
        String verificationToken = UUID.randomUUID().toString();

        if (!userRepository.existsByEmail(request.getEmail())) {
            UserEntity user = new UserEntity();
            user.setEmail(request.getEmail());
            user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(UserRole.MEMBER);
            user.setEmailVerified(false);
            UserEntity saved = userRepository.save(user);

            System.out.println("[AUTH-TOKEN] type=VERIFY email=" + request.getEmail() + " token=" + verificationToken);

            return new SignupResponse(saved.getId().toString(), "Signup successful. Check your email for verification.");
        }

        System.out.println("[AUTH-TOKEN] type=VERIFY email=" + request.getEmail() + " token=" + verificationToken);
        return new SignupResponse(UUID.randomUUID().toString(), "Signup successful. Check your email for verification.");
    }
}
