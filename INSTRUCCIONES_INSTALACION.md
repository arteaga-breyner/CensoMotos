# Instrucciones de Instalación — Censo Motos Putumayo

## Requisitos del celular
- Android 8.0 o superior (Oreo, Pie, 10, 11, 12, 13, 14)
- Al menos 50 MB de espacio libre
- Micrófono funcional (para registro por voz)

---

## Paso 1 — Transferir el APK al celular

**Opción A — WhatsApp o correo:**
1. Envíese el archivo `CensoMotos.apk` por WhatsApp o correo electrónico
2. En el celular, abra el mensaje y descargue el archivo

**Opción B — Cable USB:**
1. Conecte el celular al computador con cable USB
2. En el celular seleccione "Transferencia de archivos"
3. Copie el APK a la carpeta Descargas del celular

**Opción C — Bluetooth / USB OTG:**
- Comparta el APK por Bluetooth desde otro celular que ya lo tenga

---

## Paso 2 — Habilitar instalación desde fuentes desconocidas

> Esto es necesario porque la app no está en la Play Store.

### Android 8.0 a 9 (Oreo / Pie):
1. Vaya a **Configuración → Seguridad**
2. Active **"Fuentes desconocidas"**

### Android 10 en adelante:
1. Abra el administrador de archivos o WhatsApp
2. Toque el archivo APK
3. Aparecerá un aviso — toque **"Configuración"**
4. Active **"Permitir desde esta fuente"**
5. Presione atrás y toque instalar de nuevo

---

## Paso 3 — Instalar la aplicación

1. Ubique el archivo `CensoMotos.apk` en la carpeta **Descargas**
2. Tóquelo para abrirlo
3. Presione **"Instalar"**
4. Espere a que termine (10-30 segundos)
5. Presione **"Abrir"** o busque el ícono 🏍️ en el menú

---

## Paso 4 — Permisos necesarios

Al abrir por primera vez, la app pedirá permisos:

| Permiso | Para qué se usa |
|---------|----------------|
| **Micrófono** | Registro de motos por voz |
| **Almacenamiento** | Guardar archivo Excel en Descargas |

→ Presione **"Permitir"** en ambos casos.

---

## Solución de problemas comunes

**"Instalación bloqueada"** → Siga el Paso 2 para habilitar fuentes desconocidas.

**"App no instalada"** → Verifique que el archivo no esté corrupto. Transfiéralo de nuevo.

**No aparece el ícono** → Busque "Censo" en el buscador de aplicaciones.

**Error al exportar Excel** → Verifique que permitió el permiso de almacenamiento en Configuración → Aplicaciones → Censo Motos → Permisos.

**Reconocimiento de voz no funciona** → El reconocimiento de voz de Android requiere conexión a internet **solo la primera vez** para configurarse. Después funciona sin internet.

---

## Compilar desde código fuente (desarrolladores)

```bash
# Requisitos: Android Studio Flamingo o superior, JDK 17

git clone <repositorio>
cd CensoMotos
./gradlew assembleRelease
# APK en: app/build/outputs/apk/release/app-release.apk
```

Para firma del APK:
```bash
./gradlew assembleRelease
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore mi-keystore.jks app-release-unsigned.apk mi-alias
zipalign -v 4 app-release-unsigned.apk CensoMotos.apk
```
