# PlayScore 🎮⚽

Un proyecto de **Kotlin Multiplatform** para gestionar y seguir puntuaciones de juegos y deportes de manera multiplataforma.

## 📱 Plataformas Soportadas

- **Android** 📱
- **Desktop** 🖥️ (JVM)

## 🚀 Características

- Interfaz de usuario nativa con **Compose Multiplatform**
- Arquitectura multiplataforma compartida
- Gestión de puntuaciones en tiempo real
- Sincronización entre dispositivos

## 🛠️ Tecnologías Utilizadas

- **Kotlin Multiplatform Mobile (KMM)**
- **Compose Multiplatform** para UI
- **Kotlin/JVM** para Desktop
- **Android SDK** para Android

## 📋 Requisitos Previos

### General
- **JDK 17** o superior
- **Android Studio** (Arctic Fox o superior)
- **Kotlin** 1.9.0+

### Para Desktop
- **JVM** 17+
- **Servidor backend ejecutándose** ⚠️

## ⚠️ Importante para Desktop

**Para que la aplicación de escritorio funcione correctamente, es necesario tener el servidor backend ejecutándose.**

Asegúrate de:
1. Iniciar el servidor antes de ejecutar la aplicación desktop
2. Verificar que el servidor esté ejecutándose en el puerto correcto
3. Comprobar la conectividad de red

## 🚀 Instalación y Configuración

### 1. Clonar el repositorio
```bash
git clone https://github.com/pablisEsp/PlayScore.git
cd PlayScore
```

### 2. Configurar el entorno

#### Android
```bash
# Abrir en Android Studio
# Sincronizar Gradle
# Ejecutar en dispositivo/emulador Android
```

#### Desktop
```bash
# 1. PRIMERO: Iniciar el servidor backend
./gradlew :server:run

# 2. En otra terminal, ejecutar la aplicación desktop
./gradlew :composeApp:run
```

## 🏃‍♂️ Ejecución

### Android
```bash
./gradlew :composeApp:installDebug
```

### Desktop
```bash
# ⚠️ IMPORTANTE: Asegúrate de que el servidor esté corriendo primero
./gradlew :composeApp:runDistributable
```

## 📁 Estructura del Proyecto

```
PlayScore/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/     # Código compartido
│   │   ├── androidMain/    # Código específico Android
│   │   └── desktopMain/    # Código específico Desktop
│   └── build.gradle.kts
├── server/                 # Servidor backend (requerido para Desktop)
├── gradle/
└── build.gradle.kts
```

## 🔧 Configuración del Servidor

Para la aplicación desktop, necesitas configurar y ejecutar el servidor:

1. **Navegar al directorio del servidor:**
   ```bash
   cd server/
   ```

2. **Instalar dependencias y ejecutar:**
   ```bash
   ./gradlew run
   ```

3. **Verificar que esté ejecutándose:**
  - El servidor debería estar disponible en `http://localhost:8080` (o el puerto configurado)
  - Verifica los logs para confirmar el inicio exitoso

## 🤝 Contribuir

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📝 Notas de Desarrollo

### Para Desktop
- La aplicación desktop requiere conexión al servidor backend
- Asegúrate de que los puertos estén libres antes de ejecutar
- Revisa los logs del servidor para diagnosticar problemas de conectividad

### Para Móvil
- Las aplicaciones móviles pueden funcionar de forma independiente
- La sincronización con el servidor es opcional pero recomendada

## 🐛 Solución de Problemas

### Desktop no conecta al servidor
1. Verifica que el servidor esté ejecutándose
2. Comprueba la configuración de puertos
3. Revisa los logs de ambas aplicaciones
4. Verifica la configuración de firewall

### Problemas de compilación
1. Limpia el proyecto: `./gradlew clean`
2. Verifica las versiones de Kotlin y Gradle
3. Sincroniza las dependencias

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

## 👨‍💻 Autor

**Pablo** - [@pablisEsp](https://github.com/pablisEsp)

---

⭐ ¡No olvides dar una estrella al proyecto si te ha sido útil!