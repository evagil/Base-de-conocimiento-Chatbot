package com.municipio.base_conocimiento.seguridad.config;
import javax.crypto.SecretKey;import javax.crypto.spec.SecretKeySpec;import org.springframework.context.annotation.*;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.security.oauth2.jose.jws.MacAlgorithm;import org.springframework.security.oauth2.jwt.*;import org.springframework.security.web.SecurityFilterChain;
@Configuration @Profile("test") public class TestProfileSecurityConfiguration{
 @Bean PasswordEncoder testPasswordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecretKey testJwtKey(){return new SecretKeySpec(new byte[32],"HmacSHA256");}
 @Bean JwtEncoder testJwtEncoder(SecretKey key){return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();}
 @Bean SecurityFilterChain testSecurity(HttpSecurity http)throws Exception{return http.csrf(c->c.disable()).authorizeHttpRequests(a->a.anyRequest().permitAll()).build();}
}
