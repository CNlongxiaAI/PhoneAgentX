# Add project specific ProGuard rules here.
-keep class com.PhoneAgentX.** { *; }
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
-keep,includedescriptorclasses class com.PhoneAgentX.**$$serializer { *; }
-keepclassmembers class com.PhoneAgentX.** {
    *** Companion;
}
-keepclasseswithmembers class com.PhoneAgentX.** {
    kotlinx.serialization.KSerializer serializer(...);
}