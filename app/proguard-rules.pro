# ============================================
# PivotTranslator ProGuard / R8 Rules
# ============================================

# 디버깅을 위한 소스 파일·줄 번호 유지
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Retrofit2 ──
-keepattributes Signature,Exceptions,*Annotation*
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Retrofit의 Kotlin suspend 함수 지원에 필요
-keep class kotlin.coroutines.Continuation

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Gson ──
# Gson 리플렉션에 필요한 속성 유지
-keepattributes EnclosingMethod,InnerClasses
-dontwarn com.google.gson.**

# Gson으로 직렬화/역직렬화하는 네트워크 DTO 보존
-keep class com.tyua.pivottranslator.network.DeepL** { *; }

# ── Coroutines ──
-dontwarn kotlinx.coroutines.**
