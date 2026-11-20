package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    fun observeAllByRecent(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY LOWER(name) ASC, LOWER(companyName) ASC")
    fun observeAllByName(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY LOWER(companyName) ASC, LOWER(name) ASC")
    fun observeAllByCompany(): Flow<List<ContactEntity>>

    @Query(
        """
        SELECT * FROM contacts
        WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(companyName) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(designation) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(mobileNumber) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(email) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(companyWebsite) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(address) LIKE '%' || LOWER(:query) || '%'
        ORDER BY createdAt DESC
        """,
    )
    fun searchAllFields(query: String): Flow<List<ContactEntity>>

    @Query(
        """
        SELECT * FROM contacts
        WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%'
        ORDER BY LOWER(name) ASC
        """,
    )
    fun searchByName(query: String): Flow<List<ContactEntity>>

    @Query(
        """
        SELECT * FROM contacts
        WHERE LOWER(companyName) LIKE '%' || LOWER(:query) || '%'
        ORDER BY LOWER(companyName) ASC, LOWER(name) ASC
        """,
    )
    fun searchByCompany(query: String): Flow<List<ContactEntity>>

    @Query(
        """
        SELECT * FROM contacts
        WHERE LOWER(designation) LIKE '%' || LOWER(:query) || '%'
        ORDER BY LOWER(designation) ASC, LOWER(name) ASC
        """,
    )
    fun searchByDesignation(query: String): Flow<List<ContactEntity>>

    @Query("SELECT COUNT(*) FROM contacts")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun observeContactById(id: Long): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query(
        """
        SELECT * FROM contacts
        WHERE normalizedMobile != ''
          AND normalizedMobile = :normalizedMobile
          AND id != :excludeId
        LIMIT 1
        """,
    )
    suspend fun findByNormalizedMobile(normalizedMobile: String, excludeId: Long = -1L): ContactEntity?

    @Query(
        """
        SELECT * FROM contacts
        WHERE email != ''
          AND LOWER(email) = LOWER(:email)
          AND id != :excludeId
        LIMIT 1
        """,
    )
    suspend fun findByEmail(email: String, excludeId: Long = -1L): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity): Long

    @Update
    suspend fun update(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
