package com.municipio.base_conocimiento.seguridad.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!test")
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean SecretKey jwtKey(@Value("${app.security.jwt-secret}") String secret){try{return new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)),"HmacSHA256");}catch(Exception e){throw new IllegalStateException(e);}}
    @Bean JwtEncoder jwtEncoder(SecretKey key){return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();}
    @Bean JwtDecoder jwtDecoder(SecretKey key){return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();}
    @Bean JwtAuthenticationConverter jwtAuthenticationConverter(){JwtGrantedAuthoritiesConverter roles=new JwtGrantedAuthoritiesConverter();roles.setAuthoritiesClaimName("rol");roles.setAuthorityPrefix("ROLE_");JwtAuthenticationConverter converter=new JwtAuthenticationConverter();converter.setJwtGrantedAuthoritiesConverter(roles);return converter;}
    @Bean SecurityFilterChain security(HttpSecurity http,JwtAuthenticationConverter converter) throws Exception{
        http.csrf(csrf->csrf.disable()).cors(Customizer.withDefaults()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(a->a
            .requestMatchers("/api/auth/login","/api/auth/refresh","/swagger-ui/**","/v3/api-docs/**").permitAll()
            .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
            .requestMatchers("/api/auditoria/**").hasRole("ADMINISTRADOR")
            .requestMatchers("/api/ia/**").hasRole("ADMINISTRADOR")
            .requestMatchers(HttpMethod.POST,"/api/documentos/extraer").hasAnyRole("ADMINISTRADOR","EDITOR","LECTOR")
            .requestMatchers(HttpMethod.POST,"/api/areas/**").hasRole("ADMINISTRADOR")
            .requestMatchers(HttpMethod.PUT,"/api/areas/**","/api/subareas/**").hasRole("ADMINISTRADOR")
            .requestMatchers(HttpMethod.PATCH,"/api/areas/**","/api/subareas/**").hasRole("ADMINISTRADOR")
            .requestMatchers(HttpMethod.POST,"/api/problemas/**","/api/soluciones/**","/api/pasos/**").hasAnyRole("ADMINISTRADOR","EDITOR")
            .requestMatchers(HttpMethod.PUT,"/api/problemas/**","/api/soluciones/**","/api/pasos/**","/api/archivos/**").hasAnyRole("ADMINISTRADOR","EDITOR")
            .requestMatchers(HttpMethod.PATCH,"/api/problemas/**","/api/soluciones/**","/api/pasos/**","/api/archivos/**").hasAnyRole("ADMINISTRADOR","EDITOR")
            .requestMatchers(HttpMethod.GET,"/api/**").authenticated()
            .anyRequest().authenticated())
          .oauth2ResourceServer(o->o.jwt(jwt->jwt.jwtAuthenticationConverter(converter)));
        return http.build();
    }
}
