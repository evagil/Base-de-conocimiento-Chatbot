package com.municipio.base_conocimiento.seguridad.service;
import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.stereotype.Component;
@Component public class SeguridadActual{public boolean esLector(){var auth=SecurityContextHolder.getContext().getAuthentication();return auth!=null&&auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_LECTOR"));}}
