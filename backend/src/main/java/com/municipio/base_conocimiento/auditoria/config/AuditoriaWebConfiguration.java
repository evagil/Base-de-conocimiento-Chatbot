package com.municipio.base_conocimiento.auditoria.config;
import org.springframework.context.annotation.Configuration;import org.springframework.web.servlet.config.annotation.*;
@Configuration public class AuditoriaWebConfiguration implements WebMvcConfigurer{private final AuditoriaInterceptor interceptor;public AuditoriaWebConfiguration(AuditoriaInterceptor interceptor){this.interceptor=interceptor;}@Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(interceptor).addPathPatterns("/api/**");}}
