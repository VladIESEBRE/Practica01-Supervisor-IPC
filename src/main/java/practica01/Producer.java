// Producer: rol PRODUCER. Genera los datos de la pipe: escribe cinco nombres
// por stdout, uno por línea. Los diagnósticos van por stderr para no
// mezclarse con los datos que leerá el consumidor.
package practica01;

import java.util.List;

public final class Producer {
    // Nombres de demostración: 5 líneas y 18 caracteres en total.
    private static final List<String> NOMBRES = List.of("Ana", "Joan", "Pep", "Marc", "Laia");

    // Constructor privado: solo se usa el método estático run().
    private Producer() {}

    // Escribe cada nombre en una línea de stdout y devuelve 0 (éxito).
    // Al terminar el proceso, su stdout se cierra: es el EOF para quien lo lea.
    public static int run() {
        Log.setRol("PRODUCER");
        Log.diagnostico("generant " + NOMBRES.size() + " noms");
        for (String nombre : NOMBRES) {
            System.out.println(nombre);
        }
        System.out.flush();   // asegura que todo sale del búfer antes de acabar
        return 0;
    }
}