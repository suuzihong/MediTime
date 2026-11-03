# 保留序列化模型，避免 release 混淆影响 JSON 字段
-keepclassmembers class com.meditime.app.data.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
