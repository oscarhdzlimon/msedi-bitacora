package mx.gob.imss.edi.bitacora.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EventoPendienteRiesgosMapper {
    @Select("""
        select * from riesgo.edit_evento_pendiente where id_evento_pendiente=#{idEntrega} and ind_activo and stp_baja is null
        """)
    List<Map<String, Object>> consultarEntrega(@Param("idEntrega") UUID idEntrega);

}
