package mx.gob.imss.edi.bitacora.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MensajeMapper {

    @Select("""
            select exists(
                select 1
                  from catalogo.edic_mensaje
                 where cve_mensaje = #{cveMensaje}
                   and ind_activo = true
            )
            """)
    boolean existeActivo(String cveMensaje);

    @Select("""
            select id_mensaje
              from catalogo.edic_mensaje
             where cve_mensaje = #{cveMensaje}
               and ind_activo = true
            """)
    Long idActivoPorClave(String cveMensaje);
}
