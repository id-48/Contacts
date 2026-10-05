# Crashlytics: readable stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception

# Analytics screen and click names are derived from these class names
-keepnames class com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.** extends android.app.Activity
-keepnames class com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.** extends androidx.fragment.app.Fragment
-keepnames class com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.** extends android.view.View

-repackageclasses ''
-allowaccessmodification

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
