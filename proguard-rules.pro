#-dontoptimize

-keepnames public !abstract class * extends android.app.Activity
-keepnames public !abstract class * extends android.app.Service
-keepnames public !abstract class * extends android.content.BroadcastReceiver
-dontwarn com.google.common.**
-dontwarn com.google.appengine.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.api.client.**
-assumenosideeffects class android.util.Log {
    *;
}
-keepnames class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepnames public !abstract class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep public class org.jsoup.** {
  public *;
}
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-dontwarn sun.**
-dontwarn java.**
-dontwarn javax.**
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }
-dontwarn org.apache.**
-keep class uk.co.chrisjenx.calligraphy.** { *; }
-keep class android.support.** { *; }
-keep class android.support.v4.os.CancellationSignal { *; }

-keep class com.firebase.** { *; }
-keepnames class org.apache.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class javax.servlet.** { *; }
-keepnames class org.ietf.jgss.** { *; }
-dontwarn org.apache.**
-dontwarn org.w3c.dom.**
-dontwarn android.**
-dontwarn javax.lang.model.element.Modifier

-keepattributes InnerClasses,EnclosingMethod

-keepattributes com.google.gson.annotations.**, com.nao20010128nao.Wisecraft.misc.ShouldBeKept
-keepclassmembers class ** {
	@com.google.gson.annotations.** <fields>;
}
-keepclassmembers class * implements com.nao20010128nao.Wisecraft.misc.ShouldBeKept2
-keepclassmembers @com.nao20010128nao.Wisecraft.misc.ShouldBeKept class *

-keepclassmembers public enum * { *; }

-repackageclasses wisecraft

-allowaccessmodification
-optimizationpasses 10

-useuniqueclassmembernames
