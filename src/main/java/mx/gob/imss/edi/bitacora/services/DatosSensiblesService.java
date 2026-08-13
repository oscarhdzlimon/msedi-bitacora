package mx.gob.imss.edi.bitacora.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DatosSensiblesService {

    private static final Set<String> SECRETOS = Set.of(
            "password", "contrasena", "authorization", "bearer", "token", "jwt", "apikey", "api_key");
    private static final Set<String> IDENTIFICADORES = Set.of("nss", "curp");
    private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern CREDENCIAL = Pattern.compile(
            "(?i)(password|contrasena|api[-_ ]?key|token|jwt)(\\s*[:=]\\s*)([^,;\\s]+)");
    private static final Pattern NSS = Pattern.compile("(?<!\\d)\\d{11}(?!\\d)");
    private static final Pattern CURP = Pattern.compile(
            "(?i)(?<![A-Z0-9])[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d(?![A-Z0-9])");

    public Map<String, Object> sanitizar(Map<String, Object> detalle) {
        if (detalle == null || detalle.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        detalle.forEach((clave, valor) -> resultado.put(clave, sanitizarValor(clave, valor)));
        return resultado;
    }

    public String sanitizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        String sanitizado = BEARER.matcher(texto).replaceAll("Bearer [REDACTADO]");
        sanitizado = CREDENCIAL.matcher(sanitizado).replaceAll("$1$2[REDACTADO]");
        sanitizado = NSS.matcher(sanitizado).replaceAll(match -> enmascarar(match.group()));
        return CURP.matcher(sanitizado).replaceAll(match -> enmascarar(match.group()));
    }

    private Object sanitizarValor(String clave, Object valor) {
        String normalizada = clave.toLowerCase(Locale.ROOT).replace("-", "_");
        if (SECRETOS.stream().anyMatch(normalizada::contains)) {
            return "[REDACTADO]";
        }
        if (IDENTIFICADORES.contains(normalizada)) {
            return enmascarar(valor);
        }
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> anidado = new LinkedHashMap<>();
            mapa.forEach((key, nestedValue) -> {
                String nestedKey = String.valueOf(key);
                anidado.put(nestedKey, sanitizarValor(nestedKey, nestedValue));
            });
            return anidado;
        }
        if (valor instanceof Iterable<?> iterable) {
            List<Object> lista = new ArrayList<>();
            iterable.forEach(item -> lista.add(item instanceof Map<?, ?> mapa
                    ? sanitizarValor("detalle", mapa)
                    : item));
            return lista;
        }
        return valor;
    }

    private String enmascarar(Object valor) {
        if (valor == null) {
            return null;
        }
        String texto = String.valueOf(valor);
        int visibles = Math.min(4, texto.length());
        return "*".repeat(Math.max(0, texto.length() - visibles)) + texto.substring(texto.length() - visibles);
    }
}
