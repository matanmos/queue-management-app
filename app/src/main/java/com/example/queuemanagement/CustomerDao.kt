package com.example.queuemanagement

import androidx.room.*

@Dao
interface CustomerDao {

    @Insert
    suspend fun insert(customer: Customer)

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)

    @Query("SELECT * FROM customers")
    suspend fun getAll(): List<Customer>
}
