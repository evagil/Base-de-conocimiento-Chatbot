package com.municipio.base_conocimiento.areas.mapper;

import com.municipio.base_conocimiento.areas.dto.AreaResponse;
import com.municipio.base_conocimiento.areas.dto.SubareaResponse;
import com.municipio.base_conocimiento.areas.entity.Area;
import com.municipio.base_conocimiento.areas.entity.Subarea;

public final class AreaMapper {
    private AreaMapper() {}

    public static AreaResponse response(Area area) {
        return new AreaResponse(area.getId(), area.getNombre(), area.getDescripcion(), area.getOrden(),
                area.isActivo(), area.getCreadoEn(), area.getModificadoEn(), area.getVersion());
    }

    public static SubareaResponse response(Subarea subarea) {
        return new SubareaResponse(subarea.getId(), subarea.getArea().getId(), subarea.getArea().getNombre(),
                subarea.getNombre(), subarea.getDescripcion(), subarea.getOrden(), subarea.isActivo(),
                subarea.getCreadoEn(), subarea.getModificadoEn(), subarea.getVersion());
    }
}
