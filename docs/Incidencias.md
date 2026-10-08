# Registro de incidencias

| Incidencia | Causa | Cómo se ha detectado | Solución aplicada |
|---|---|---|---|
| Error `Source option 5 is no longer supported. Use 8 or later.` al compilar | Maven 3.8.7 usa por defecto una versión antigua de `maven-compiler-plugin` (3.1) que ignora la propiedad `maven.compiler.release` y compila como Java 5 | Al ejecutar `mvn package` | Fijar `maven-compiler-plugin` 3.13.0 en `pom.xml` |
| `zsh: parse error near '\n'` al ejecutar `pstree -p <40833>` | Los símbolos `< >` del marcador de posición `<PID>` se escribieron literalmente; la shell los interpreta como redirecciones | Error de la shell al ejecutar la orden | Escribir solo el número: `pstree -p 40833` |
