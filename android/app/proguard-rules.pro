-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class id.tinta.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class id.tinta.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
