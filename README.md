# EjemplosJWTAndroid

Ejemplo educativo de autenticación con **JSON Web Tokens (JWT)** en Android:
una app en **Kotlin + Jetpack Compose** que consume una API REST con login,
renovación de tokens con rotación, cierre de sesión y autorización por roles.

> La app **no firma ni verifica** tokens: la clave secreta solo existe en el
> servidor. El cliente se limita a guardar los tokens emitidos (cifrados con
> el Android Keystore), adjuntarlos a cada petición y renovarlos cuando
> expiran.

## Endpoints que consume

| Método | Ruta                   | Uso en la app                                            |
| ------ | ---------------------- | -------------------------------------------------------- |
| POST   | `/api/auth/login`      | Pantalla de login: canjea credenciales por tokens         |
| POST   | `/api/auth/refresh`    | Automático ante un 401 y manual con “Renovar tokens”      |
| POST   | `/api/auth/logout`     | Botón “Cerrar sesión”: revoca el refresh token            |
| GET    | `/api/auth/me`         | Pestaña Sesión: claims del usuario autenticado            |
| GET    | `/api/products/public` | Pantalla inicial (sin sesión) y pestaña Productos          |
| GET    | `/api/products`        | Pestaña Productos, catálogo completo (requiere token)     |
| POST   | `/api/products`        | Pantalla “Nuevo producto” (solo rol Admin)                |

## El flujo central: renovación automática ante un 401

```mermaid
sequenceDiagram
    participant App
    participant API

    App->>API: GET /api/products (Bearer access token vencido)
    API-->>App: 401 Unauthorized
    Note over App: TokenAuthenticator entra en acción
    App->>API: POST /api/auth/refresh (refresh token)
    API-->>App: 200 · par de tokens nuevo (el anterior queda revocado)
    Note over App: guarda el par nuevo (rotación)
    App->>API: GET /api/products (Bearer access token nuevo)
    API-->>App: 200 OK
```

Si el refresh también falla (token revocado o expirado), la app borra la
sesión local y vuelve sola a la pantalla de login. Este flujo está cubierto
por tests unitarios con un servidor HTTP falso (MockWebServer).

## Estructura

El código separa la autenticación JWT de los productos, de las vistas y de
la configuración de Android:

```
com.carlosalbertoxw.ejemplos_jwt_android/
├── App.kt, MainActivity.kt   Configuración Android, cableado (AppContainer)
│                             y navegación (Navigation Compose)
├── auth/                     Todo lo relativo a JWT — sin nada de UI
│   ├── AuthApi.kt            login/refresh (cliente plano) y me/logout
│   ├── AuthModels.kt         LoginRequest, TokenPair, Me, Session…
│   ├── SessionStorage.kt     Contrato de almacenamiento (testeable con fakes)
│   ├── TokenStore.kt         Implementación: DataStore + caché en memoria
│   ├── TokenCipher.kt        Cifra los tokens con una clave del Keystore
│   ├── AuthInterceptor.kt    Adjunta "Authorization: Bearer …"
│   ├── TokenAuthenticator.kt Renovación automática ante un 401 (rotación)
│   ├── JwtDecoder.kt         Lee el payload del JWT (sin verificar firma)
│   └── AuthRepository.kt     Fachada: lo único que la UI conoce de auth
├── products/                 Recurso de productos — no sabe nada de JWT
│   ├── ProductsApi.kt        Catálogo público, completo y alta
│   ├── ProductModels.kt      Product, NewProduct
│   └── ProductsRepository.kt Fachada del recurso
├── core/                     Utilidades compartidas
│   ├── ApiFailure.kt         Errores de la API como datos + apiResult
│   └── MoshiProvider.kt      Instancia única de Moshi
└── ui/                       Pantallas Compose y sus ViewModels
    ├── PublicCatalogScreen   Primera pantalla: catálogo sin sesión
    ├── LoginScreen
    ├── HomeScreen            Pestañas Sesión y Productos (pull-to-refresh)
    ├── CreateProductScreen   Alta de productos (solo Admin)
    ├── ProductListCard       Lista compartida por ambos catálogos
    └── UiMessages.kt         Traduce errores/mensajes a strings.xml
```

Los ViewModels solo hablan con los repositorios; el paquete `products` usa
el cliente HTTP autenticado sin saber que existe JWT, porque el interceptor
y el authenticator del paquete `auth` adjuntan y renuevan los tokens de
forma transparente. El único lugar donde los paquetes se conocen entre sí
es `AppContainer`.

## Cómo ejecutarlo

1. Arranca una API compatible en el puerto 8000. Para probar desde un
   dispositivo físico debe escuchar en `0.0.0.0` (todas las interfaces),
   no solo en `localhost`.
2. Configura la URL base **sin tocar el código**, con la propiedad de Gradle
   `apiBaseUrl` (se inyecta como `BuildConfig.BASE_URL`):

   ```properties
   # gradle.properties
   apiBaseUrl=http://10.0.2.2:8000/
   ```

   - **Emulador**: `http://10.0.2.2:8000/` (el localhost de tu PC visto
     desde el emulador).
   - **Dispositivo físico**: la IP de tu PC en la red local (teléfono y PC
     en la misma red), p. ej. `http://192.168.1.75:8000/` — es el valor
     por defecto, definido en [app/build.gradle.kts](app/build.gradle.kts).

   Si usas otra IP, añádela también a
   [network_security_config.xml](app/src/main/res/xml/network_security_config.xml):
   el tráfico HTTP en claro solo está permitido hacia los hosts de
   desarrollo listados ahí.
3. Al abrir la app se muestra el catálogo público (sin precios). Con
   **Iniciar sesión** entra con los usuarios de prueba:
   `admin / Admin123!` (rol Admin) o `user / User123!` (rol User).
   Las listas se refrescan arrastrándolas hacia abajo (*pull-to-refresh*).

## Tests

```
./gradlew :app:testDebugUnitTest
```

- `TokenAuthenticatorTest` — el flujo 401 → refresh → reintento con un
  servidor HTTP falso (MockWebServer): renovación exitosa, refresh fallido
  (borra la sesión), límite de un reintento (sin bucles) y sin sesión.
- `JwtDecoderTest` — decodificación del payload Base64Url de un JWT.

## Notas de seguridad

Este es un proyecto educativo; aun así aplica varias prácticas reales:

- Los tokens se persisten **cifrados** con una clave AES/GCM del Android
  Keystore ([TokenCipher.kt](app/src/main/java/com/carlosalbertoxw/ejemplos_jwt_android/auth/TokenCipher.kt)):
  la clave no puede extraerse del dispositivo.
- `allowBackup="false"`: los tokens no salen del dispositivo en los
  respaldos automáticos del sistema.
- El tráfico HTTP en claro está **restringido a los hosts de desarrollo**;
  hacia cualquier otro host queda bloqueado. En producción la API debe ir
  por HTTPS.
- El log de peticiones (que incluye los tokens, útil para estudiar el
  flujo en Logcat) solo existe en compilaciones **debug**; en release no se
  registra nada.

## Solución de problemas

- **“No se pudo conectar con el servidor”** en el teléfono físico:
  - Verifica que la API escuche en `0.0.0.0`, no en `localhost`.
  - El Firewall de Windows puede bloquear conexiones entrantes al puerto
    8000: permite el puerto o la aplicación del servidor.
  - Teléfono y PC deben estar en la misma red Wi-Fi, y la IP de
    `apiBaseUrl` debe ser la actual de tu PC (`ipconfig`).
- **Funciona en el emulador pero no en el teléfono** (o al revés): revisa
  que la URL de `apiBaseUrl` corresponda al caso (10.0.2.2 vs IP local) y
  que el host esté en `network_security_config.xml`.
- **429 Demasiados intentos** al hacer login: la API limita los intentos
  fallidos; espera los segundos que indica el mensaje (encabezado
  `Retry-After`).
- **La sesión “se cierra sola”**: es el comportamiento esperado cuando el
  refresh token expira o fue revocado (p. ej. reutilización detectada tras
  la rotación): la app borra la sesión y vuelve al login.
