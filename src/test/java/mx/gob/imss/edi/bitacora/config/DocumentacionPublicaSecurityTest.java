package mx.gob.imss.edi.bitacora.config;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import com.fasterxml.jackson.databind.ObjectMapper;
import mx.gob.imss.edi.bitacora.security.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class DocumentacionPublicaSecurityTest {
    static final String CONTEXTO = "/msedi-bitacora";
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;

    @RestController static class Rutas {
        // Estas respuestas permiten verificar las reglas y filtros sin iniciar bases o servicios.
        @GetMapping({"/actuator", "/actuator/", "/actuator/health", "/actuator/health/liveness",
                "/actuator/health/readiness", "/actuator/info", "/swagger-ui", "/swagger-ui.html",
                "/swagger-ui/index.html", "/swagger-ui/swagger-ui.css", "/v3/api-docs",
                "/v3/api-docs/swagger-config", "/v3/api-docs.yaml", "/api/v1/prueba-seguridad"})
        String consultar() { return "ok"; }
        @PostMapping("/actuator/shutdown") String modificar() { return "ok"; }
    }

    @Configuration @EnableWebMvc @EnableWebSecurity @Import(SecurityConfig.class)
    static class Config {
        @Bean ObjectMapper mapper() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean JwtService jwt() { return mock(JwtService.class); }
        @Bean JwtAuthenticationFilter jwtFilter(JwtService jwt) { return new JwtAuthenticationFilter(jwt); }
        @Bean TransactionIdFilter transactionFilter(ObjectMapper mapper) { return new TransactionIdFilter(mapper); }
        @Bean RestAuthenticationEntryPoint entryPoint() throws Exception {
            var entry = mock(RestAuthenticationEntryPoint.class);
            doAnswer(call -> {
                ((jakarta.servlet.http.HttpServletResponse) call.getArgument(1)).setStatus(401);
                return null;
            }).when(entry).commence(any(),any(),any());
            return entry;
        }
        @Bean Rutas rutas() { return new Rutas(); }
    }

    @BeforeEach void preparar() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext()); context.register(Config.class); context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void cerrar() { context.close(); }

    @Test void actuatorIncluyeRaizYConsultasPublicasSinBearer() throws Exception {
        for (String path : new String[]{"/actuator", "/actuator/", "/actuator/health",
                "/actuator/health/liveness", "/actuator/health/readiness", "/actuator/info"}) {
            mvc.perform(get(CONTEXTO+path).contextPath(CONTEXTO)).andExpect(status().isOk());
        }
    }
    @Test void swaggerIncluyeEntradaRecursosYDocumentosSinBearer() throws Exception {
        for (String path : new String[]{"/swagger-ui", "/swagger-ui.html", "/swagger-ui/index.html",
                "/swagger-ui/swagger-ui.css", "/v3/api-docs", "/v3/api-docs/swagger-config", "/v3/api-docs.yaml"}) {
            mvc.perform(get(CONTEXTO+path).contextPath(CONTEXTO)).andExpect(status().isOk());
        }
    }
    @Test void endpointsDeNegocioSiguenRequiriendoAutenticacion() throws Exception {
        mvc.perform(get(CONTEXTO+"/api/v1/prueba-seguridad").contextPath(CONTEXTO))
                .andExpect(status().isUnauthorized());
    }
    @Test void permitirConsultasDeActuatorNoHabilitaEscriturasAnonimas() throws Exception {
        mvc.perform(post(CONTEXTO+"/actuator/shutdown").contextPath(CONTEXTO))
                .andExpect(status().isUnauthorized());
    }
}
