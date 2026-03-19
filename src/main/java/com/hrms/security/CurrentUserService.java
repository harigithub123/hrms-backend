package com.hrms.security;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves the logged-in {@link User} from the DB. Supports both principal types used by our JWT filter:
     * {@link UserDetails} (legacy) and {@link String} username (current), since
     * {@code UsernamePasswordAuthenticationToken} may use the username string as principal.
     */
    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Not authenticated");
        }
        String username;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails details) {
            username = details.getUsername();
        } else if (principal instanceof String s) {
            username = s;
        } else {
            throw new IllegalStateException("Not authenticated");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
