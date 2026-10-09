# kotlinx.serialization ships consumer rules; these cover the generated serializers
# for our own @Serializable classes when R8 full mode is on.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.mrd.**$$serializer { *; }
-keepclassmembers class com.example.mrd.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.mrd.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit service interfaces are used via reflection/proxies.
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
