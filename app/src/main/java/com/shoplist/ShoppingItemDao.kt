package com.shoplist

import androidx.room.*
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Query("SELECT * FROM local_shopping_items WHERE groupId = :groupId")
    fun getItemsByGroup(groupId: String): Flow<List<LocalShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LocalShoppingItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LocalShoppingItem>)

    @Query("UPDATE local_shopping_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun updateItemCheckState(id: String, isChecked: Boolean)

    @Delete
    suspend fun deleteItem(item: LocalShoppingItem)

    @Query("DELETE FROM local_shopping_items WHERE groupId = :groupId")
    suspend fun deleteAllItemsByGroup(groupId: String)
}
