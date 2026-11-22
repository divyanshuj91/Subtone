# Subtone Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
# Keep JNI exports
-keepclasseswithmembernames class * {
    native <methods>;
}
