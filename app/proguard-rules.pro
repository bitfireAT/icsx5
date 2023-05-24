
# ProGuard usage for ICSx⁵:
#       shrinking        yes (main reason for using ProGuard)
#       optimization     yes
#       obfuscation      no (ICSx⁵ is open-source)
#       preverification  no

-dontobfuscate

# ical4j: keep all iCalendar properties/parameters (used via reflection)
-keep class net.fortuna.ical4j.** { *; }

# we use enum classes (https://www.guardsquare.com/en/products/proguard/manual/examples#enumerations)
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# keep ICSx⁵ and ical4android
-keep class at.bitfire.** { *; }	# all ICSx⁵ code is required

# Additional rules which are now required since missing classes can't be ignored in R8 anymore.
# [https://developer.android.com/build/releases/past-releases/agp-7-0-0-release-notes#r8-missing-class-warning]
-dontwarn groovy.**
-dontwarn java.beans.Transient
-dontwarn org.codehaus.groovy.**
-dontwarn org.joda.**
-dontwarn org.json.*
-dontwarn org.xmlpull.**
