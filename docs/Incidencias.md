# Registro de incidencias

| Incidencia | Causa | Cómo se ha detectado | Solución aplicada |
|---|---|---|---|
| Error `Source option 5 is no longer supported. Use 8 or later.` al compilar | Maven 3.8.7 usa por defecto una versión antigua de `maven-compiler-plugin` (3.1) que ignora la propiedad `maven.compiler.release` y compila como Java 5 | Al ejecutar `mvn package` | Fijar `maven-compiler-plugin` 3.13.0 en `pom.xml` |
