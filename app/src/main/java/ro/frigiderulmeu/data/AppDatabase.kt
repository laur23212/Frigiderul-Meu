package ro.frigiderulmeu.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE status = 'ACTIVE' ORDER BY expiryDate ASC, id ASC")
    fun observeActive(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeById(id: Long): Flow<Item?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): Item?

    /** Pentru notificări (etapa 2): produsele active care expiră într-o anumită zi. */
    @Query("SELECT * FROM items WHERE status = 'ACTIVE' AND expiryDate = :epochDay")
    suspend fun getActiveExpiringOn(epochDay: Long): List<Item>

    @Query("SELECT COUNT(*) FROM items WHERE imagePath = :path")
    suspend fun countWithImage(path: String): Int

    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)
}

@Dao
interface ProductTypeDao {
    @Query("SELECT * FROM product_types WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductType?

    @Query("SELECT COUNT(*) FROM product_types WHERE imagePath = :path")
    suspend fun countWithImage(path: String): Int

    @Upsert
    suspend fun upsert(type: ProductType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(type: ProductType)
}

@Database(entities = [Item::class, ProductType::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun productTypeDao(): ProductTypeDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "frigiderul_meu.db"
                ).build().also { instance = it }
            }
    }
}
