package ai.synalix.synalixai.config;

import ai.synalix.synalixai.enums.UserRole;
import ai.synalix.synalixai.service.UserService;
import ai.synalix.synalixai.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT Authentication Filter
 * This filter intercepts HTTP requests and validates JWT tokens from Authorization header
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final var authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        // Skip if no Authorization header or not a Bearer token
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract token from Authorization header
            final var jwt = authorizationHeader.substring(BEARER_PREFIX.length());

            // Validate token format and expiration
            if (!jwtUtil.validateToken(jwt)) {
                logger.debug("Invalid or expired JWT token");
                filterChain.doFilter(request, response);
                return;
            }

            // Check if token is access token
            if (!jwtUtil.isAccessToken(jwt)) {
                logger.debug("Token is not an access token");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract user information from token
            final var username = jwtUtil.extractUsername(jwt);
            final var userIdStr = jwtUtil.extractUserId(jwt);
            final var userRole = jwtUtil.extractUserRole(jwt);
            final var userStatus = jwtUtil.extractUserStatus(jwt);

            // Validate extracted information
            if (username == null || userIdStr == null || userRole == null || userStatus == null) {
                logger.debug("Missing required claims in JWT token");
                filterChain.doFilter(request, response);
                return;
            }

            // Check if user is already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Verify user still exists and is active
            try {
                final var userId = UUID.fromString(userIdStr);
                final var user = userService.getUserById(userId);

                // Validate token against user
                if (!jwtUtil.validateToken(jwt, user)) {
                    logger.debug("Token validation failed for user: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Create authentication principal with user information
                JwtUserPrincipal userPrincipal = new JwtUserPrincipal(
                        userId,
                        username,
                        userRole.equals("USER") ? UserRole.USER : UserRole.ADMIN,
                        userStatus
                );

                // Create authorities based on user role
                var authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + userRole)
                );

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        authorities
                );

                // Set authentication details
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);

                logger.debug("Successfully authenticated user: {} with role: {}", username, userRole);

            } catch (Exception e) {
                logger.debug("Error validating user from token: {}", e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

}