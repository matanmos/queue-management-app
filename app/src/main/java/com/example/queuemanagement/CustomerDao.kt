package com.example.queuemanagement

import androidx.room.*
import com.example.queuemanagement.database.Customer

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
    @Query("SELECT EXISTS(SELECT 1 FROM customers WHERE name = :name)")
    suspend fun isCustomerNameExists(name: String): Boolean
}
