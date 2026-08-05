# RemDoc

Лёгкое локальное Android-приложение для коротких напоминаний после разблокировки и других выбранных событий. Без аккаунта, интернета, рекламы и аналитики.

**Издатель:** FoxDoC  
**Package:** `ru.recalltoast.app`  
**minSdk:** 21 · **targetSdk/compileSdk:** 35

## Идея

Пользователь вводит короткий текст. Приложение показывает его:

1. после разблокировки (`ACTION_USER_PRESENT`);
2. опционально после завершения звонка;
3. по дополнительным лёгким событиям (зарядка, наушники, расписание, виджет, QS tile);
4. полностью офлайн;
5. с минимальной работой между событиями.

## Режимы

| Режим | Поведение |
|---|---|
| **Надёжный** | Foreground service + тихое уведомление «Напоминания активны» + динамический `USER_PRESENT` |
| **Экономичный** | Без постоянного FGS; manifest `USER_PRESENT` (exempt broadcast). На части OEM событие может не прийти после выгрузки |

## Способы показа

| Способ | Permission | Ограничения |
|---|---|---|
| System Toast | не нужен overlay | Android 11+ без custom Toast view; Android 12+ ~2 строки |
| Overlay card | `SYSTEM_ALERT_WINDOW` | только после явного согласия пользователя |

## Архитектура

Один модуль `app`, лёгкая clean architecture:

- `data/` — DataStore + JSON (`kotlinx.serialization`)
- `domain/` — модели, `ReminderSelector`, `ReminderScheduler`
- `service/` — FGS
- `receiver/` / `trigger/` — boot, unlock, optional events
- `telephony/` — call end detection без номеров и call log
- `display/` — Toast / overlay
- `ui/` — Jetpack Compose + Material 3

DI вручную через `RecallApplication` (без Hilt).

## Сборка

```bash
# Windows
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew.bat clean
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest
gradlew.bat lintDebug
gradlew.bat assembleRelease
```

Release включает `minifyEnabled` + `shrinkResources`.

## Разрешения

| Permission | Зачем | Когда спрашиваем |
|---|---|---|
| `POST_NOTIFICATIONS` | FGS notification (Android 13+) | при старте надёжного режима |
| `FOREGROUND_SERVICE` / `SPECIAL_USE` | надёжный режим | манифест |
| `SYSTEM_ALERT_WINDOW` | overlay card | только после выбора карточки |
| `READ_PHONE_STATE` | детект IDLE после звонка | только после включения «После звонка» |
| `RECEIVE_BOOT_COMPLETED` | восстановление режима | если включён автозапуск |
| **нет** `INTERNET` | — | — |

> **Google Play:** перед публикацией проверьте актуальную политику чувствительных разрешений (`READ_PHONE_STATE`, `SPECIAL_USE` FGS). Может потребоваться декларация в Play Console.

## Ограничения платформы

### Android 8+
Фоновые лимиты; FGS нужен для стабильного unlock в надёжном режиме.

### Android 11+
Custom Toast view из фона ограничен — используем только `Toast.makeText`.

### Android 12+
`ForegroundServiceStartNotAllowedException` при старте FGS из запрещённого фона. Toast обрезается примерно до 2 строк. Старт FGS только из UI / exempt context.

### Android 13+
`POST_NOTIFICATIONS` обязателен для показа уведомления сервиса.

### Android 14+
`foregroundServiceType=specialUse` + property subtype.

### После перезагрузки
Восстановление только если включены «автозапуск» и «включено». Иначе — честная инструкция, без обхода OEM.

## Совместимость

| Версия | Toast | Overlay | Notifications | FGS | Звонки | Автозапуск | Ограничения |
|---|---|---|---|---|---|---|---|
| 5–7 | OK | TYPE_PHONE | n/a | startForeground | PhoneStateListener | BOOT_COMPLETED | мало фоновых лимитов |
| 8–10 | OK | overlay / phone | channels | FGS + channel | PhoneStateListener | OK, батарея OEM | background limits |
| 11 | OK (no custom) | TYPE_APPLICATION_OVERLAY | channels | OK | PhoneStateListener | OEM | Toast restrictions |
| 12 | 2-line limit | overlay | channels | start restrictions | PhoneStateListener | OEM | FGS start rules |
| 13 | same | overlay | runtime POST_NOTIFICATIONS | same | PhoneStateListener | OEM | notification permission |
| 14 | same | overlay | same | specialUse type | TelephonyCallback on 31+ | OEM | FGS types |
| 15 / актуальная | same | overlay | same | stricter FGS | TelephonyCallback | OEM | проверяйте release notes |

OEM (Xiaomi, Huawei, Samsung, OnePlus, Oppo, Vivo, Realme): см. экран «Помощь по устройству».

## Приватность

- Нет INTERNET permission
- Нет аналитики / рекламы / облака
- Не читает номера, call log, уведомления, location, contacts, mic, camera
- Не использует AccessibilityService
- Экспорт/импорт через SAF без доступа ко всему хранилищу

## Тестирование на устройстве

1. Ввести текст → Preview.
2. Надёжный режим → Start → выдать уведомления при необходимости.
3. Заблокировать/разблокировать → сообщение.
4. Stop → разблокировка ничего не показывает.
5. Overlay: выдать permission → карточка.
6. После звонка: выдать READ_PHONE_STATE → завершить звонок.
7. Отказать в permission → приложение не падает.
8. Перезапуск приложения → настройки на месте.

## Подпись APK/AAB

```bash
gradlew.bat assembleRelease
# или
gradlew.bat bundleRelease
```

Настройте `signingConfigs` в `app/build.gradle.kts` своим keystore (не коммитьте ключи).

## Checklist перед Google Play

- [ ] Privacy policy URL
- [ ] Declarations: Phone permission, Special-use FGS
- [ ] Нет INTERNET / ads / trackers
- [ ] Data safety form: data not collected
- [ ] Screenshots + short/full description
- [ ] targetSdk актуальный
- [ ] Проверка на Xiaomi/Samsung минимум
- [ ] ProGuard/R8 не ломает serialization и receivers

## Лицензия

Самостоятельный продукт. Не связан с Toastr Pro и не копирует его ресурсы/название.
