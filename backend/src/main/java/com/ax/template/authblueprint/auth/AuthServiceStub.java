package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.VerificationState;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceStub implements AuthService {

    @Override
    public AuthStateView getAuthStatePlaceholder() {
        return new AuthStateView(
            null,
            null,
            List.of(),
            VerificationState.UNVERIFIED,
            List.of()
        );
    }
}
