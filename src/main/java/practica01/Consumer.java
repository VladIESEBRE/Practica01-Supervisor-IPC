// Consumer: rol CONSUMER. Lee líneas por stdin hasta recibir EOF (fin de la
// entrada), cuenta las líneas y los caracteres y escribe el resultado por
// stdout con el formato "COUNT=5 CHARS=18". Los diagnósticos van por stderr
// para que stdout contenga solo el resultado.
package practica01;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Consumer {
    // Constructor privado: solo se usa el método estático run().
    private Consumer() {}

    // Lee stdin línea a línea. readLine() devuelve null cuando llega EOF, es
    // decir, cuando quien escribe cierra el tubo. Entonces escribe el
    // resultado por stdout y devuelve 0.
    public static int run() throws IOException {
        Log.setRol("CONSUMER");
        Log.diagnostico("esperant dades per stdin");

        BufferedReader entrada = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        int lineas = 0;
        int caracteres = 0;
        String linea;
        while ((linea = entrada.readLine()) != null) {   // null = EOF
            lineas++;
            caracteres += linea.length();
        }

        Log.diagnostico("EOF rebut");
        System.out.println("COUNT=" + lineas + " CHARS=" + caracteres);
        return 0;
    }
}