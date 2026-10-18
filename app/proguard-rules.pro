# ProGuard rules for Retrofit, Gson and Coil
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class com.sofamaven.musicrecognizer.data.model.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
