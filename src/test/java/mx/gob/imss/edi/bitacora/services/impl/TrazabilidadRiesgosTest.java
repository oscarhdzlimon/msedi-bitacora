package mx.gob.imss.edi.bitacora.services.impl;
import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
import java.util.*;import java.time.*;import com.fasterxml.jackson.databind.ObjectMapper;import org.junit.jupiter.api.Test;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.test.util.ReflectionTestUtils;
import mx.gob.imss.edi.bitacora.models.dto.*;import mx.gob.imss.edi.bitacora.models.request.RegistroEventoRequest;import mx.gob.imss.edi.bitacora.repository.*;import mx.gob.imss.edi.bitacora.security.*;import mx.gob.imss.edi.bitacora.services.DatosSensiblesService;
class TrazabilidadRiesgosTest {
 final EventoMapper eventos=mock(EventoMapper.class);final MensajeMapper mensajes=mock(MensajeMapper.class);final TransaccionConsultaMapper transacciones=mock(TransaccionConsultaMapper.class);final EventoBitacoraMapper registros=mock(EventoBitacoraMapper.class);final SecurityContextService seguridad=mock(SecurityContextService.class);final EventoPendienteRiesgosMapper db=mock(EventoPendienteRiesgosMapper.class);final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
 final BitacoraServiceImpl service=new BitacoraServiceImpl(eventos,mensajes,transacciones,registros,seguridad,new DatosSensiblesService(),json);
 final UUID uuid=UUID.randomUUID();
 RegistroEventoRequest request(){return new RegistroEventoRequest("MOTOR_AUTORIZACION_APROBADA","PROCESO","AUTORIZADA",OffsetDateTime.parse("2026-11-03T10:00:00-06:00"),null,"ABC12345","INCAPACIDAD",null,Map.of("idEntrega",uuid.toString(),"idEvaluacion",1));}
 void preparar()throws Exception{
  ReflectionTestUtils.setField(service,"trazaMapper",db);
  when(seguridad.validarTransaccion(40L)).thenReturn(new JwtPrincipal("msedi-evaluacion-riesgos","jti",40L,"tx","NSSA","12345","SERVICIO_BITACORA",null,null,null,null,"edi:bitacora:riesgos",null));
  when(eventos.buscarActivoPorClave(anyString())).thenReturn(new EventoCatalogoDto(7L,"MOTOR_AUTORIZACION_APROBADA"));
  when(transacciones.buscarContexto(40L)).thenReturn(new TransaccionContextoDto(40L,13L,"NSSA","tx40"));
  when(transacciones.buscarContexto(39L)).thenReturn(new TransaccionContextoDto(39L,12L,"EDI","tx39"));
  var f=new HashMap<String,Object>();f.put("id_evento_pendiente",uuid);f.put("id_transaccion_origen",39L);f.put("id_transaccion_actor",40L);f.put("ref_evento_json",json.writeValueAsString(request()));f.put("cve_usuario","12345");f.put("ref_nombre_usuario","PERSONA PRUEBA");f.put("ref_perfil_usuario","PERFIL_DPES_AUTORIZADOR");f.put("ref_sesion","9");
  when(db.consultarEntrega(eq(uuid))).thenReturn(List.of(f));
  when(registros.buscarDuplicado(any())).thenReturn(null);
  when(registros.insertar(any())).thenAnswer(i->{((EventoBitacoraRegistro)i.getArgument(0)).setIdEventoBitacora(99L);return 1;});
 }
 @Test void conservaOrigenDeEdiActorNssaYFechaOriginal()throws Exception{
  preparar();service.registrar(40L,request(),"local");var c=org.mockito.ArgumentCaptor.forClass(EventoBitacoraRegistro.class);verify(registros).insertar(c.capture());
  var r=c.getValue();assertThat(r.getIdTransaccion()).isEqualTo(39L);assertThat(r.getIdTransaccionActor()).isEqualTo(40L);assertThat(r.getCveUsuario()).isEqualTo("12345");assertThat(r.getRefClaveDeduplicacion()).isEqualTo("RIESGOS:"+uuid);assertThat(r.getStpOcurrencia()).isEqualTo(request().stpOcurrencia());
 }
 @Test void reintentoDevuelveElRegistroExistente()throws Exception{preparar();when(registros.buscarDuplicado(any())).thenReturn(99L);service.registrar(40L,request(),"local");verify(registros,never()).insertar(any());}
 @Test void rechazaCambioDeContenidoSinInsertar()throws Exception{preparar();var r=request();var alterado=new RegistroEventoRequest(r.cveEvento(),r.cveOperacion(),"RECHAZADA",r.stpOcurrencia(),null,r.cveFolioIncapacidad(),r.objeto(),null,r.detalle());assertThatThrownBy(()->service.registrar(40L,alterado,"local")).isInstanceOf(mx.gob.imss.edi.bitacora.exceptions.EdiException.class);verifyNoInteractions(registros);}
}
