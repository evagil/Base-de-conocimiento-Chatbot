package com.municipio.base_conocimiento.rag;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.municipio.base_conocimiento.agente.*;
import com.municipio.base_conocimiento.agente.HerramientasAgenteRepository.*;
import com.municipio.base_conocimiento.ia.client.ClienteConversacion;
import com.municipio.base_conocimiento.ia.dto.RespuestaIa;
import com.municipio.base_conocimiento.rag.service.*;
import java.time.Duration;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

class AgenteFixtures {
 final BusquedaHibridaRag busqueda=mock(BusquedaHibridaRag.class);
 final VigenciaFuentesRag vigencia=mock(VigenciaFuentesRag.class);
 final HerramientasAgenteRepository repo=mock(HerramientasAgenteRepository.class);
 final ClienteConversacion cliente=mock(ClienteConversacion.class);
 final Solucion solucion=new Solucion(8,7,"Recuperar E17",ConsultaRagFixtures.EVIDENCIA,true,0);
 final List<Paso> pasos=List.of(new Paso(9,8,"Cerrar","Cerrar la aplicación.",1),new Paso(10,8,"Abrir","Volver a abrir la aplicación.",3));
 final ConfiguracionAgente config=new ConfiguracionAgente(true,6,Duration.ofSeconds(600));
 AgenteFixtures(){
  when(busqueda.buscar(anyString())).thenReturn(new BusquedaHibridaRag.Seleccion("COINCIDENCIAS","HIBRIDA",false,List.of(new BusquedaHibridaRag.Coincidencia(ConsultaRagFixtures.registro(1,ConsultaRagFixtures.EVIDENCIA,null),1))));
  when(vigencia.todas(anyList())).thenReturn(true);when(repo.soluciones(7)).thenReturn(List.of(solucion));when(repo.pasos(8)).thenReturn(pasos);
 }
 AgenteConsulta servicio(){return new AgenteConsulta(config,ConsultaRagFixtures.config(true,true),ConsultaRagFixtures.ia(),busqueda,vigencia,repo,cliente,new ObjectMapper());}
 RespuestaIa accion(String herramienta,long id){return salida("{\"herramienta\":\""+herramienta+"\",\"id\":"+id+"}");}
 RespuestaIa salida(String texto){return new RespuestaIa("granite4.2:3b",texto,1,10);}
 void cadena(){when(cliente.conversar(any(),anyList(),anyMap())).thenReturn(accion("obtener_solucion",7),accion("obtener_pasos",8));}
}
