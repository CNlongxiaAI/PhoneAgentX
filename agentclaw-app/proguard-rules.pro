# Add project specific ProGuard rules here.
-keep class com.agentclaw.** { *; }
-keepattributes *Annotation*

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.agentclaw.**$$serializer { *; }
-keepclassmembers class com.agentclaw.** {
    *** Companion;
}
-keepclasseswithmembers class com.agentclaw.** {
    kotlinx.serialization.KSerializer serializer(...);
}