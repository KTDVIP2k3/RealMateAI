package com.GSU26SE22_SU26SE002.RealMateAI.config;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    JwtFilterConfig jwtFilter;

    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return  new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(customizer -> customizer.disable())
                .cors(cors -> cors.configure(httpSecurity))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(
                                "/auth/login",
                                "/auth/verify-login",
                                "/auth/activate-account",
                                "/auth/register",
                                "/auth/verify-otp",
                                "/auth/send-otp",
                                "/auth/forgot-password",
                                "/auth/new-password"
                        ).permitAll()
                        .requestMatchers(
                                "/provinces",
                                "/wards",
                                "/property-types",
                                "/property-conditions",
                                "/strategies",
                                "/api/chat",
                                "/error",
                                "/posting-packages/active",
                                "/membership-plans/active"
                        ).permitAll()
                        .requestMatchers(
                                "/wallets/deposit/success",
                                "/wallets/deposit/cancel",
                                "/wallets/deposit/webhook"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/listings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/posting-packages/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/membership-plans/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/media/thumbnail").permitAll()
                        .requestMatchers(
                                "/locations/**",
                                "/news/**",
                                "/news-categories/**",
                                "/ward-boundary/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .formLogin(customizer -> customizer.disable())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(){
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        daoAuthenticationProvider.setHideUserNotFoundExceptions(false);
        return daoAuthenticationProvider;
    }
}
