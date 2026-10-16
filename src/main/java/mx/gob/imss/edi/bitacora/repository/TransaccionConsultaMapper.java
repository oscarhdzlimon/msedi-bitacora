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
              from trazabilidad.edit_transaccion t
              join catalogo.edic_sistema_origen s on s.id_sistema_origen = t.id_sistema_origen
             where t.id_transaccion = #{idTransaccion}
            """)
    TransaccionContextoDto buscarContexto(Long idTransaccion);
    @Select("""
            select a.ref_payload from integracion.edit_solicitud_acceso_edi a
             where a.id_transaccion = #{idTransaccion} and a.ind_activo = true
               and a.cve_estatus = 'USADA' and a.ind_utilizado = true
             order by a.id_solicitud_acceso desc limit 1
            """)
    String buscarPayloadActor(Long idTransaccion);

    @Select("""
            select i.id_transaccion, i.id_incapacidad,
                   i.cve_folio_incapacidad as folio_incapacidad,
                   i.cve_ooad_expedicion as cve_ooad, i.cve_unidad_expedicion as cve_unidad
              from expedicion.edit_autorizacion_incapacidad a
              join expedicion.edit_incapacidad i on i.id_incapacidad = a.id_incapacidad
             where a.id_autorizacion_incap = #{idAutorizacion}
               and a.ind_activo = true and i.ind_activo = true
            """)
    mx.gob.imss.edi.bitacora.models.dto.AutorizacionTrazaDto buscarAutorizacion(Long idAutorizacion);
}
