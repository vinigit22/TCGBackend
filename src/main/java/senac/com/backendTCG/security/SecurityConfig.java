package senac.com.backendTCG.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // Consultas abertas ao publico (vitrine de torneios, eventos, lojas e jogadores) e imagens enviadas.
    // /inscricoes fica de fora: tem status de pagamento e so o jogador, a equipe da loja e o admin veem.
    // /jogadores/me e publico aqui, mas responde 401 sem token (exige a conta logada).
    private static final String[] GET_PUBLICOS = {
            "/jogos/**",
            "/formatos/**",
            "/lojas/**",
            "/jogadores/**",
            "/enderecos/**",
            "/eventos/**",
            "/evento-participacoes/**",
            "/torneios/**",
            "/rodadas/**",
            "/partidas/**",
            "/games/**",
            "/torneio-resultados/**",
            "/uploads/**"
    };

    // Cadastro, login e recuperacao de conta
    private static final String[] POST_PUBLICOS = {
            "/auth/login",
            "/auth/registro/**",
            "/auth/esqueci-senha",
            "/auth/redefinir-senha",
            "/jogadores",
            "/lojas"
    };

    private final JwtFilter jwtFilter;

    // So o perfil h2 liga o console; nos outros perfis a rota nem e liberada
    @Value("${spring.h2.console.enabled:false}")
    private boolean consoleH2;

    @Value("${app.cors.origens}")
    private String[] origensCors;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .headers(headers -> {
                    // O console do H2 usa frames da mesma origem
                    if (consoleH2) {
                        headers.frameOptions(frame -> frame.sameOrigin());
                    }
                })
                .authorizeHttpRequests(auth -> {
                    // Preflight do navegador
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers("/error").permitAll()
                            .requestMatchers(HttpMethod.POST, POST_PUBLICOS).permitAll()
                            .requestMatchers(HttpMethod.GET, GET_PUBLICOS).permitAll();

                    if (consoleH2) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }

                    // O resto exige token; as regras de dono/equipe/admin ficam nos services
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Origens aceitas vem de app.cors.origens (variavel CORS_ORIGENS)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(origensCors));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
