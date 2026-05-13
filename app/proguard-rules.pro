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

# LazySodium + JNA — required for GhostCrypto native crypto in release builds
-keep class com.sun.jna.** { *; }
-keep interface com.sun.jna.** { *; }
-keep class com.goterl.lazysodium.** { *; }
-dontwarn com.sun.jna.**
-dontwarn com.goterl.lazysodium.**
