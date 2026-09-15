# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.personal.fittrack.**$$serializer { *; }
-keepclassmembers class com.personal.fittrack.** {
    *** Companion;
}
-keepclasseswithmembers class com.personal.fittrack.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ML Kit
-keep class com.google.mlkit.** { *; }
