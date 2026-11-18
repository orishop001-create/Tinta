# Aturan R8 untuk build release Tinta.
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**

# kotlinx.serialization membuat serializer hasil kode generik; jaga agar tidak dibuang R8.
-keep,includedescriptorclasses class id.tinta.app.**$$serializer { *; }
-keepclassmembers class id.tinta.app.** {
    *** Companion;
}
-keepclasseswithmembers class id.tinta.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# BiometricPrompt memakai callback lewat refleksi pada sebagian versi Android.
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**
