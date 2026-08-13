package mx.gob.imss.edi.bitacora.repository;

import mx.gob.imss.edi.bitacora.models.dto.TransaccionContextoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TransaccionConsultaMapper {

    @Select("""
            select t.id_transaccion as id_transaccion,
                   t.id_sistema_origen as id_sistema_origen,
                   s.cve_sistema_origen as cve_sistema_origen,
                   t.cve_transaccion as cve_transaccion
              from trazabilidad.transaccion t
              join catalogo.sistema_origen s on s.id_sistema_origen = t.id_sistema_origen
             where t.id_transaccion = #{idTransaccion}
            """)
    TransaccionContextoDto buscarContexto(Long idTransaccion);
}
