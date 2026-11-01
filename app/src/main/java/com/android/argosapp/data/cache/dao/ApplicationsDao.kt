package com.android.argosapp.data.cache.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.android.argosapp.data.cache.entity.ApplicationEntity
import com.android.argosapp.data.cache.entity.ApplicationsCacheMetadata

@Dao
interface ApplicationsDao {
    @Query(
        """SELECT * FROM applications
            WHERE (:textFilter IS NULL
                OR medicineName LIKE '%' || :textFilter || '%' COLLATE NOCASE
                OR lotName LIKE '%' || :textFilter || '%' COLLATE NOCASE
                OR lotCode LIKE '%' || :textFilter || '%' COLLATE NOCASE
                OR earCode LIKE '%' || :textFilter || '%' COLLATE NOCASE)
            AND (:statusFilter IS NULL
                OR status = :statusFilter COLLATE NOCASE
                OR (:statusFilter = 'Concluida' AND status = 'Concluída'))
            ORDER BY applicationDate DESC, applicationTime DESC""",
    )
    suspend fun listAll(textFilter: String?, statusFilter: String?): List<ApplicationEntity>

    @Query("SELECT createdAt FROM applications_cache_metadata WHERE id = 1 LIMIT 1")
    suspend fun getCreatedAt(): Long?

    @Query("DELETE FROM applications")
    suspend fun clearApplications()

    @Insert
    suspend fun saveApplications(applications: List<ApplicationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadata(metadata: ApplicationsCacheMetadata)

    @Transaction
    suspend fun replaceAll(
        applications: List<ApplicationEntity>,
        metadata: ApplicationsCacheMetadata,
    ) {
        clearApplications()
        if (applications.isNotEmpty()) {
            saveApplications(applications)
        }
        saveMetadata(metadata)
    }

    @Transaction
    suspend fun clearAll() {
        clearApplications()
        clearMetadata()
    }

    @Query("DELETE FROM applications_cache_metadata")
    suspend fun clearMetadata()
}
