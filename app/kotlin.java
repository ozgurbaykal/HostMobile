-dontshrink
-dontoptimize
-dontobfuscate
-keep class kotlin.** { *; }

-keep class kotlin.reflect.** { *; }
-keep class org.jetbrains.kotlin.** { *; }

-dontwarn org.jetbrains.annotations.**

# Exclude specific jar (if applicable)
-keep class! ***.jetified-kotlin-reflect-1.8.22.jar
