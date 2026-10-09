# Crashlytics: readable stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception

# Analytics screen and click names are derived from these class names
-keepnames class com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.** extends android.app.Activity
-keepnames class com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.** extends androidx.fragment.app.Fragment
-keepnames class com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.** extends android.view.View

# WorkManager creates its Room database and workers by reflection
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-repackageclasses ''
-allowaccessmodification

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
