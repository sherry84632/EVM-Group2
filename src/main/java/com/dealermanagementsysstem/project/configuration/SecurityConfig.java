package com.dealermanagementsysstem.project.configuration;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

            if (!account.isActive()) {
                throw new UsernameNotFoundException("Account is disabled");
            }

            // Clear any previous failed attempts on successful login
            loginAttempts.remove(email);

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + account.getRole()));

            return User.builder()
                    .username(account.getEmail())
                    .password(account.getPassword()) // This will be handled by our custom password encoder
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(!account.isActive())
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
                // Use default HttpSession-based CSRF token repository for better stability
                // This avoids cookie sync issues and token expiration problems
                .ignoringRequestMatchers("/test", "/health", "/api/test/**", "/evm/vehicle/create")
            )
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/", "/login", "/success", "/test", "/health", "/api/test/**", "/css/**", "/js/**", "/images/**", "/static/**", "/scripts/**").permitAll()

                // Public read-only access for vehicle browsing and quotation entry point
                .requestMatchers(HttpMethod.GET, "/evm/vehicle/list", "/evm/vehicle/detail/**", "/evm/vehicle/showImage/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/quotation/new").permitAll()
                .requestMatchers(HttpMethod.POST, "/quotation/save").permitAll()
                .requestMatchers(HttpMethod.GET, "/quotation/preview/**").permitAll()
                
                // EVM/Admin role endpoints
                .requestMatchers("/showEVMHomePage", "/evmVehicleList", "/evmCreateANewVehicleToList", 
                               "/evmOrderList", "/evmOrderHistory", "/vehicleDistributionManagement",
                               "/getVehicleList").hasAnyRole("ADMIN", "EVM", "EVMSTAFF")
                
                // Account Management (ADMIN and EVMSTAFF only)
                .requestMatchers("/account/**").hasAnyRole("ADMIN", "EVMSTAFF")

                // EVM Vehicle management endpoints
                .requestMatchers(HttpMethod.GET, "/evm/vehicle/create").hasAnyRole("ADMIN", "EVM", "EVMSTAFF")
                .requestMatchers("/evm/vehicle/create", "/evm/vehicle/edit/**", "/evm/vehicle/delete/**").hasAnyRole("ADMIN", "EVM", "EVMSTAFF")
                
                // Dealer role endpoints  
                .requestMatchers("/showDealerHomePage", "/dealerCustomerManagement", "/betterCustomerListFinal",
                               "/dealerCreateANewCustomer", "/dealerCustomerDetail", "/dealerVehiclesInformation",
                               "/getVehicleListToOrder", "/getVehicleListToCreateQuotation",
                               "/customer/**").hasAnyRole("DEALER", "DEALERSTAFF", "ADMIN")

                // Quotation management (authenticated dealer/admin for actions beyond opening form)
                .requestMatchers("/quotation/list", "/quotation/detail/**", "/quotation/approve/**", "/quotation/reject/**").hasAnyRole("DEALER", "DEALERSTAFF", "ADMIN")
                
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

    // REMOVED: Duplicate configure method that was causing CSRF conflicts

}
