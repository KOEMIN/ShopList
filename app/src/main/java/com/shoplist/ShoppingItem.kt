package com.shoplist

import androidx.annotation.Keep

@Keep // Mencegah ProGuard/R8 merusak struktur nama variable saat rilis aplikasi
data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    @field:JvmField val isChecked: Boolean = false // Memastikan pemetaan Boolean Firestore aman
)