# GitHub: integración continua y protección de `master`

Complementa a [TEAM_WORKFLOW.md](TEAM_WORKFLOW.md) (ramas, PR y commits).

## Qué comprueba CI

| Workflow | Archivo | Qué hace |
|---|---|---|
| **Android CI** | `.github/workflows/android.yml` | JDK 25 → `./gradlew assembleDebug` → `./gradlew test` → `./gradlew lint` |
| **Backend CI** | `.github/workflows/backend.yml` | Python 3.14 → `pip install -r requirements-dev.txt` → `pytest` |

Se ejecutan en:

- cada `push` a `master`;
- cada Pull Request hacia `master` (y en cada nuevo push a ese PR);
- manualmente: pestaña **Actions** → elegir workflow → **Run workflow**.

Si subes otro commit a un PR mientras CI sigue corriendo, la ejecución anterior se cancela
y solo cuenta la nueva.

Los workflows no usan secretos ni servicios externos. Los tests del backend crean su propia
base de datos SQLite temporal.

Si Android CI falla en tests o lint, la ejecución adjunta el artefacto **android-reports**
(informes HTML/XML, se guardan 7 días): ábrelo desde la página de la ejecución, en *Artifacts*.
No se publica ningún APK.

### Reproducir CI en local

```powershell
# Android (Windows, JDK de Android Studio)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat lint

# Backend
cd backend
.\.venv\Scripts\activate
pip install -r requirements-dev.txt
pytest
```

Si pasa en local pero falla en CI, lo habitual es un archivo que no se ha subido
(`git status`) o una diferencia de mayúsculas en un nombre de archivo (Linux las distingue).

## Protección de `master` (configuración manual recomendada)

> **Esto no se puede configurar desde el código.** Lo tiene que hacer una persona con permisos
> de administración del repositorio en GitHub. Hasta que alguien lo haga, **`master` NO está
> protegida**: se puede hacer push directo y merge aunque CI falle.

Recomendación para `master`:

- Exigir Pull Request antes de hacer merge.
- **Android CI** obligatorio.
- **Backend CI** obligatorio.
- No permitir force push (ni borrar la rama).

### Pasos

1. Comprobar que ambos workflows se han ejecutado al menos una vez (por ejemplo, abriendo el PR
   de esta rama). GitHub solo ofrece como obligatorios los checks que ya ha visto.
2. En el repositorio: **Settings → Rules → Rulesets → New ruleset → New branch ruleset**.
   (Alternativa clásica: **Settings → Branches → Add branch protection rule**, patrón `master`.)
3. Nombre: `master`. **Enforcement status: Active**.
4. **Target branches → Add target → Include default branch** (o por patrón: `master`).
5. Activar:
   - **Restrict deletions**.
   - **Block force pushes**.
   - **Require a pull request before merging** (aprobaciones requeridas: 1 es razonable;
     0 si el equipo aún no hace revisiones).
   - **Require status checks to pass** → **Add checks** y seleccionar:
     - `Build, test y lint` (job de *Android CI*)
     - `Pytest` (job de *Backend CI*)

     Opcional: **Require branches to be up to date before merging**, para que el PR se pruebe
     contra el último `master`.
6. **Create** / **Save changes**.
7. Verificar: abrir un PR de prueba y comprobar que el botón de merge queda bloqueado hasta que
   ambos checks estén en verde.

### Notas

- Los checks aparecen con el **nombre del job** (`Build, test y lint`, `Pytest`). Si algún día se
  renombra un job, hay que actualizar la regla o los PR se quedarán esperando un check que ya no existe.
- Los workflows no filtran por rutas a propósito: un check obligatorio que no se ejecuta deja el PR
  bloqueado. Por eso Backend CI corre también en PR que solo tocan Android, y viceversa.
