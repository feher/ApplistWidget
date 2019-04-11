# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/feher/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

###### Crashlytics
# First of all, Fabric uses annotations internally, so add the following line to your configuration file:
-keepattributes *Annotation*

# Next, in order to provide the most meaningful crash reports, add the following line to your configuration file.
# Crashlytics will still function without this rule, but your crash reports will
# not include proper file names or line numbers:
-keepattributes SourceFile,LineNumberTable

# If you are using custom exceptions, add this line so that custom exception types are skipped during obfuscation:
#-keep public class * extends java.lang.Exception

# To skip running ProGuard on Crashlytics, just add the following to your ProGuard config file.
# This will help speed up your builds so that you can ship and test even faster.
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

###### Greenrobot Eventbus

-dontskipnonpubliclibraryclassmembers
-keep class de.greenrobot.event.** { *; }
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
-keep class * {
    @de.greenrobot.event.* <methods>;
}
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}

###### Android support lib
-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

###### Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

###### Kotlin
-dontwarn kotlinx.atomicfu.**
