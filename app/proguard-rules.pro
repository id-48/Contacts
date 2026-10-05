# Crashlytics: readable stack traces
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Analytics screen names are derived from these class names
-keepnames class com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.** extends android.app.Activity
-keepnames class com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.** extends androidx.fragment.app.Fragment
