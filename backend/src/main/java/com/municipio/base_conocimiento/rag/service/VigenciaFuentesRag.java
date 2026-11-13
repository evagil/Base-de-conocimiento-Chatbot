package com.municipio.base_conocimiento.rag.service;

import com.municipio.base_conocimiento.rag.repository.*;
import com.municipio.base_conocimiento.rag.repository.BusquedaRagRepository.Registro;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

@Component
public class VigenciaFuentesRag {
    private final FuentesIndiceRepository fuentes;
    private final BusquedaRagRepository busqueda;
    private final IndexadorConocimiento indexador;
    public VigenciaFuentesRag(FuentesIndiceRepository fuentes,BusquedaRagRepository busqueda,IndexadorConocimiento indexador){
        this.fuentes=fuentes;this.busqueda=busqueda;this.indexador=indexador;
    }
    public boolean comprobar(Registro registro,Map<Long,String> huellas){
        try{
            String actual=huellas.computeIfAbsent(registro.problemaId(),id->{var snapshot=fuentes.obtener(id);return snapshot.visible()?indexador.huella(snapshot):"";});
            if(!actual.equals(registro.huella()))return false;
            return busqueda.vigente(registro.id()).filter(r->r.huella().equals(actual) && r.texto().equals(registro.texto())
                    && r.problemaId()==registro.problemaId() && r.tipo().equals(registro.tipo()) && r.origenId()==registro.origenId()).isPresent();
        }catch(EmptyResultDataAccessException|IllegalArgumentException e){huellas.put(registro.problemaId(),"");return false;}
    }
    public boolean todas(List<Registro> registros){Map<Long,String> huellas=new HashMap<>();return registros.stream().allMatch(r->comprobar(r,huellas));}
}
