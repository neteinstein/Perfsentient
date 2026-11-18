# Report and allowlist DTOs are persisted as JSON via kotlinx.serialization: keep generated serializers.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class org.neteinstein.perfsentient.core.data.mapper.**$$serializer { *; }
-keepclassmembers class org.neteinstein.perfsentient.core.data.mapper.** {
    *** Companion;
}
-keepclasseswithmembers class org.neteinstein.perfsentient.core.data.mapper.** {
    kotlinx.serialization.KSerializer serializer(...);
}
