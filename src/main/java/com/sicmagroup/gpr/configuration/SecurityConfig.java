package com.sicmagroup.gpr.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sicmagroup.gpr.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final UserRepository userRepository;
        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                // disable csrf

                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> corsConfigurationSource())
                                .authorizeHttpRequests(registry -> registry
                                                // public endpoints
                                                .requestMatchers("/api/v1/auth/authenticate", "/api/v1/auth/essai", "/api/v1/auth/infoLicense", "/ws/**", "/api/v1/session**", "/api/v1/message/**", "/**")
                                                
                                                .permitAll()
                                                // .requestMatchers("/api/v1/auth/update",
                                                // "/api/v1/auth/update_pwd").permitAll()
                                                .requestMatchers("/api/v1/config/**").hasAnyAuthority("H12")
                                                // CLAIM
                                                .requestMatchers("/api/v1/claim/add", "/api/v1/media/download/**",
                                                                "/api/v1/claim/getFilesBy/**")
                                                .hasAnyAuthority("H1")
                                                .requestMatchers("/api/v1/claim/save_temp").hasAnyAuthority("H1")
                                                .requestMatchers(
                                                                "/api/v1/claim/unapprouvedSolution",
                                                                "/api/v1/claim/approuvedSolution")
                                                .hasAnyAuthority("H6", "DE")
                                                .requestMatchers("/api/v1/claim/affectTreatment")
                                                .hasAnyAuthority("H6", "PILOTE")
                                                .requestMatchers("/api/v1/claim/treatClaim")
                                                .hasAnyAuthority("H2", "H3", "H4")
                                                .requestMatchers("/api/v1/claim/measureSatisfaction",
                                                                "/api/v1/claim/classedClaim", "/api/v1/claim/litigate",
                                                                "/api/v1/claim/listAssuranceSatisfaction")
                                                .hasAnyAuthority("H5", "PILOTE")
                                                .requestMatchers("/api/v1/chat/**").hasAnyAuthority("H2", "H3", "H4")
                                                .requestMatchers("/api/v1/claim/list/**").hasAuthority("H1")
                                                // DENUNCIATION
                                                .requestMatchers("/api/v1/denunciation/add",
                                                                "/api/v1/media/download/**",
                                                                "/api/v1/denunciation/getFilesBy/**")
                                                .hasAnyAuthority("H1")
                                                .requestMatchers("/api/v1/denunciation/save_temp").hasAnyAuthority("H1")
                                                .requestMatchers("/api/v1/denunciation/affectTreatment")
                                                .hasAnyAuthority("H6", "PILOTE")
                                                .requestMatchers(
                                                                "/api/v1/denunciation/unapprouvedSolution",
                                                                "/api/v1/denunciation/approuvedSolution")
                                                .hasAnyAuthority("H6", "DE")
                                                .requestMatchers("/api/v1/denunciation/treatDenun/**")
                                                .hasAnyAuthority("H2", "H3", "H4")
                                                .requestMatchers("/api/v1/denunciation/list/**").hasAuthority("H1")
                                                // ALERT
                                                .requestMatchers("/api/v1/alert/**")
                                                .hasAnyAuthority("H13",  "DE", "PILOTE")
                                                // .requestMatchers("/api/v1/report/**").hasAnyAuthority("h11")
                                                // private endpoints
                                                .anyRequest()
                                                .authenticated());

                // Set session management to stateless
                http.sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        // Extract authorities from the roles claim

        // enable CORS for the rest API
        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                // TODO spécifier l'URL du serveur prod
                config.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:3001", "http://192.168.100.5:81", "http://192.168.100.5",
                "http://localhost:9195", "http://localhost:8080", "http://196.168.30.157:81", "https://196.168.30.157", "http://196.168.30.157", "https://196.168.30.157:81", "https://196.168.30.157:443", "https://196.168.30.157:444",
                 "http://localhost:81", "https://gpsassilassime.sicmagroup.com", "http://192.168.100.51:21465"));
                // config.setAllowedOrigins(Arrays.asList("https://gprfigec.gprserver.com", "https://gpr-figec:9001"));
                // config.setAllowedOrigins(Arrays.asList("*"));

                config.setAllowedMethods(Arrays.asList("*"));
                config.setAllowedHeaders(Arrays.asList("*"));
                config.setAllowCredentials(true);
                // config.addAllowedOrigin("/**");
             
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }

}
