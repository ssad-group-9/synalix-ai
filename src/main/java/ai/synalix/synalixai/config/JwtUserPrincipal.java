package ai.synalix.synalixai.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

/**
 * Custom UserDetails implementation for JWT authentication
 */
@AllArgsConstructor
public class JwtUserPrincipal implements UserDetails {
    @Getter
    private final UUID id;
    private final String username;
    @Getter
    private final String role;
    @Getter
    private final String status;

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return null; // Password not needed for JWT authentication
    }

    @Override
    public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return "ENABLED".equals(status);
    }

    @Override
    public boolean isAccountNonLocked() {
        return "ENABLED".equals(status);
    }

    @Override
    public boolean isEnabled() {
        return "ENABLED".equals(status);
    }
}
