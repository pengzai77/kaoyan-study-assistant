# ── 项目通用规则 ──────────────────────────────────────────────────────────────
# 保留所有 data class / entity / DAO 以免 Room 和 Gson 反序列化失败
-keep class com.kaoyan.studyassistant.data.** { *; }
-keep class com.kaoyan.studyassistant.domain.** { *; }
-keep class com.kaoyan.studyassistant.service.** { *; }

# 保留 Hilt 生成的组件
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Retrofit / Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose — 保留 @Composable 方法名（调试用，可选）
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# DataStore
-keep class androidx.datastore.** { *; }

# 枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
