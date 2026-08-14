package mx.gob.imss.edi.bitacora.repository;

import mx.gob.imss.edi.bitacora.models.dto.EventoCatalogoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EventoMapper {

    @Select("""
            select id_evento as id_evento, cve_evento as cve_evento
              from catalogo.edic_evento
             where cve_evento = #{cveEvento}
               and ind_activo = true
            """)
    EventoCatalogoDto buscarActivoPorClave(String cveEvento);
}
