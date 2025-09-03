
# ProGuard usage for ICSx⁵:
#       shrinking        yes (main reason for using ProGuard)
#       optimization     yes
#       obfuscation      no (ICSx⁵ is open-source)
#       preverification  no

-dontobfuscate

# okhttp3's internal util is not used by us
-dontwarn okhttp3.internal.Util

# we are not using those optional features of ical4j
-dontwarn org.jparsec.** # parser for filter expressions
-dontwarn javax.cache.** # timezone caching

# we use enum classes (https://www.guardsquare.com/en/products/proguard/manual/examples#enumerations)
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# keep ICSx⁵ and synctools
-keep class at.bitfire.** { *; }	# all ICSx⁵ code is required

# Additional rules which are now required since missing classes can't be ignored in R8 anymore.
# [https://developer.android.com/build/releases/past-releases/agp-7-0-0-release-notes#r8-missing-class-warning]
-dontwarn groovy.**
-dontwarn java.beans.Transient
-dontwarn org.codehaus.groovy.**
-dontwarn org.joda.**
-dontwarn org.json.*
-dontwarn org.xmlpull.**

# Seems to be incorrectly detected in https://github.com/ical4j/ical4j/blob/176fc84a4785d6f87be5dd18a20c6f83c6287b35/src/main/java/net/fortuna/ical4j/validate/schema/JsonSchemaValidator.java#L36C1-L36C39
# Should be fixed eventually by ical4j
# Commented in https://github.com/ical4j/ical4j/blob/176fc84a4785d6f87be5dd18a20c6f83c6287b35/build.gradle#L76
-dontwarn com.github.erosb.jsonsKema.*
