# ---------------------------------------------------------------------------------------------
# Persentient R8 configuration.
# Library modules ship their own consumer rules (core/data, feature/monitoring); AndroidX, Room,
# WorkManager, Koin and kotlinx.serialization bundle the rules they need.
# ---------------------------------------------------------------------------------------------

# Obfuscate aggressively: flatten packages and allow R8 to widen access when inlining.
-repackageclasses ''
-allowaccessmodification

# Keep line numbers for readable crash stack traces, but hide the original file names.
# Upload build/outputs/mapping/release/mapping.txt to Play Console to de-obfuscate.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip verbose/debug logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Enums are persisted by name (DataStore / Room JSON payload) – keep their names stable.
-keepclassmembers enum com.neteinstein.persentient.core.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Coroutines: no debug agent in release.
-dontwarn kotlinx.coroutines.debug.**
