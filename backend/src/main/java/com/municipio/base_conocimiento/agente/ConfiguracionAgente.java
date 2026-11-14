package com.municipio.base_conocimiento.agente;
import java.time.Duration;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("app.agente")
public record ConfiguracionAgente(boolean habilitado,int maxIteraciones,Duration timeout) {
 public ConfiguracionAgente {
  if(maxIteraciones<1 || maxIteraciones>10 || timeout==null || timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofMinutes(15))>0)
   throw new IllegalArgumentException("Límites del agente inválidos.");
 }
 @Configuration @EnableConfigurationProperties(ConfiguracionAgente.class) public static class Registro {}
}
