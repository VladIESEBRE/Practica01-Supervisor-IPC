// ProcessForkJoinDemo: punto de entrada. Según el primer argumento decide qué
// rol hace este proceso. Sin argumentos actúa como SUPERVISOR (así
// "java -jar supervisor.jar" sigue funcionando). Termina con System.exit(codigo)
// para que el padre (la shell o el supervisor) reciba el código de retorno.
package practica01;

public class ProcessForkJoinDemo {
    // Lee el rol del primer argumento, ejecuta la parte del programa que toca
    // y termina el proceso con el código de retorno obtenido.
    //   sin argumentos               -> supervisor
    //   worker <NOMBRE> <pasos> <cod> -> worker
    //   cualquier otra cosa          -> error, código 2
    public static void main(String[] args) throws Exception {
        String rol = args.length > 0 ? args[0] : "supervisor";

        int codigo = switch (rol) {
            case "supervisor" -> {
                Log.info("hola, soc el supervisor");
                yield 0;
            }
            case "worker" -> Worker.run(args[1],
                    Integer.parseInt(args[2]),
                    Integer.parseInt(args[3]));
            default -> {
                Log.error("rol desconegut: " + rol);
                yield 2;
            }
        };

        System.exit(codigo);
    }
}