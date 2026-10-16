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
                id_evento, id_mensaje, id_transaccion, id_sistema_origen,
                id_transaccion_actor, cve_usuario, ref_perfil_usuario, ref_clave_deduplicacion,
                ref_nombre_usuario, ref_error, ref_detalle, ref_sesion,
                ref_terminal, ref_objeto, cve_folio_incapacidad, cve_operacion, ref_resultado,
                stp_ocurrencia, ind_activo, stp_alta, cve_usuario_alta
            ) values (
                #{idEvento}, #{idMensaje}, #{idTransaccion}, #{idSistemaOrigen},
                #{idTransaccionActor}, #{cveUsuario}, #{refPerfilUsuario}, #{refClaveDeduplicacion},
                #{refNombreUsuario}, #{refError}, #{refDetalle}, #{refSesion},
                #{refTerminal}, #{refObjeto}, #{cveFolioIncapacidad}, #{cveOperacion}, #{refResultado},
                #{stpOcurrencia}, true, current_timestamp, #{cveUsuarioAlta}
            )
            on conflict (id_transaccion, id_evento, ref_clave_deduplicacion)
                where ref_clave_deduplicacion is not null do nothing
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
                   coalesce(eb.cve_usuario, eb.ref_nombre_usuario) as usuario,
                   eb.ref_objeto as objeto,
                   eb.cve_folio_incapacidad as cve_folio_incapacidad,
                   eb.ref_error as error,
                   eb.ref_detalle as detalle,
                   eb.stp_ocurrencia as stp_ocurrencia,
                   eb.stp_alta as stp_alta,
                   eb.id_transaccion_actor as id_transaccion_actor,
                   eb.ref_perfil_usuario as perfil_usuario, eb.ref_nombre_usuario as nombre_usuario
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
                   coalesce(eb.cve_usuario, eb.ref_nombre_usuario) as usuario,
                   eb.ref_objeto as objeto,
                   eb.cve_folio_incapacidad as cve_folio_incapacidad,
                   eb.ref_error as error,
                   eb.ref_detalle as detalle,
                   eb.stp_ocurrencia as stp_ocurrencia,
                   eb.stp_alta as stp_alta,
                   eb.id_transaccion_actor as id_transaccion_actor,
                   eb.ref_perfil_usuario as perfil_usuario, eb.ref_nombre_usuario as nombre_usuario
              from trazabilidad.edit_evento_bitacora eb
              join catalogo.edic_evento e on e.id_evento = eb.id_evento
              join trazabilidad.edit_transaccion t on t.id_transaccion = eb.id_transaccion
             where eb.ind_activo = true
               and eb.id_transaccion = #{idTransaccion}
              <if test="cveTransaccion != null and cveTransaccion != ''">
                and t.cve_transaccion = #{cveTransaccion}
              </if>
              <if test="cveUsuario != null and cveUsuario != ''">
                and coalesce(eb.cve_usuario, eb.ref_nombre_usuario) = #{cveUsuario}
              </if>
              <if test="folioIncapacidad != null and folioIncapacidad != ''">
                and eb.cve_folio_incapacidad = #{folioIncapacidad}
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
            @Param("folioIncapacidad") String folioIncapacidad,
            @Param("fechaInicio") OffsetDateTime fechaInicio,
            @Param("fechaFin") OffsetDateTime fechaFin,
            @Param("cveEvento") String cveEvento,
            @Param("resultado") String resultado);
    @Select("""
            select id_evento_bitacora from trazabilidad.edit_evento_bitacora
             where id_transaccion = #{idTransaccion} and id_evento = #{idEvento}
               and ref_clave_deduplicacion = #{refClaveDeduplicacion}
            """)
    Long buscarDuplicado(EventoBitacoraRegistro registro);
    @Select("""
            select pg_advisory_xact_lock(hashtextextended(
                cast(#{idTransaccion} as text) || ':' || cast(#{idEvento} as text) || ':' ||
                split_part(#{refClaveDeduplicacion}, ':', 1), 0))::text
            """)
    String bloquearResumen(EventoBitacoraRegistro registro);

    @Select("""
            select id_evento_bitacora, ref_clave_deduplicacion
              from trazabilidad.edit_evento_bitacora
             where id_transaccion = #{idTransaccion} and id_evento = #{idEvento}
               and ref_clave_deduplicacion like split_part(#{refClaveDeduplicacion}, ':', 1) || ':%'
             order by id_evento_bitacora desc limit 1
            """)
    mx.gob.imss.edi.bitacora.models.dto.ResumenAnteriorDto ultimoResumen(EventoBitacoraRegistro registro);
}
