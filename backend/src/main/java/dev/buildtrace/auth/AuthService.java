package dev.buildtrace.auth;

import dev.buildtrace.generation.ShowcaseProject;
import dev.buildtrace.project.ProjectService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ProjectService projectService;
    private final ShowcaseProject showcaseProject;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        ProjectService projectService,
        ShowcaseProject showcaseProject
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.projectService = projectService;
        this.showcaseProject = showcaseProject;
    }

    @Transactional
    public AuthController.AuthResponse register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("该邮箱已注册");
        }
        UserEntity user = new UserEntity(
            UUID.randomUUID().toString(), normalizedEmail, passwordEncoder.encode(password), Instant.now());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("该邮箱已注册", exception);
        }
        projectService.createShowcase(user.getId(), showcaseProject.files());
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthController.AuthResponse login(String email, String password) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new IllegalArgumentException("邮箱或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("邮箱或密码错误");
        }
        return response(user);
    }

    private AuthController.AuthResponse response(UserEntity user) {
        return new AuthController.AuthResponse(
            jwtService.issue(user), new AuthController.UserResponse(user.getId(), user.getEmail(), user.getCreatedAt()));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
