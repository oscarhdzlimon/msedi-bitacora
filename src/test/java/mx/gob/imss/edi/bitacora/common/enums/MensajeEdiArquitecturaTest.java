package mx.gob.imss.edi.bitacora.common.enums;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MensajeEdiArquitecturaTest {
    @Test
    void clavesConocidasSoloSeDeclaranEnElEnumYNoEnLosConsumidores() throws Exception {
        Set<String> codigos = Arrays.stream(MensajeEdi.values()).map(MensajeEdi::codigo).collect(Collectors.toSet());
        var tokens = Pattern.compile("/\\*[\\s\\S]*?\\*/|//[^\\r\\n]*|\"(?:\\\\.|[^\"\\\\])*\"");
        var violaciones = new ArrayList<String>();
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("MensajeEdi.java")).toList()) {
                var matcher = tokens.matcher(Files.readString(file));
                while (matcher.find()) {
                    String token = matcher.group();
                    if (token.startsWith("\"") && codigos.contains(token.substring(1, token.length()-1))) {
                        violaciones.add(file + ": " + token);
                    }
                }
            }
        }
        assertTrue(violaciones.isEmpty(), () -> "Usar MensajeEdi en vez de literales: " + violaciones);
    }
}
