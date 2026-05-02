package com.kaoyan.studyassistant.data.repository

import com.kaoyan.studyassistant.data.local.dao.SubjectDao
import com.kaoyan.studyassistant.data.local.entity.Subject
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 科目 Repository
 * 封装科目相关的数据操作，ViewModel 通过此类访问数据
 */
@Singleton
class SubjectRepository @Inject constructor(
    private val subjectDao: SubjectDao
) {
    /** 获取所有科目的 Flow */
    fun getAllSubjects(): Flow<List<Subject>> = subjectDao.getAllSubjects()

    /** 根据 ID 获取科目 */
    suspend fun getSubjectById(id: Long): Subject? = subjectDao.getSubjectById(id)

    /** 根据名称获取科目（用于重名检测） */
    suspend fun getSubjectByName(name: String): Subject? = subjectDao.getSubjectByName(name)

    /**
     * 新增科目
     * @return 新行 ID，若名称已存在则抛出异常（由 Room UNIQUE 约束触发）
     */
    suspend fun addSubject(name: String, color: String = "#4A90D9"): Long {
        val subject = Subject(name = name, color = color)
        return subjectDao.insertSubject(subject)
    }

    /** 更新科目 */
    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)

    /** 删除科目（默认科目不可删除） */
    suspend fun deleteSubject(subject: Subject) {
        if (!subject.isDefault) {
            subjectDao.deleteSubject(subject)
        }
    }

    /** 初始化默认科目（首次启动时调用） */
    suspend fun initDefaultSubjects() {
        if (subjectDao.getSubjectCount() == 0) {
            val defaults = listOf(
                Subject(name = "政治", color = "#E53935", isDefault = true),
                Subject(name = "英语", color = "#1E88E5", isDefault = true),
                Subject(name = "数学", color = "#43A047", isDefault = true),
                Subject(name = "专业课", color = "#FB8C00", isDefault = true)
            )
            defaults.forEach { subjectDao.insertSubject(it) }
        }
    }

    /** 同步查询所有科目（用于备份导出） */
    suspend fun getAllSubjectsSync(): List<Subject> = subjectDao.getAllSubjectsSync()
}
