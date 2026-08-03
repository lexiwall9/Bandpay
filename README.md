# BandPay

Aplicación Android para la gestión de una banda musical: integrantes, compromisos, asistencia, pagos y perfiles. Está desarrollada con Kotlin y Jetpack Compose, y sincroniza sus datos con Firebase Realtime Database.

## Funcionalidades

- Inicio de sesión con correo y contraseña mediante Firebase Authentication.
- Acceso biométrico en dispositivos compatibles.
- Perfiles con nombre, celular, instrumento y foto por URL.
- Panel principal con métricas actualizadas desde Firebase: eventos, pagos, tareas, avance y actividad reciente.
- Gestión de compromisos, asistencia y pagos para administradores.
- Roles de administrador y músico; los músicos no ven herramientas administrativas.
- Solicitud de registro para nuevos músicos:
  1. El músico envía sus datos sin contraseña.
  2. El administrador revisa y aprueba o rechaza la solicitud desde **Integrantes**.
  3. Tras la aprobación, el músico crea su contraseña y activa su cuenta.
- Notificaciones locales de Android para nuevas solicitudes mientras la app del administrador está activa o en segundo plano.

## Tecnologías y herramientas

| Herramienta | Uso en el proyecto |
| --- | --- |
| Kotlin | Lenguaje principal de la aplicación. |
| Jetpack Compose + Material 3 | Interfaz declarativa, navegación y componentes visuales. |
| Android SDK 36 | Compilación y comportamiento de Android moderno. |
| Firebase Authentication | Registro, acceso con correo/contraseña y sesiones anónimas para solicitudes. |
| Firebase Realtime Database | Almacenamiento y sincronización en tiempo real de usuarios, integrantes, eventos, asistencia, pagos y solicitudes. |
| Firebase App Check (reCAPTCHA) | Dependencia preparada para reforzar la protección de Firebase. |
| Room | Persistencia local preparada para datos de la banda. |
| Kotlin Coroutines y Flow | Estado reactivo y actualización automática de la interfaz. |
| Navigation Compose | Navegación entre inicio, eventos, integrantes, perfil y detalle de pagos. |
| AndroidX Biometric | Inicio de sesión con huella digital. |
| Coil | Carga de imágenes para la foto de perfil mediante URL. |
| KSP | Generación de código para Room y Moshi. |
| Moshi, Retrofit y OkHttp | Dependencias disponibles para integración de APIs. |
| JUnit, Robolectric, Espresso y Roborazzi | Pruebas unitarias, instrumentadas y visuales. |
| Gradle Version Catalog | Administración centralizada de versiones y librerías. |

## Requisitos

- Android Studio actualizado.
- JDK 11 o superior configurado en `JAVA_HOME`.
- Dispositivo o emulador con Android 7.0 (API 24) o superior.
- Un proyecto Firebase configurado para Android.

## Configuración de Firebase

1. Crea o selecciona un proyecto en [Firebase Console](https://console.firebase.google.com/).
2. Registra la aplicación Android con el paquete:

   ```text
   com.aistudio.pagosbandas.ptnyws
   ```

3. Descarga `google-services.json` y colócalo en `app/google-services.json`.
4. En **Authentication → Método de acceso**, habilita:

   - Correo electrónico/contraseña.
   - Anónimo, necesario para enviar una solicitud antes de crear una cuenta.

5. Crea una instancia de **Realtime Database**.
6. Configura reglas apropiadas antes de publicar la aplicación. Durante pruebas, la app requiere que las solicitudes autenticadas puedan acceder a los datos que utiliza.

> No guardes contraseñas en Realtime Database. Firebase Authentication se encarga de almacenarlas de forma segura.

## Estructura de datos usada en Realtime Database

```text
users/{uid}
  name, email, phone, instrument, role, photoUrl

members/{memberId}
  id, name, phone, instrument

commitments/{commitmentId}
  id, title, description, date, time, location, isCompleted

attendance/{attendanceId}
  id, commitmentId, memberId, status, paymentAmount, isPaid

registrationRequests/{requestId}
  name, email, phone, instrument, status, requesterId, createdAt, reviewedAt
```

Los estados de una solicitud son `pending`, `approved` y `rejected`.

## Arquitectura de la aplicación

El proyecto sigue una organización simple basada en MVVM y flujos reactivos.

```text
Pantallas Compose
        │
        ▼
ViewModels (estado, validaciones y acciones)
        │
        ▼
BandRepository (lectura y escritura)
        │
        ├── Firebase Authentication
        ├── Firebase Realtime Database
        └── Room / almacenamiento local preparado
```

### Capas principales

- **`ui/screens/`**: pantallas y componentes de Jetpack Compose.
- **`ui/viewmodel/`**: estado de login, perfil, tablero, compromisos, integrantes y pagos.
- **`data/`**: entidades (`Member`, `Commitment`, `Attendance`), base local y repositorio Firebase.
- **`ui/theme/`**: colores, tipografías y tema visual de la aplicación.
- **`notifications/`**: creación del canal y notificaciones locales de solicitudes para Android.
- **`MainActivity.kt`**: inicialización de Firebase, ViewModels, navegación principal, biometría y permisos.

## Pantallas

| Pantalla | Descripción |
| --- | --- |
| Splash | Muestra la identidad visual mientras se prepara la navegación. |
| Inicio de sesión | Permite ingresar con correo/contraseña, huella o cambiar de usuario. |
| Solicitud de cuenta | Permite a un músico solicitar acceso con nombre, correo, celular e instrumento. |
| Inicio | Muestra las métricas sincronizadas de eventos, pagos, tareas y actividad reciente. |
| Eventos | Lista compromisos próximos e históricos; los administradores pueden crear nuevos. |
| Integrantes | Lista músicos, permite crear cuentas administrativas y revisar solicitudes pendientes. |
| Detalle de pago | Gestiona asistencia, montos, pagos y cierre de un compromiso. |
| Perfil | Edita información personal, URL de foto, datos de banda para el administrador y salida de sesión. |

## Roles y permisos de interfaz

### Administrador

- Puede ver **Inicio**, **Eventos** e **Integrantes**.
- Puede crear compromisos, gestionar asistentes, pagos y miembros.
- Puede aprobar o rechazar solicitudes de registro.
- Puede actualizar la configuración de la banda desde el perfil.

### Músico

- Ve una interfaz simplificada con **Inicio** y **Eventos**.
- No ve la pestaña de integrantes, acciones de creación ni detalle administrativo de pagos.
- Puede actualizar sus propios datos de perfil cuando tenga una cuenta Firebase activa.

> La ocultación de controles en la interfaz no sustituye las reglas de Firebase. Las reglas de Realtime Database deben restringir también el acceso real a los datos.

## Flujo de solicitud y activación de cuenta

1. El interesado toca **Registrarse** desde el login.
2. Completa nombre, correo, celular e instrumento.
3. La app inicia una sesión anónima de Firebase si aún no existe una sesión.
4. Se crea un registro en `registrationRequests` con estado `pending`.
5. El administrador visualiza la solicitud en **Integrantes** y decide aprobar o rechazar.
6. Si se aprueba, el solicitante vuelve a abrir la app.
7. La app detecta el estado `approved`, muestra el campo de contraseña y crea la cuenta con Firebase Authentication.
8. Se guarda el perfil del músico en `users/{uid}` y un registro de integrante en `members/{memberId}`.

Este flujo evita almacenar contraseñas de personas que todavía esperan aprobación.

## Métricas del tablero

El panel de inicio no utiliza valores estáticos. Sus datos se recalculan al recibir cambios de Realtime Database:

- **Eventos**: compromisos cuya propiedad `isCompleted` es falsa.
- **Pagos**: asistencias cuyo valor `isPaid` es falso.
- **Tareas**: asistencias con estado `Pendiente`.
- **Avance**: proporción entre compromisos completados y compromisos totales.
- **Próximo evento**: primer compromiso pendiente recibido desde Firebase.
- **Actividad reciente**: resumen de eventos completados, pagos registrados e integrantes existentes.

## Fotos de perfil

La aplicación acepta una URL pública de imagen en el perfil. Coil descarga y muestra la imagen en:

- El avatar del panel principal.
- La tarjeta superior de la pantalla de perfil.

Para administradores locales, la URL se conserva también en preferencias del dispositivo. Para usuarios Firebase, se sincroniza en `users/{uid}/photoUrl`.

## Autenticación y sesiones

- **Correo y contraseña**: administrado por Firebase Authentication.
- **Sesión anónima**: se utiliza para enviar una solicitud antes de tener una cuenta aprobada.
- **Biometría**: confirma el acceso en el dispositivo cuando este cuenta con biometría configurada.
- **Cambio de usuario**: limpia el correo y contraseña del formulario para ingresar con otra cuenta.

## Configuración de reglas: desarrollo y producción

Las reglas actuales que exigen `auth != null` requieren que el acceso anónimo esté habilitado, porque un solicitante aún no posee una cuenta de correo/contraseña.

En producción no se recomienda permitir lectura y escritura general para cualquier usuario autenticado. Debes restringir las reglas según el propietario del dato y el rol. Por ejemplo, una solicitud debería poder ser creada por su solicitante, pero solo revisada por un administrador autenticado.

Antes de cambiar reglas en producción, pruébalas con Firebase Emulator Suite o Rules Playground. Un error `Permission denied` significa que la sesión actual no satisface las condiciones de las reglas configuradas.

## Solución de problemas

### `Firebase Database error: Permission denied`

1. Verifica que el dispositivo tenga conexión a Internet.
2. Confirma que `google-services.json` corresponda al mismo proyecto Firebase.
3. En Authentication, habilita **Anónimo** y **Correo electrónico/contraseña**.
4. Revisa las reglas de Realtime Database; la operación necesita cumplir la condición de `.read` o `.write`.
5. Vuelve a instalar la versión más reciente en ambos celulares después de actualizar el código.

### La solicitud no aparece en el administrador

1. Confirma que el solicitante vea el mensaje de solicitud enviada.
2. Confirma que ambos equipos usen el mismo archivo `google-services.json` y el mismo proyecto Firebase.
3. Inicia sesión como administrador y abre **Integrantes**.
4. Verifica en Realtime Database que exista `registrationRequests` con estado `pending`.

### No aparece una notificación Android

1. En Android 13 o superior, acepta el permiso de notificaciones al abrir la app.
2. En Ajustes del celular, habilita el canal **Solicitudes de registro** para BandPay.
3. La notificación local se muestra con la app activa o en segundo plano.
4. Para notificaciones cuando la app está cerrada se requiere FCM con un servicio de envío externo.

### La foto no carga

1. Usa una URL directa y pública de imagen, preferentemente HTTPS.
2. Guarda los cambios desde el perfil.
3. Comprueba que el teléfono tenga Internet.
4. Si la URL cambió en el mismo servidor, puede existir caché temporal de imágenes.

## Ejecutar localmente

1. Abre el proyecto en Android Studio.
2. Espera a que Gradle sincronice las dependencias.
3. Verifica que `app/google-services.json` corresponda a tu proyecto Firebase.
4. Conecta un celular o inicia un emulador.
5. Ejecuta la configuración `app`.

También puedes compilar desde la terminal:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Permisos de Android

- `INTERNET`: conexión con Firebase.
- `USE_BIOMETRIC`: acceso con huella.
- `POST_NOTIFICATIONS`: alertas locales de solicitudes, requerido en Android 13 o superior.

## Notificaciones

Actualmente se crean notificaciones locales cuando la app del administrador está activa o en segundo plano. Para recibirlas con la app completamente cerrada se debe integrar Firebase Cloud Messaging (FCM) con un servicio de envío confiable, como Cloud Functions o un servidor propio.

## Seguridad y publicación

- No publiques la app con reglas de Firebase abiertas.
- Usa cuentas Firebase reales para administradores en un entorno de producción.
- Define reglas que permitan al músico crear solo su propia solicitud y que restrinjan la aprobación a administradores autenticados.
- Revisa y actualiza las credenciales de Firebase antes de distribuir un APK.

## Próximas mejoras recomendadas

- Integrar Firebase Cloud Messaging (FCM) para alertas cuando la app esté cerrada.
- Implementar Cloud Functions o un backend para aprobar solicitudes y asignar roles de forma segura.
- Reemplazar los identificadores basados en tiempo por claves `push()` o UUID.
- Agregar pruebas de los flujos de autenticación, solicitudes, roles y reglas Firebase.
- Incorporar carga de fotos desde cámara o galería mediante Firebase Storage.
- Añadir búsqueda real, filtros avanzados y exportación de reportes de pagos.
- Añadir recuperación de contraseña y verificación de correo electrónico.

## Contribución

1. Crea una rama con prefijo `codex/`.
2. Mantén los cambios enfocados en una funcionalidad o corrección.
3. Ejecuta pruebas y revisa que no existan errores de formato.
4. No agregues archivos de credenciales, claves privadas o archivos `.env` al repositorio.
5. Documenta cualquier cambio de Firebase, dependencias o reglas de seguridad.

## Licencia

Este repositorio no incluye una licencia explícita. Agrega una licencia antes de reutilizar o distribuir el código públicamente.

## Pruebas

```powershell
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
```

Las pruebas instrumentadas requieren un emulador o dispositivo conectado.
