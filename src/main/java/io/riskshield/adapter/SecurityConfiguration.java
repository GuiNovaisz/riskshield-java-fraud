package io.riskshield.adapter;

import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
public class SecurityConfiguration {
    @Bean UserDetailsService users(Environment env) {
        var encoder=new BCryptPasswordEncoder();
        return new InMemoryUserDetailsManager(
            user("engine","ENGINE",env,encoder),user("analyst","ANALYST",env,encoder),user("auditor","AUDITOR",env,encoder),
            user("admin-a","ADMIN",env,encoder),user("admin-b","ADMIN",env,encoder));
    }
    private UserDetails user(String name,String role,Environment env,BCryptPasswordEncoder encoder) {
        String password=env.getRequiredProperty("risk.users."+name);
        if(password.length()<20) throw new IllegalStateException("each deployment password must contain at least 20 characters");
        return User.withUsername(name).password("{bcrypt}"+encoder.encode(password)).roles(role).build();
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers("/actuator/health/**","/actuator/prometheus").permitAll()
                .requestMatchers("/risk/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,"/risk/evaluations").hasRole("ENGINE")
                .requestMatchers(HttpMethod.POST,"/risk/feedback").hasRole("ANALYST")
                .requestMatchers(HttpMethod.GET,"/risk/reviews").hasRole("ANALYST")
                .requestMatchers(HttpMethod.GET,"/risk/decisions/**").hasAnyRole("ANALYST","AUDITOR")
                .requestMatchers(HttpMethod.GET,"/risk/audit/**").hasRole("AUDITOR")
                .anyRequest().denyAll())
            .httpBasic(Customizer.withDefaults())
            .exceptionHandling(e->e
                .authenticationEntryPoint((req,res,ex)->problem(res,401,"Authentication required"))
                .accessDeniedHandler((req,res,ex)->problem(res,403,"Access denied")))
            .build();
    }
    static void problem(jakarta.servlet.http.HttpServletResponse response,int status,String title) throws java.io.IOException {
        response.setStatus(status); response.setContentType("application/problem+json");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\""+title+"\",\"status\":"+status+"}");
    }
}
