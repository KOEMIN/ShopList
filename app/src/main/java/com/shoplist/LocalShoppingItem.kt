package com.shoplist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_shopping_items")
data class LocalShoppingItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val isChecked: Boolean = false,
    val groupId: String = ""
)
