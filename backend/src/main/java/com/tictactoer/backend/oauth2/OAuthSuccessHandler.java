package com.tictactoer.backend.oauth2;

import com.tictactoer.backend.player.PlayerEntity;
import com.tictactoer.backend.security.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

/**
 * Called by Spring Security after a successful OAuth2 authorization.
 *
 * Flow:
 *  1. Extract the authenticated OAuth2User from the token.
 *  2. Delegate to OAuthService to find/create/link the PlayerEntity.
 *  3. Generate a JWT using the existing JwtService.
 *  4. Redirect the browser to the frontend /oauth/success page with
 *     the token as a query parameter (the frontend stores it in localStorage).
 *
 * Security note: the token is short-lived (24 h) and transmitted over HTTPS
 * in production. The frontend page must consume and remove the token from the
 * URL immediately after reading it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthService oAuthService;
    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        try {
            PlayerEntity player = oAuthService.processOAuthUser(oauthUser, registrationId);
            String jwt = jwtService.generateToken(player.getEmail());

            String redirectUrl = UriComponentsBuilder
                    .fromUri(URI.create(frontendUrl))
                    .path("/oauth/success")
                    .queryParam("token", jwt)
                    .build()
                    .toUriString();

            log.info("OAuth login successful for player_id={}. Redirecting to frontend.", player.getId());
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth post-processing failed for provider={}: {}", registrationId, e.getMessage());
            String errorRedirect = UriComponentsBuilder
                    .fromUri(URI.create(frontendUrl))
                    .path("/login")
                    .queryParam("error", "oauth_failed")
                    .build()
                    .toUriString();
            response.sendRedirect(errorRedirect);
        }
    }
}
