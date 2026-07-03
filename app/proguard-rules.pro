# ====================================================================================
# PROGUARD & R8 SECURITY ENFORCEMENT CONFIGURATION (Production Hardened Mode)
# ====================================================================================

# 1. ATTRIBUTE RETENTION & COMPLIANCE
# Preserve annotation runtime metrics, signatures for reflection, and exception traceback records.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

# Preserve line numbers inside crash trace records for secure obfuscated analytics parsing.
-renamesourcefileattribute SourceFile

# 2. FINTECH MUTEX & TRANSACTIONAL INTEGRITY GATEWAYS
# Ensure that atomic lock operations, audit registries, and secure log gateways are preserved from
# obfuscation or inlining to prevent transaction-handling failures.
-keep class com.example.service.FintechWalletHandler {
    public *;
}
-keep class com.example.service.GoldTokenService {
    public *;
}
-keep class com.example.service.GoldTokenAuditLogger {
    public *;
}
-keep class com.example.service.SecureLogger {
    public *;
}

# 3. ENTITY MODELS & ENTERPRISE ROOM STATE PERSISTENCE
# Safeguard Room tables, column indices, database schemas, and primitive entity structures from being fully minimized.
-keep @androidx.room.Entity class * { *; }
-keep class com.example.data.model.** { *; }
-keep class com.example.data.database.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * implements androidx.room.RoomOpenHelper { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# 4. FIRESTORE, RETENTION & DISCOVERY SERIALIZATION CONFIGURATION
# Safeguard JSON, flow stream models, and retention metadata classes from being stripped down or having
# variable names altered by matching serializable keys.
-keep class com.example.service.AestheticInterestProfile {
    <fields>;
    <methods>;
}
-keep class com.example.service.MicroRetentionMetric {
    <fields>;
    <methods>;
}
-keep class com.example.service.RecommendedFeedItem {
    <fields>;
    <methods>;
}

# 5. CORE STANDARDIZED KOTLIN & DELEGATE ARCHITECTURE
# Guard standard serializable capabilities and dynamic companion objects.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep companion objects that hold crucial default instantiation functions.
-keepclassmembers class * {
    @kotlin.jvm.JvmField public static final *;
    public static final ** Companion;
}

# ====================================================================================
# 6. OKHTTP, RETROFIT & MOSHI API PACKAGES NETWORK HARDENING
# ====================================================================================
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

# Guard Moshi dynamic adapters and all on-chain verification payloads
-keep class com.example.data.model.** { *; }
-keep class com.example.service.** { *; }
-dontwarn okio.**
-dontwarn javax.annotation.**

# Preserve all HTTP dynamic annotations on endpoints
-keepclassmembers class * {
    @retrofit2.http.** <methods>;
}

# ====================================================================================
# 7. RESOURCE PRESERVATION FOR DYNAMIC RESOURCE LOADING
# ====================================================================================
# Preserve all R resource ID classes to prevent drawable/resource stripping
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep all drawable and resource references accessible
-keep class **.R { *; }
-keep class **.R$drawable { *; }
-keep class **.R$raw { *; }
-keep class **.R$mipmap { *; }
-keep class **.R$layout { *; }
-keep class **.R$string { *; }
-keep class **.R$color { *; }
-keep class **.R$id { *; }
-keep class **.R$attr { *; }
-keep class **.R$style { *; }
