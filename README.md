# FoxDoCX Mobile Applications

Каталог и монорепозиторий мобильных приложений [FoxDoCX](https://github.com/FoxDoCX).

Здесь можно хранить исходники нескольких приложений в одной репе (`apps/<Name>/`)  
и/или ссылаться на отдельные репозитории (как WifiMetro).

## Структура

```text
mobile-applications/
├── README.md                 # этот каталог
├── .gitignore
└── apps/
    ├── RemDoc/               # RemDoc — исходники в этом репозитории
    └── <AnotherApp>/         # следующее приложение — просто добавьте папку
```

### Как добавить новое приложение

1. Создайте каталог `apps/<AppName>/`.
2. Положите туда Android/iOS-проект (без `build/`, `local.properties`, keystore и секретов).
3. Добавьте секцию в этот README (имя, описание, package, сборка, релизы).
4. Закоммитьте и запушьте в `main`.

Секреты (`*.jks`, `keystore.properties`, `local.properties`) **не коммитить** — они в `.gitignore`.

---

## Приложения

### [RemDoc](apps/RemDoc)

Локальные напоминания после разблокировки и других событий. Без интернета, рекламы и аналитики.

| | |
|---|---|
| **Расположение** | [`apps/RemDoc`](apps/RemDoc) |
| **Платформа** | Android 5.0+ (minSdk 21, targetSdk 35) |
| **Package** | `ru.recalltoast.app` |
| **Издатель** | FoxDoC |
| **UI** | Jetpack Compose + Material 3 |

**Сборка:**

```bash
cd apps/RemDoc
# Windows: set JAVA_HOME to Android Studio JBR
./gradlew assembleDebug
./gradlew assembleRelease   # нужен свой keystore / keystore.properties
```

Подробности: [`apps/RemDoc/README.md`](apps/RemDoc/README.md)

---

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

#### Стек сборки WifiMetro 2.8.4

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

## Карта репозиториев

| Репозиторий / путь | Описание |
|---|---|
| [FoxDoCX/mobile-applications](https://github.com/FoxDoCX/mobile-applications) | Каталог + исходники приложений в `apps/` |
| [`apps/RemDoc`](apps/RemDoc) | RemDoc (в этом репозитории) |
| [FoxDoCX/WifiMetro](https://github.com/FoxDoCX/WifiMetro) | WifiMetro — отдельный репозиторий и релизы |
