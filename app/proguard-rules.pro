# R8-Regeln für GlobeSwap. Die meisten Bibliotheken (AndroidX, Compose, OkHttp, Coil,
# kotlinx.serialization, WorkManager) bringen ihre eigenen Regeln mit; hier nur, was per
# Reflection erzeugt wird und deshalb nicht umbenannt oder entfernt werden darf.

# ViewModels werden von der Standard-Factory per Reflection erzeugt (leerer bzw. SavedStateHandle-Konstruktor).
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
    <init>(androidx.lifecycle.SavedStateHandle);
}

# WorkManager erzeugt den Worker per Reflection über den Klassennamen.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Datenklassen für die Server-Antworten (kotlinx.serialization): Namen der Felder sind das JSON-Format.
-keep,includedescriptorclasses class tech.eiki.globeswap.data.**$$serializer { *; }
-keepclassmembers class tech.eiki.globeswap.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Lesbare Stacktraces in Absturzberichten (Zeilennummern), Dateinamen werden verborgen.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
