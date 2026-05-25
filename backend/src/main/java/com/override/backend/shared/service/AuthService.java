package com.override.backend.shared.service;

import com.override.backend.dto.AuthResponse;
import com.override.backend.dto.LoginRequest;
import com.override.backend.dto.RegisterRequest;
import com.override.backend.entity.PlayerProfile;
import com.override.backend.entity.User;
import com.override.backend.repository.PlayerProfileRepository;
import com.override.backend.repository.UserRepository;
import com.override.backend.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PlayerProfileRepository profileRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepo,
                       PlayerProfileRepository profileRepo,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authManager,
                       JwtUtils jwtUtils) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(
                req.getUsername(),
                req.getEmail(),
                passwordEncoder.encode(req.getPassword())
        );
        userRepo.save(user);

        // Create default player profile
        PlayerProfile profile = new PlayerProfile(user);
        profileRepo.save(profile);

        String token = jwtUtils.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        String token = jwtUtils.generateToken(req.getUsername());
        return new AuthResponse(token, req.getUsername());
    }
}
