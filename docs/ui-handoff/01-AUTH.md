# Integrante 1: Acceso (Splash, Login, Registro, Recuperar contraseña, Perfil)

**Rama:** `feature/auth-ui`
**Carpeta:** `app/src/main/java/com/ecore/demo2/feature/auth/`

Todas estas pantallas se pueden diseñar sin backend. La navegación después del login (ir a Inicio
o volver a Login al cerrar sesión) la hace `navigation/SmartHomeApp.kt` automáticamente: tu pantalla
**no** navega sola al terminar el login.

## Archivos

| Puede modificar | Puede crear | No debe modificar |
|---|---|---|
| `feature/auth/splash/SplashScreen.kt` | `feature/auth/components/*.kt` (componentes compartidos por tus pantallas) | `feature/auth/**/*ViewModel.kt` |
| `feature/auth/login/LoginScreen.kt` | `feature/auth/<pantalla>/components/*.kt` | `feature/auth/**/*UiState.kt` |
| `feature/auth/register/RegisterScreen.kt` | Iconos nuevos `res/drawable/ic_auth_*.xml` | `core/**`, `navigation/**`, `MainActivity.kt` |
| `feature/auth/forgot/ForgotPasswordScreen.kt` | Previews adicionales en tus archivos | `backend/**`, Gradle, `strings.xml` |
| `feature/auth/profile/ProfileScreen.kt` | | La parte `XxxRoute` de cada archivo |

---

## Splash

- **Archivo:** `feature/auth/splash/SplashScreen.kt`
- **UiState:** ninguno. `SplashScreen()` no recibe parámetros; se muestra mientras la app lee la sesión guardada.
- **Acciones:** ninguna (la redirección es automática).
- **Estados:** solo "cargando".
- **Preview:** `SplashScreenPreview`.

## Login

- **Archivo:** `feature/auth/login/LoginScreen.kt`
- **Firma:** `LoginScreen(state, onEmailChange, onPasswordChange, onLogin, onRegister, onForgotPassword, onOpenSettings)`

**UI STATE: `LoginUiState`**

| Propiedad | Tipo | Uso |
|---|---|---|
| `email` | `String` | Texto del campo email |
| `password` | `String` | Texto del campo contraseña |
| `isLoading` | `Boolean` | `true` mientras se valida: deshabilita el botón / muestra progreso |
| `error` | `String?` | Mensaje de error listo para mostrar (credenciales, validación, sin conexión) |

**ACCIONES**

| Callback | Qué hace |
|---|---|
| `onEmailChange(String)` / `onPasswordChange(String)` | Actualiza el campo (y borra el error) |
| `onLogin()` | Valida y hace login (`LoginViewModel.login()`) |
| `onRegister()` | Navega a Registro |
| `onForgotPassword()` | Navega a Recuperar contraseña |
| `onOpenSettings()` | Abre Ajustes (para cambiar Demo/API y la URL antes de entrar) |

**ESTADOS:** normal, cargando (`isLoading`), error (`error != null`).
**PREVIEW:** `LoginScreenPreview` (email de ejemplo, sin error).
**Importante:** mantén visibles el acceso a *Crear cuenta*, *¿Olvidaste tu contraseña?* y
*Configurar servidor / modo demo* (el último es la única forma de pasar a modo API antes de entrar).

## Registro

- **Archivo:** `feature/auth/register/RegisterScreen.kt`
- **Firma:** `RegisterScreen(state, onNameChange, onEmailChange, onPasswordChange, onConfirmPasswordChange, onRegister, onBack)`

**UI STATE: `RegisterUiState`:** `name`, `email`, `password`, `confirmPassword` (`String`), `isLoading` (`Boolean`), `error` (`String?`).

**ACCIONES:** `onNameChange`, `onEmailChange`, `onPasswordChange`, `onConfirmPasswordChange` (`(String) -> Unit`),
`onRegister()` (valida: nombre, email, contraseña ≥ 6 y coincidencia; si va bien, la sesión se abre sola), `onBack()`.

**ESTADOS:** normal, cargando, error. **PREVIEW:** `RegisterScreenPreview`.

## Recuperar contraseña

- **Archivo:** `feature/auth/forgot/ForgotPasswordScreen.kt`
- **Firma:** `ForgotPasswordScreen(state, onEmailChange, onSubmit, onBack)`

**UI STATE: `ForgotPasswordUiState`:** `email` (`String`), `isLoading` (`Boolean`), `sent` (`Boolean`: solicitud enviada), `error` (`String?`).

**ACCIONES:** `onEmailChange(String)`, `onSubmit()`, `onBack()`.
**ESTADOS:** normal, cargando, enviado (`sent`), error. **PREVIEW:** `ForgotPasswordScreenPreview` (estado `sent = true`).
Nota: aún no se envían emails reales; el texto de confirmación no debe prometer más de lo que ocurre.

## Perfil

- **Archivo:** `feature/auth/profile/ProfileScreen.kt`
- **Firma:** `ProfileScreen(state, onLogout, onBack)`

**UI STATE: `ProfileUiState`:** `user: User?` (`id`, `name`, `email`), `isLoggingOut: Boolean`.
**ACCIONES:** `onLogout()` (cierra sesión; la app vuelve sola a Login), `onBack()`.
**ESTADOS:** normal, cerrando sesión. **PREVIEW:** `ProfileScreenPreview` (`PreviewData.user`).

---

## Contrato de diseño: Login (pantalla principal de acceso)

- **Entrada:** `LoginUiState`
- **Puede mostrar:** email, contraseña, error, progreso de carga, accesos a registro/recuperar/ajustes.
- **Acciones:** escribir email/contraseña, entrar, ir a registro, recuperar, ajustes.
- **Debe manejar:** normal, cargando, error.
- **No conoce:** si el login es Demo o contra FastAPI, tokens, DataStore, Retrofit.

## Reglas específicas

- No guardes contraseñas, tokens ni usuarios en la pantalla ni en `remember` persistente.
- No cambies la validación: está en los ViewModels (y en el backend).
- Usa `PasswordVisualTransformation` para contraseñas y el teclado adecuado (`KeyboardType.Email/Password`).
- Reglas generales: [UI-RULES.md](UI-RULES.md).

## Tests relevantes

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.ecore.demo2.feature.auth.*" --tests "com.ecore.demo2.architecture.*"
```

## Checklist de entrega

- [ ] Estoy en `feature/auth-ui`
- [ ] Solo modifiqué archivos permitidos (`git diff --stat` solo muestra `feature/auth/**` y, si acaso, `res/drawable/ic_auth_*`)
- [ ] Todas mis previews funcionan
- [ ] `.\gradlew.bat :app:assembleDebug` pasa
- [ ] Tests relevantes pasan (incluido `UiArchitectureTest`)
- [ ] Login funciona en modo Demo (`demo@casa.local` / `demo1234`) y en modo API con el backend
- [ ] El error se ve (prueba contraseña corta o modo API con el backend apagado)
- [ ] Registro, Recuperar y Cerrar sesión siguen funcionando; la navegación no se rompió
- [ ] Se ve bien en modo oscuro y en pantalla pequeña; el teclado no tapa el botón
- [ ] No agregué dependencias, no hice network calls desde UI, no modifiqué backend
- [ ] Revisé `git diff`, hice commits claros y el working tree quedó limpio
