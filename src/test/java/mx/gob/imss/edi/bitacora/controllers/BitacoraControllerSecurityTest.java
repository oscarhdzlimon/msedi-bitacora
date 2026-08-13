package mx.gob.imss.edi.bitacora.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import mx.gob.imss.edi.bitacora.models.response.RegistroEventoResponse;
import mx.gob.imss.edi.bitacora.services.BitacoraService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BitacoraControllerSecurityTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private BitacoraService bitacoraService;

    @Test
    void rechazaConsultaSinBearer() throws Exception {
        mockMvc.perform(get("/api/v1/transacciones/12/eventos")
                        .header("X-Transaccion-Id", "12"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    void rechazaHeaderDeTransaccionDiferente() throws Exception {
        mockMvc.perform(get("/api/v1/transacciones/12/eventos")
                        .header("Authorization", "Bearer " + token(12L))
                        .header("X-Transaccion-Id", "13"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("TRANSACCION_NO_CORRESPONDE"));
    }

    @Test
    void validaCamposObligatoriosDelRegistro() throws Exception {
        mockMvc.perform(post("/api/v1/eventos")
                        .header("Authorization", "Bearer " + token(12L))
                        .header("X-Transaccion-Id", "12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void registraEventoValido() throws Exception {
        when(bitacoraService.registrar(anyLong(), any(), anyString()))
                .thenReturn(new RegistroEventoResponse(101L));

        mockMvc.perform(post("/api/v1/eventos")
                        .header("Authorization", "Bearer " + token(12L))
                        .header("X-Transaccion-Id", "12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cveEvento": "TOKEN_EDI_GENERADO",
                                  "cveOperacion": "proceso",
                                  "resultado": "EXITOSA",
                                  "stpOcurrencia": "2026-08-12T15:00:00-06:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.respuesta.idEventoBitacora").value(101));
    }

    @Test
    void consultaAvanzadaAutorizada() throws Exception {
        when(bitacoraService.consultar(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/eventos")
                        .header("Authorization", "Bearer " + token(12L))
                        .header("X-Transaccion-Id", "12")
                        .queryParam("resultado", "EXITOSA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").isArray());
    }

    private String token(Long idTransaccion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("msedi-autenticacion")
                .id("jti-prueba")
                .issuer("msedi-autenticacion")
                .audience().add("edi").and()
                .claim("idTransaccion", idTransaccion)
                .claim("cveTransaccion", "tx-prueba")
                .claim("sistemaOrigen", "SIMF")
                .claim("user", "usuario-prueba")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
