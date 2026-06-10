package com.ax.template.authblueprint.user;

import com.ax.template.authblueprint.common.PublishedApi;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Published account port — the {@code user} feature's use-case surface for the {@code auth}
 * feature (DDD decomposition: retires the 4 AX-DDD-AUTH-USER service-level grandfather
 * exceptions; BACKLOG P0-1~11). The User aggregate is manipulated ONLY here: {@code auth}
 * keeps token/JWT/flow/rate-limit policy and calls these use cases instead of importing
 * {@link UserEntity}/{@link UserRepository}.
 *
 * <p>Deliberately use-case-shaped, NOT a repository mirror: credential material never
 * crosses the seam (hashing + verification live here, next to the aggregate that stores
 * the hash; callers see only {@link UserAccountDto#hasPassword()}), and every mutation is
 * a named business operation (register / markEmailVerified / resetPassword /
 * changePassword), not a generic save.
 */
@PublishedApi
@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserAccountDto> listAll() {
        return userRepository.findAll().stream().map(UserAccountDto::of).toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserAccountDto> findById(UUID id) {
        return userRepository.findById(id).map(UserAccountDto::of);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccountDto> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserAccountDto::of);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /** Password signup. The raw password is hashed HERE — the hash never crosses the seam. */
    @Transactional
    public UserAccountDto register(String email, String rawPassword, UserRole role, boolean emailVerified) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setHashedPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEmailVerified(emailVerified);
        return UserAccountDto.of(userRepository.save(user));
    }

    /** OAuth-originated account: passwordless, email pre-verified by the provider. */
    @Transactional
    public UserAccountDto registerOAuthAccount(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setRole(UserRole.MEMBER);
        user.setEmailVerified(true);
        return UserAccountDto.of(userRepository.save(user));
    }

    /**
     * Credential check — empty on unknown email, passwordless (OAuth-only) account, or
     * mismatch, indistinguishably (the caller maps all three to the same 401; ASVS
     * enumeration posture is preserved).
     */
    @Transactional(readOnly = true)
    public Optional<UserAccountDto> authenticate(String email, String rawPassword) {
        return userRepository.findByEmail(email)
            .filter(u -> u.getHashedPassword() != null
                && passwordEncoder.matches(rawPassword, u.getHashedPassword()))
            .map(UserAccountDto::of);
    }

    @Transactional
    public void markEmailVerified(UUID userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /** Token-authorized reset — the caller has already validated the RESET token. */
    @Transactional
    public void resetPassword(UUID userId, String newRawPassword) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        user.setHashedPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    /** @return false when the current password does not match (caller maps to its 401/422). */
    @Transactional
    public boolean changePassword(UUID userId, String currentRawPassword, String newRawPassword) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        if (user.getHashedPassword() == null
                || !passwordEncoder.matches(currentRawPassword, user.getHashedPassword())) {
            return false;
        }
        user.setHashedPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
        return true;
    }
}
