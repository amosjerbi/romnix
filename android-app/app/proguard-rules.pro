# Keep OkHttp and Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep BouncyCastle crypto provider
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep SSHJ
-keep class net.schmizz.** { *; }
-dontwarn net.schmizz.**
