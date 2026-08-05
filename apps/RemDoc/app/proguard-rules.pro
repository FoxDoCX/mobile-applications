# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class ru.recalltoast.app.**$$serializer { *; }
-keepclassmembers class ru.recalltoast.app.** {
    *** Companion;
}
-keepclasseswithmembers class ru.recalltoast.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data models used in DataStore JSON
-keep class ru.recalltoast.app.domain.model.** { *; }

# TileService / receivers referenced from manifest
-keep class ru.recalltoast.app.tile.** { *; }
-keep class ru.recalltoast.app.receiver.** { *; }
-keep class ru.recalltoast.app.widget.** { *; }
-keep class ru.recalltoast.app.service.** { *; }
-keep class ru.recalltoast.app.trigger.** { *; }

-assumenosideeffects class ru.recalltoast.app.util.Logger {
    public static void d(...);
    public static void v(...);
}
