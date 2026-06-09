package com.shoplist

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representasi tabel "local_shopping_items" di database SQLite lokal (Room).
 * Digunakan sebagai cache lokal untuk menyimpan data barang belanjaan per grup.
 */
@Entity(tableName = "local_shopping_items")
data class LocalShoppingItem(
    // ID unik barang belanjaan (disinkronkan dengan ID dokumen Firestore)
    @PrimaryKey
    val id: String,
    
    // Nama barang belanjaan
    val name: String,
    
    // Status apakah barang sudah dicentang/dibeli
    val isChecked: Boolean = false,
    
    // ID grup belanja tempat barang ini berada (untuk pengelompokan cache lokal)
    val groupId: String = ""
)
