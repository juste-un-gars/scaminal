# Scaminal ProGuard Rules

# Room
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * { @androidx.room.* <methods>; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# JSch SSH
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**
