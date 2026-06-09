package com.shoplist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Konfigurasi utama Room Database untuk aplikasi.
 * Menampung entitas LocalShoppingItem dan mendefinisikan versi schema.
 */
@Database(entities = [LocalShoppingItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Menyediakan akses ke DAO ShoppingItemDao
    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        // Volatile memastikan perubahan nilai INSTANCE langsung terlihat oleh thread lain
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Mengembalikan instance singleton dari AppDatabase.
         * Jika database belum dibuat, builder akan membuat instance baru secara aman (thread-safe).
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shoplist_database"
                )
                // Jika versi schema berubah tanpa adanya objek migrasi, Room akan membersihkan/membuat ulang tabel
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
