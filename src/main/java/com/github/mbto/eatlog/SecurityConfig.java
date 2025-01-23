package com.github.mbto.eatlog;
/*
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;

@Configuration
@EnableWebSecurity(debug = true)
@EnableMethodSecurity(prePostEnabled = false)
public class SecurityConfig {
//    @Bean
//    AuthenticationManager authenticationManager(AuthenticationManagerBuilder builder) throws Exception {
//        return builder
//                .authenticationProvider(new RememberMeAuthenticationProvider("somekey"))
//                .build();
//    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        /*http
//            .formLogin()
//                .loginPage("/login.xhtml")
//                .defaultSuccessUrl("/", true)
//                .failureUrl("/login.xhtml?failed")
//        .and()
//            .logout()
//                .logoutUrl("/logout")
//                .logoutSuccessUrl("/login.xhtml")
//                .invalidateHttpSession(true)
//        .and()
//            .rememberMe()
//                .userDetailsService(UserDetailsService)
//                .tokenRepository(new InMemoryTokenRepositoryImpl())
//                .alwaysRemember(true)
//        .and()
//            .x509().disable()
//            .cors().disable()
//            .csrf().disable()
//            .jee().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        .and().authorizeHttpRequests()
            .requestMatchers("/css/**","/images/**","/jakarta.faces.resource/**","/403.html").permitAll()
//            .antMatchers("/brokers**","/editBroker**","/games**","/managers**","/new**").hasRole("broker")
//            .antMatchers("/editProfile**","/editProject**","/identities**","/","/index**","/portsByProject**","/projects**").hasAnyRole("broker","project")
//            .antMatchers("/","/index**").hasAnyRole("project")
            .requestMatchers("/test").permitAll()
            .requestMatchers("/login.xhtml").permitAll()
            .requestMatchers("/login**").permitAll()
//            .anyRequest().denyAll()
        .and()
            .exceptionHandling()
            .accessDeniedPage("/403.html")
        ;*/
        /*return http.authorizeHttpRequests()
                .requestMatchers(HttpMethod.GET).permitAll()
                .requestMatchers(HttpMethod.POST).permitAll()
                .anyRequest().permitAll()
                .and().build();
    }
//    @Autowired
//    private PasswordEncoder bcryptPasswordEncoder;
    //    @Autowired
//    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//        auth.userDetailsService(accountUserDetailService)
//                .passwordEncoder(bcryptPasswordEncoder);
//    }
}*/