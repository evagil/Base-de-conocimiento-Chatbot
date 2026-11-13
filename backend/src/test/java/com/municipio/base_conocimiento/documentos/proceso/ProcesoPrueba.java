package com.municipio.base_conocimiento.documentos.proceso;

import java.nio.file.*;
import java.util.stream.Collectors;

public final class ProcesoPrueba {
    private ProcesoPrueba() { }
    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "esperar" -> { Files.writeString(Path.of("pid.txt"), Long.toString(ProcessHandle.current().pid())); Thread.sleep(30000); }
            case "entorno" -> Files.writeString(Path.of("entorno.txt"), System.getenv().keySet().stream().sorted().collect(Collectors.joining("\n")));
            case "salida" -> { for (int i = 0; i < 32; i++) System.out.write(new byte[65536]); }
            default -> System.exit(7);
        }
    }
}
