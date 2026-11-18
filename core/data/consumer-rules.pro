# Report and allowlist DTOs are persisted as JSON via kotlinx.serialization: keep generated serializers.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.neteinstein.perfsentient.core.data.mapper.**$$serializer { *; }
-keepclassmembers class com.neteinstein.perfsentient.core.data.mapper.** {
    *** Companion;
}
-keepclasseswithmembers class com.neteinstein.perfsentient.core.data.mapper.** {
    kotlinx.serialization.KSerializer serializer(...);
}
