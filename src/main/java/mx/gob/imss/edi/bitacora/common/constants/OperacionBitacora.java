package mx.gob.imss.edi.bitacora.common.constants;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum OperacionBitacora {
    INSERT, UPDATE, DELETE, SELECT, LOGIN, PROCESO, ERROR;

    public static Optional<String> normalizar(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(valor.strip()))
                .map(value -> value.name().toLowerCase(Locale.ROOT))
                .findFirst();
    }
}
