-keep class com.teledrive.android.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
