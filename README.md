# FoxDoCX Mobile Applications

Каталог мобильных приложений организации [FoxDoCX](https://github.com/FoxDoCX).

## Приложения

### [WifiMetro](https://github.com/FoxDoCX/WifiMetro)

Автоматическая авторизация в бесплатных Wi‑Fi сетях общественного транспорта (Москва, СПб и др.).

| | |
|---|---|
| **Последняя версия** | [2.8.4 FoxDoC](https://github.com/FoxDoCX/WifiMetro/releases/tag/v2.8.4) |
| **Платформа** | Android 8.0+ (targetSdk 36) |
| **Package** | `pw.thedrhax.mosmetro` |
| **Лицензия** | GPL-3.0 |
| **Исходники** | https://github.com/FoxDoCX/WifiMetro |

**Скачать APK:**

- [arm64-v8a](https://github.com/FoxDoCX/WifiMetro/releases/download/v2.8.4/WifiMetro-2.8.4-FoxDoC-android16-arm64-v8a-debug.apk) — большинство современных устройств
- [armeabi-v7a](https://github.com/FoxDoCX/WifiMetro/releases/download/v2.8.4/WifiMetro-2.8.4-FoxDoC-android16-armeabi-v7a-debug.apk) — 32-bit устройства

#### Что нового в 2.8.4

- Фикс краша FGS при выключенном foreground-уведомлении
- `NEARBY_WIFI_DEVICES` для чтения SSID на Android 13+
- Overlay WebView через `TYPE_APPLICATION_OVERLAY`
- Пароли в Keystore, `allowBackup=false`
- Честный UX для сетей без silent-login

#### Стек сборки

| Компонент | Версия |
|---|---|
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.0 |
| compileSdk / targetSdk | 36 (Android 16) |
| minSdk | 26 (Android 8.0) |

---

## Структура репозиториев

| Репозиторий | Описание |
|---|---|
| [FoxDoCX/mobile-applications](https://github.com/FoxDoCX/mobile-applications) | Каталог (этот репозиторий) |
| [FoxDoCX/WifiMetro](https://github.com/FoxDoCX/WifiMetro) | WifiMetro — исходники и релизы |
