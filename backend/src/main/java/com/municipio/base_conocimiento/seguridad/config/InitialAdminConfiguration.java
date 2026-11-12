package com.municipio.base_conocimiento.seguridad.config;
import com.municipio.base_conocimiento.seguridad.entity.*;
import com.municipio.base_conocimiento.seguridad.repository.UsuarioRepository;
import com.municipio.base_conocimiento.seguridad.service.AuthService;
import org.springframework.beans.factory.annotation.Value;import org.springframework.boot.CommandLineRunner;import org.springframework.context.annotation.*;import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration @Profile("!test")
public class InitialAdminConfiguration{
 @Bean CommandLineRunner initialAdmin(UsuarioRepository usuarios,PasswordEncoder encoder,@Value("${app.security.initial-admin-email}") String email,@Value("${app.security.initial-admin-user}") String user,@Value("${app.security.initial-admin-password}") String password){return args->{String normalizedEmail=AuthService.normalizar(email);String normalizedUser=AuthService.normalizarUsuario(user);if(usuarios.findByUsuarioNormalizado(normalizedUser).isEmpty()&&usuarios.findByEmailNormalizado(normalizedEmail).isEmpty())usuarios.save(new Usuario("Administrador",user,normalizedUser,email,normalizedEmail,encoder.encode(password),Rol.ADMINISTRADOR));};}
}
