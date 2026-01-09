# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.nanogpt.chat.data.remote.dto.** { *; }
-keep class com.nanogpt.chat.data.local.entity.** { *; }
-keep class kotlinx.serialization.json.** { *; }
-keep @kotlinx.serialization.Serializable class * {*;}
-dontwarn okhttp3.internal.**
-keep class okhttp3.internal.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
