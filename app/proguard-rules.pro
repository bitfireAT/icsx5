
# ProGuard usage for ICSdroid:
#       shrinking        yes (main reason for using ProGuard)
#       optimization     yes
#       obfuscation      no (ICSdroid is open-source)
#       preverification  no

-dontobfuscate

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# ical4j: ignore unused dynamic libraries
-dontwarn aQute.**
-dontwarn groovy.**                       # Groovy-based ContentBuilder not used
-dontwarn net.fortuna.ical4j.model.**
-dontwarn org.codehaus.groovy.**
-dontwarn org.apache.log4j.**             # ignore warnings from log4j dependency
-keep class net.fortuna.ical4j.** { *; }  # keep all model classes (properties/factories, created at runtime)
-keep class org.threeten.bp.** { *; }     # keep ThreeTen (for time zone processing)

# keep ICSdroid and ical4android
-keep class at.bitfire.** { *; }	# all ICSdroid code is required
