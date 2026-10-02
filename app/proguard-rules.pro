# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes SourceFile, LineNumberTable

-keepnames class * extends android.app.Activity
-keepnames class * extends androidx.fragment.app.Fragment

-keep class androidx.appcompat.view.** { *; }
-keep class androidx.window.extensions.embedding.** { *; }
-keep class is.xyz.mpv.** { *; }
-keep class lis.xyz.mpv.** { *; }

-keepclasseswithmembernames class io.github.daisukikaffuchino.han1meviewer.ui.screen.video.VideoRouteHostScreenKt {
    native <methods>;
}

# Conscrypt reflects on this public method to enable ECH. R8 must not rename
# members or remove the method, or ECH silently falls back to the disabled policy.
-keep class io.github.daisukikaffuchino.han1meviewer.logic.network.ech.ConscryptEch$PolicyTrustManager { *; }

# The WebView bridge is called by name from injected JavaScript.
-keepclassmembers class io.github.daisukikaffuchino.han1meviewer.logic.network.ech.EchWebBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Rust exports this JNI symbol from libchino.so.
-keep class io.github.daisukikaffuchino.han1meviewer.logic.network.ech.HyEchH3 { *; }
-keepclasseswithmembernames class io.github.daisukikaffuchino.han1meviewer.logic.network.ech.HyEchH3 {
    native <methods>;
}
