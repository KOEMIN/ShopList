package com.shoplist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk tabel "local_shopping_items".
 * Berisi definisi query SQL dan operasi database lokal.
 */
@Dao
interface ShoppingItemDao {

    // Mengambil semua barang belanjaan yang memiliki groupId tertentu secara reaktif menggunakan Flow
    @Query("SELECT * FROM local_shopping_items WHERE groupId = :groupId")
    fun getItemsByGroup(groupId: String): Flow<List<LocalShoppingItem>>

    // Menyisipkan satu barang belanjaan baru (mengganti jika ada konflik ID yang sama)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LocalShoppingItem)

    // Menyisipkan daftar barang belanjaan sekaligus (misal saat sinkronisasi snapshot Firestore)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LocalShoppingItem>)

    // Mengubah status centang (isChecked) barang secara lokal berdasarkan ID-nya
    @Query("UPDATE local_shopping_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun updateItemCheckState(id: String, isChecked: Boolean)

    // Menghapus barang belanjaan tertentu dari database lokal
    @Delete
    suspend fun deleteItem(item: LocalShoppingItem)

    // Menghapus seluruh cache barang belanjaan lokal milik suatu grup
    @Query("DELETE FROM local_shopping_items WHERE groupId = :groupId")
    suspend fun deleteAllItemsByGroup(groupId: String)
}
