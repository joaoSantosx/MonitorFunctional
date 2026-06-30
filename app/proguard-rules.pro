# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
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
# ====================================================================
# 1. BLINDAGEM DE MODELOS DE DADOS (CRUCIAL PARA O FIREBASE)
# ====================================================================

# Impede a ofuscação da sua classe de log de vídeos para que o Firebase consiga lê-la
-keep class com.jvf.monitorfunctional.ui.LogVideoApp { *; }

# Caso você decida criar um pacote exclusivo para seus modelos/classes de dados no futuro,
# esta regra já blinda o pacote inteiro de uma vez:
-keep class com.jvf.monitorfunctional.models.** { *; }

# Mantém propriedades e métodos que usem anotações do Firebase
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# ====================================================================
# 2. REGRAS PARA O COROUTINES E SUCESSOR ASSÍNCRONO
# ====================================================================

# O uso de .await() do kotlinx-coroutines-tasks exige que os estados internos sejam mantidos
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ====================================================================
# 3. REGRAS PARA JETPACK COMPOSE (CASO HOUVER REFLEXÃO DE CORES/ESTADOS)
# ====================================================================
-keepclassmembers class * extends androidx.compose.ui.node.ModifierNodeElement {
    <fields>;
    <methods>;
}

# ====================================================================
# 4. TRATAMENTO DE LOGS EM PRODUÇÃO (OPCIONAL - OTIMIZAÇÃO DE PERFORMANCE)
# ====================================================================
# Se você quiser remover completamente os Logs de Debug e Info da versão final da Play Store,
# economizando processamento e ocultando mensagens do Logcat para invasores, descomente as linhas abaixo:
# -assumenosideeffects class android.util.Log {
#     public static boolean isLoggable(java.lang.String, int);
#     public static int v(...);
#     public static int d(...);
#     public static int i(...);
# }