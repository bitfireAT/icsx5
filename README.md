
# ICSdroid

Official Web site: https://icsdroid.bitfire.at/ – If you link to ICSdroid, please
use this URL, which is a redirection to the current repository now, but may be a
"real" homepage at some time.

News on Twitter: [@icsdroidapp](https://twitter.com/icsdroidapp)

Help and discussion: [ICSdroid forum](https://icsdroid.bitfire.at/forums)

If you like ICSdroid, please [consider a donation](https://icsdroid.bitfire.at/donate).

ICSdroid is a Android 4.0+ app to subscribe to remote or local iCalendar files (like
time tables of your school/university or event files of your sports team).

It will download the iCalendar file at a certain (free-to-choose) interval
and synchronize them into Android-native read-only calendars which can then used
with your favorite calendar app.

No data (neither login data, nor calendar data, nor statistical or usage data)
is transferred to anywhere except the chosen server. No Google Calendar or
account required.


## How to get it

[![Available at Amazon](images/amazon.png)](http://www.amazon.com/gp/product/B0161BJKIY/ref=sr_1_1)
[![Get it on Google Play](images/play.png)](https://play.google.com/store/apps/details?id=at.bitfire.icsdroid)

The source code is available on Gitlab. Pre-built binaries are available on F-Droid.


## How to use

ICSdroid supports

* iCalendar files on the external storage (file:/// URLs)
* iCalendar resources at HTTP/HTTPS URLs

1. Subscribe to .ics calendars by clicking them in your browser (either http[s]://…/xxx.ics or
   webcal[s]:// URLs are supported) or local file manager. As an alternative, you can
   click on "+" in the ICSdroid main activity.
2. Enter a title for the calendar and select a color.
3. In the ICSdroid main activity, you can set your sync interval. Single-tap existing
   calendars to edit/delete them. 
4. Swipe the calendar list to force a synchronization.

Please note:

* User name and password for the calendars are stored in the calendar meta-data. Any
  app with calendar access might be able to read them.
* If you use a privacy guard, take into account that calendar access is necessary
  for ICSdroid to work.
* We have intentionally avoided per-resource sync intervals to keep things simple. Set
  the sync interval to the shortest required period of all your subscriptions.


## Implementation notes

For HTTP(S) resources, ICSdroid uses `ETag`/`If-None-Match` and
`Last-Modified`/`If-Modified-Since` to avoid repeated downloading of unchanged data.

When processing VEVENTs, ICSdroid uses the LAST-MODIFIED property to check whether
a resource (identified by UID) has changed. Only changed resources will be updated
in the calendar.

ICSdroid uses Android's `HttpURLConnection` which doesn't follow redirects to
different protocols (http:// to https:// or vice versa).


## License 

Copyright (C) 2013 – 2015 bitfire web engineering (Ricki Hirner, Bernhard Stockmann).

This program comes with ABSOLUTELY NO WARRANTY. This is free software, and you are welcome
to redistribute it under the conditions of the [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.html).

ICSdroid uses these libraries (more information available in the app):

* [Android Color Picker](https://github.com/yukuku/ambilwarna) (Apache License 2.0)
* [Android Support Library](https://developer.android.com/tools/support-library/) (Apache License 2.0)
* [Apache Commons](https://commons.apache.org) (Apache License 2.0)
* [ical4android](https://gitlab.com/bitfireAT/ical4android) (GPLv3)
  * [bnd, OSGI Core](http://bnd.bndtools.org) (Apache License 2.0)
  * [ical4j](https://github.com/ical4j/ical4j) (BSD 3-Clause License)
  * [slf4j](http://www.slf4j.org) (MIT License)
