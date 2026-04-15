package com.example.snaptag

import android.app.Application
import com.example.snaptag.data.AppDatabase
import com.example.snaptag.data.ProductRepository

class SnapTagApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ProductRepository(database.productDao()) }
}
