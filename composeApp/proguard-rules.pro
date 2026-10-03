# Re-declared from com.revenuecat.purchases:purchases-store-amazon consumer rules, minus its
# `-dontoptimize` (that library's rules are ignored in build.gradle.kts → release.optimization).
-dontwarn com.amazon.**
-keep class com.amazon.** { *; }
-keepattributes *Annotation*
