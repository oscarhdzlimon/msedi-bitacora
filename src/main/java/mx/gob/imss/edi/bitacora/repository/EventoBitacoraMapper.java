package mx.gob.imss.edi.bitacora.repository;

import java.time.OffsetDateTime;
import java.util.List;
import mx.gob.imss.edi.bitacora.models.dto.EventoBitacoraRegistro;
import mx.gob.imss.edi.bitacora.models.dto.EventoConsultaDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EventoBitacoraMapper {

    @Insert("""
            insert into trazabilidad.edit_evento_bitacora (
                id_evento, id_transaccion, id_sistema_origen,
                ref_nombre_usuario, ref_error, ref_detalle, ref_sesion,
                ref_terminal, ref_objeto, cve_operacion, ref_resultado,
                stp_ocurrencia, ind_activo, stp_alta, cve_usuario_alta
            ) values (
                #{idEvento}, #{idTransaccion}, #{idSistemaOrigen},
                #{refNombreUsuario}, #{refError}, #{refDetalle}, #{refSesion},
                #{refTerminal}, #{refObjeto}, #{cveOperacion}, #{refResultado},
                #{stpOcurrencia}, true, current_timestamp, #{cveUsuarioAlta}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "idEventoBitacora", keyColumn = "id_evento_bitacora")
    int insertar(EventoBitacoraRegistro registro);

    @Select("""
            <script>
            select eb.id_evento_bitacora as id_evento_bitacora,
                   eb.id_transaccion as id_transaccion,
                   t.cve_transaccion as cve_transaccion,
                   e.cve_evento as cve_evento,
                   e.des_evento as descripcion_evento,
                   eb.cve_operacion as cve_operacion,
                   eb.ref_resultado as resultado,
                   eb.ref_nombre_usuario as usuario,
                   eb.ref_objeto as objeto,
                   eb.ref_error as error,
                   eb.ref_detalle as detalle,
                   eb.stp_ocurrencia as stp_ocurrencia,
                   eb.stp_alta as stp_alta
              from trazabilidad.edit_evento_bitacora eb
              join catalogo.edic_evento e on e.id_evento = eb.id_evento
              join trazabilidad.edit_transaccion t on t.id_transaccion = eb.id_transaccion
             where eb.ind_activo = true
               and eb.id_transaccion = #{idTransaccion}
             order by eb.stp_ocurrencia, eb.id_evento_bitacora
            </script>
            """)
    List<EventoConsultaDto> consultarPorTransaccion(Long idTransaccion);

    @Select("""
            <script>
            select eb.id_evento_bitacora as id_evento_bitacora,
                   eb.id_transaccion as id_transaccion,
                   t.cve_transaccion as cve_transaccion,
                   e.cve_evento as cve_evento,
                   e.des_evento as descripcion_evento,
                   eb.cve_operacion as cve_operacion,
                   eb.ref_resultado as resultado,
                   eb.ref_nombre_usuario as usuario,
                   eb.ref_objeto as objeto,
                   eb.ref_error as error,
                   eb.ref_detalle as detalle,
                   eb.stp_ocurrencia as stp_ocurrencia,
                   eb.stp_alta as stp_alta
              from trazabilidad.edit_evento_bitacora eb
              join catalogo.edic_evento e on e.id_evento = eb.id_evento
              join trazabilidad.edit_transaccion t on t.id_transaccion = eb.id_transaccion
             where eb.ind_activo = true
               and eb.id_transaccion = #{idTransaccion}
              <if test="cveTransaccion != null and cveTransaccion != ''">
                and t.cve_transaccion = #{cveTransaccion}
              </if>
              <if test="cveUsuario != null and cveUsuario != ''">
                and eb.ref_nombre_usuario = #{cveUsuario}
              </if>
              <if test="fechaInicio != null">
                and eb.stp_ocurrencia &gt;= #{fechaInicio}
              </if>
              <if test="fechaFin != null">
                and eb.stp_ocurrencia &lt;= #{fechaFin}
              </if>
              <if test="cveEvento != null and cveEvento != ''">
                and e.cve_evento = #{cveEvento}
              </if>
              <if test="resultado != null and resultado != ''">
                and eb.ref_resultado = #{resultado}
              </if>
             order by eb.stp_ocurrencia, eb.id_evento_bitacora
            </script>
            """)
    List<EventoConsultaDto> consultar(
            @Param("idTransaccion") Long idTransaccion,
            @Param("cveTransaccion") String cveTransaccion,
            @Param("cveUsuario") String cveUsuario,
            @Param("fechaInicio") OffsetDateTime fechaInicio,
            @Param("fechaFin") OffsetDateTime fechaFin,
            @Param("cveEvento") String cveEvento,
            @Param("resultado") String resultado);
}
