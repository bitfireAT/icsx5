
# ProGuard usage for ICSdroid:
# 	shrinking		    yes - main reason for using ProGuard
# 	optimization	    no - too risky
# 	obfuscation		    no - ICSdroid is open-source
# 	preverification	    no (Android default)

-dontobfuscate

# ical4j: ignore unused dynamic libraries
-dontwarn groovy.**				# Groovy-based ContentBuilder not used
-dontwarn org.codehaus.groovy.**
-dontwarn org.apache.commons.logging.**		# Commons logging is not available
-dontwarn net.fortuna.ical4j.model.**		# ignore warnings from Groovy dependency
-keep class net.fortuna.ical4j.** { *; }	# keep all ical4j classes (properties/factories, created at runtime)

-dontwarn yuku.ambilwarna.widget.AmbilWarnaPrefWidgetView

# keep ICSdroid and ical4android
-keep class at.bitfire.** { *; }	# all ICSdroid code is required

# unneeded libraries
-dontwarn aQute.**
