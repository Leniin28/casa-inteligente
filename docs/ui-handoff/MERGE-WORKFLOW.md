# Flujo de ramas, commits e integración

## Ramas

| Rama | Quién | Parte de |
|---|---|---|
| `master` | Responsable técnico | Versión estable (tag `v0.1.0-base` = Fase 1) |
| `feature/auth-ui` | Integrante 1 | `master` |
| `feature/dashboard-electricity-ui` | Integrante 2 | `master` |
| `feature/water-devices-ui` | Integrante 3 | `master` |
| `feature/assistant-alerts-ui` | Integrante 4 | `master` |
| `feature/budget-history-ui` | Integrante 5 | `master` |

Cada integrante trabaja **solo en su rama** y solo en los archivos de su documento.

## Paso 0: publicar el repositorio (solo el responsable técnico, una vez)

Ahora mismo el repositorio es local (no tiene remoto). Para que el equipo pueda clonarlo, crea un
repositorio vacío (GitHub/GitLab, **privado**) y ejecuta:

```powershell
git remote add origin https://github.com/<usuario>/<repo>.git
git push -u origin master
git push origin feature/auth-ui feature/dashboard-electricity-ui feature/water-devices-ui feature/assistant-alerts-ui feature/budget-history-ui
git push origin --tags
```

Protege `master` en la web (Settings → Branches → "Require a pull request before merging") si es posible.

## Primera vez (cada integrante)

```powershell
git clone https://github.com/<usuario>/<repo>.git casa-inteligente
cd casa-inteligente
git switch feature/<tu-rama>      # p. ej. feature/water-devices-ui
git merge origin/master           # trae lo último de master (documentación incluida)
```

Abre la carpeta en Android Studio y ejecuta la app una vez en modo Demo.

## Día a día

```powershell
# 1. Ir a tu rama
git switch feature/<tu-rama>

# 2. Traer lo último de master
git fetch origin
git merge origin/master

# 3. Trabajar (Android Studio). Luego revisar qué cambiaste:
git status
git diff

# 4. Comprobar que todo compila y los tests pasan
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest

# 5. Commit pequeño y claro, añadiendo SOLO tus archivos
git add app/src/main/java/com/ecore/demo2/feature/water
git commit -m "style: redesign water screen"

# 6. Subir tu rama
git push -u origin feature/<tu-rama>     # la primera vez; después basta con: git push
```

Revisa `git status` antes de cada commit: si aparece un archivo que no es de tu feature, **no lo
añadas** (`git restore <archivo>` para descartar cambios accidentales en ese archivo).

## Integrar tu trabajo en master

1. Asegúrate de tener master al día en tu rama (`git fetch origin` + `git merge origin/master`),
   de que compila y de que los tests pasan.
2. Abre un **Pull Request** de `feature/<tu-rama>` → `master`.
3. El responsable técnico lo revisa (que solo cambie archivos permitidos) y hace el merge.

Si todavía no hay remoto, el responsable técnico integra en local:

```powershell
git switch master
git merge --no-ff feature/<rama>
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

## Si hay un conflicto al hacer `git merge origin/master`

1. `git status` muestra los archivos en conflicto.
2. Ábrelos en Android Studio (*Git → Resolve Conflicts*) y elige/combina los cambios.
3. Si el conflicto está en un archivo que **no es tuyo**, quédate con la versión de master
   ("Accept Theirs") y avisa al responsable técnico.
4. `git add <archivo>` y `git commit` para cerrar el merge.
5. Si te lías: `git merge --abort` deja todo como estaba antes del merge.

## ❌ Nunca ejecutes

- `git push --force` / `git push -f` (borra trabajo de otros en el remoto).
- `git reset --hard` sobre ramas compartidas o trabajo que no es tuyo.
- `git clean -fdx` o variantes agresivas (borra archivos no versionados, incluida tu config local).
- `git rebase` sobre ramas ya subidas, o cualquier cosa que reescriba historia compartida.
- Commits directos en `master`.

Si algo sale mal, **para y pregunta** antes de "arreglarlo" con comandos destructivos.
