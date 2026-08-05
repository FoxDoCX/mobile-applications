# FoxDoCX Mobile Applications

Каталог мобильных приложений организации [FoxDoCX](https://github.com/FoxDoCX).

## Приложения

### [WifiMetro](https://github.com/FoxDoCX/WifiMetro)

Автоматическая авторизация в бесплатных Wi‑Fi сетях общественного транспорта (Москва, СПб и др.).

| | |
|---|---|
| **Последняя версия** | [2.8.3 FoxDoC](https://github.com/FoxDoCX/WifiMetro/releases/tag/v2.8.3) |
| **Платформа** | Android 8.0+ (targetSdk 36) |
| **Package** | `pw.thedrhax.mosmetro` |
| **Лицензия** | GPL-3.0 |
| **Исходники** | https://github.com/FoxDoCX/WifiMetro |

**Скачать APK:**

- [arm64-v8a](https://github.com/FoxDoCX/WifiMetro/releases/download/v2.8.3/WifiMetro-2.8.3-FoxDoC-android16-arm64-v8a-debug.apk) — большинство современных устройств
- [armeabi-v7a](https://github.com/FoxDoCX/WifiMetro/releases/download/v2.8.3/WifiMetro-2.8.3-FoxDoC-android16-armeabi-v7a-debug.apk) — 32-bit устройства

#### Стек сборки WifiMetro 2.8.3

| Компонент | Версия |
|---|---|
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.0 |
| compileSdk / targetSdk | 36 (Android 16) |
| minSdk | 26 (Android 8.0) |
| Java | 10 |

| Зависимость | Версия |
|---|---|
| AndroidX RecyclerView | 1.4.0 |
| AndroidX Work | 2.10.4 |
| OkHttp | 5.1.0 |
| jsoup | 1.21.2 |
| dnsjava | 3.6.3 |
| Sentry Android | 8.21.1 |
| libsu | 3.1.2 |

#### Происхождение

Форк на базе [mosmetro-android](https://github.com/mosmetro-android/mosmetro-android) → [XeonDead/mosmetro-android](https://github.com/XeonDead/mosmetro-android) → **FoxDoCX/WifiMetro**.

---

## Структура репозиториев

Каждое приложение публикуется в **отдельном репозитории**; этот репозиторий — индекс и точка входа.

| Репозиторий | Описание |
|---|---|
| [FoxDoCX/mobile-applications](https://github.com/FoxDoCX/mobile-applications) | Каталог (этот репозиторий) |
| [FoxDoCX/WifiMetro](https://github.com/FoxDoCX/WifiMetro) | WifiMetro — исходники и релизы |
