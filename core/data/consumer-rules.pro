# Report DTOs are persisted as JSON via kotlinx.serialization: keep generated serializers.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.neteinstein.perfsentience.core.data.mapper.**$$serializer { *; }
-keepclassmembers class com.neteinstein.perfsentience.core.data.mapper.** {
    *** Companion;
}
-keepclasseswithmembers class com.neteinstein.perfsentience.core.data.mapper.** {
    kotlinx.serialization.KSerializer serializer(...);
}
