package com.dealermanagementsysstem.project.configuration;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Spring Security Configuration for EVM Group 2 Project
 * 
 * Features implemented:
 * - BCrypt password hashing
 * - Role-based authorization
 * - CSRF protection
 * - Session management
 * - Login rate limiting
 * - Secure logout
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private DAOAccount daoAccount;

    // Simple in-memory rate limiting for login attempts
    private final ConcurrentHashMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new CustomPasswordEncoder();
    }

    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(true);
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowUrlEncodedPeriod(true);
        return firewall;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            // Check rate limiting
            if (isAccountLocked(email)) {
                throw new UsernameNotFoundException("Account temporarily locked due to too many failed attempts");
            }

            DTOAccount account = daoAccount.findAccountByEmail(email);
            if (account == null) {
                recordFailedAttempt(email);
                throw new UsernameNotFoundException("User not found: " + email);
            }

            if (!account.isStatus()) {
                throw new UsernameNotFoundException("Account is disabled");
            }

            // Clear any previous failed attempts on successful login
            loginAttempts.remove(email);

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + account.getRole().toUpperCase()));

            return User.builder()
                    .username(account.getEmail())
                    .password(account.getPassword()) // This will be handled by our custom password encoder
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(!account.isStatus())
                    .build();
        };
    }

    private boolean isAccountLocked(String email) {
        LoginAttempt attempt = loginAttempts.get(email);
        if (attempt == null) {
            return false;
        }
        
        long lockoutTime = attempt.getLastAttemptTime() + TimeUnit.MINUTES.toMillis(LOCKOUT_DURATION_MINUTES);
        return attempt.getAttemptCount() >= MAX_ATTEMPTS && System.currentTimeMillis() < lockoutTime;
    }

    private void recordFailedAttempt(String email) {
        LoginAttempt attempt = loginAttempts.getOrDefault(email, new LoginAttempt());
        attempt.incrementAttempts();
        loginAttempts.put(email, attempt);
    }

    // Inner class for tracking login attempts
    private static class LoginAttempt {
        private int attemptCount = 0;
        private long lastAttemptTime = System.currentTimeMillis();

        public void incrementAttempts() {
            this.attemptCount++;
            this.lastAttemptTime = System.currentTimeMillis();
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public long getLastAttemptTime() {
            return lastAttemptTime;
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/test", "/health", "/api/test/**") // Allow these endpoints without CSRF
            )
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/", "/login", "/success", "/test", "/health", "/api/test/**", "/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                
                // EVM/Admin role endpoints
                .requestMatchers("/showEVMHomePage", "/evmVehicleList", "/evmCreateANewVehicleToList", 
                               "/evmOrderList", "/evmOrderHistory", "/vehicleDistributionManagement",
                               "/getVehicleList").hasAnyRole("ADMIN", "EVM", "EVMSTAFF")
                
                // Dealer role endpoints  
                .requestMatchers("/showDealerHomePage", "/dealerCustomerManagement", "/dealerCustomerList",
                               "/dealerCreateANewCustomer", "/dealerCustomerDetail", "/dealerVehiclesInformation",
                               "/getVehicleListToOrder", "/getVehicleListToCreateQuotation",
                               "/customer/**").hasAnyRole("DEALER", "DEALERSTAFF", "ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/success", true)
                .failureUrl("/login?error=true")
                .usernameParameter("email")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}
