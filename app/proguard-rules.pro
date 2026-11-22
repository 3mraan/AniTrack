# ProGuard / R8 keep rules for AniTrack Release builds
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keep,includedescriptorclasses class com.example.data.remote.**$$serializer { *; }
-keepclassmembers class com.example.data.remote.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.example.ui.navigation.**$$serializer { *; }
-keepclassmembers class com.example.ui.navigation.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.example.data.repository.**$$serializer { *; }

# Retrofit & OkHttp
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Firebase Firestore & Auth + Google Credential Manager
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class androidx.credentials.** { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.lang.model.element.Modifier

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coil
-dontwarn coil.**
