package com.kaoyan.studyassistant.data.local.dao

import androidx.room.*
import com.kaoyan.studyassistant.data.local.entity.Subject
import kotlinx.coroutines.flow.Flow

/**
 * 科目数据访问对象
 */
@Dao
interface SubjectDao {

    /** 查询所有科目，按创建时间排序 */
    @Query("SELECT * FROM subjects ORDER BY createdAt ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    /** 根据 ID 查询科目 */
    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): Subject?

    /** 插入科目，返回新行 ID */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    /** 更新科目 */
    @Update
    suspend fun updateSubject(subject: Subject)

    /** 删除科目（默认科目不应被删除，业务层控制） */
    @Delete
    suspend fun deleteSubject(subject: Subject)

    /** 查询科目数量 */
    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int

    /** 查询所有科目（同步，用于备份导出） */
    @Query("SELECT * FROM subjects ORDER BY createdAt ASC")
    suspend fun getAllSubjectsSync(): List<Subject>

    /** 根据名称查询科目（用于重名检测） */
    @Query("SELECT * FROM subjects WHERE name = :name LIMIT 1")
    suspend fun getSubjectByName(name: String): Subject?
}
