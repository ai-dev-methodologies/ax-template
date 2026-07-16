package com.ax.template.authblueprint.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserEntityTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByEmail_persistsAllFields() {
        UserEntity user = new UserEntity();
        user.setEmail("test@example.com");
        user.setHashedPassword("$2a$10$hashedpassword");
        user.setRole(UserRole.MEMBER);
        user.setEmailVerified(false);

        userRepository.save(user);

        Optional<UserEntity> found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getRole()).isEqualTo(UserRole.MEMBER);
        assertThat(found.get().isEmailVerified()).isFalse();
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void emailIsUnique_throwsOnDuplicate() {
        UserEntity user1 = new UserEntity();
        user1.setEmail("dup@example.com");
        user1.setHashedPassword("hash1");
        user1.setRole(UserRole.MEMBER);
        userRepository.save(user1);

        UserEntity user2 = new UserEntity();
        user2.setEmail("dup@example.com");
        user2.setHashedPassword("hash2");
        user2.setRole(UserRole.MEMBER);

        org.junit.jupiter.api.Assertions.assertThrows(
            Exception.class,
            () -> userRepository.saveAndFlush(user2)
        );
    }
}
