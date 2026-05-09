-keep class com.teledrive.android.data.** { *; }
-keep interface com.teledrive.android.telegram.TelegramGateway { *; }
-keep class org.drinkless.** { *; }
-keep class org.drinkless.tdlib.** { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
