# ============================================================
# REGLAS PROGUARD / R8 - Release
# ============================================================

# ---------- ERRORES GENERALES ----------
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes Exceptions
-dontwarn sun.misc.**

# ---------- MODELOS (Firestore usa reflexion via setter) ----------
# Los modelos se deserializan con toObject() de Firestore.
-keep class com.example.myapplication.models.** { *; }

# ---------- ROOM ----------
-keep class com.example.myapplication.room.** { *; }
-keepclassmembers class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# ---------- VIEWBINDING ----------
-keep class com.example.myapplication.databinding.** { *; }

# ---------- FIRESTORE / FIREBASE ----------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ---------- OSMDROID ----------
# osmdroid usa reflexion para(tile sources) y para los overlays.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-keep class org.osmdroid.tileprovider.tilesource.** { *; }

# ---------- MPANDROIDCHART ----------
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ---------- PDFBOX (Semaforo del Estudiante) ----------
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-dontwarn com.tom_roush.**

# ---------- GLIDE (fotos de usuario) ----------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }
-dontwarn com.bumptech.glide.**

# ---------- CIRCLEIMAGEVIEW ----------
-keep class de.hdodenhof.** { *; }
-dontwarn de.hdodenhof.**

# ---------- ANDROIDX ----------
-keep class androidx.annotation.** { *; }
-keep class androidx.fragment.app.** { *; }
-keep class androidx.recyclerview.widget.** { *; }
-keep class androidx.navigation.** { *; }
-dontwarn androidx.**

# ---------- KOTLIN ----------
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# ---------- ACTIVITIES / FRAGMENTS (Android los instancia por reflexion) ----------
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends androidx.fragment.app.Fragment
-keep class com.example.myapplication.** extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ---------- JSON (org.json usado en OSRM / Nominatim) ----------
-dontwarn org.json.**