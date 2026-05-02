package com.kaoyan.studyassistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.kaoyan.studyassistant.data.repository.SubjectRepository
import javax.inject.Inject

/**
 * 应用 Application 类
 * 使用 @HiltAndroidApp 启用 Hilt 依赖注入
 */
@HiltAndroidApp
class KaoyanApp : Application() {

    @Inject
    lateinit var subjectRepository: SubjectRepository

    override fun onCreate() {
        super.onCreate()
        // 首次启动时初始化默认科目
        CoroutineScope(Dispatchers.IO).launch {
            subjectRepository.initDefaultSubjects()
        }
    }
}
