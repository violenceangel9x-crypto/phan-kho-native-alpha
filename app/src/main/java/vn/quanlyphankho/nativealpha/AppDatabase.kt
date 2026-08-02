package vn.quanlyphankho.nativealpha

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String = "",
    val category: String = "Cán bộ",
    val active: Boolean = true
)

@Entity(tableName = "attendance", primaryKeys = ["date", "personId"])
data class AttendanceEntity(
    val date: String,
    val personId: Long,
    val morning: Boolean = false,
    val noon: Boolean = false,
    val evening: Boolean = false
)

@Entity(tableName = "menus")
data class MenuEntity(
    @PrimaryKey val date: String,
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = ""
)

@Entity(tableName = "finance_transactions")
data class FinanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val type: String,
    val category: String,
    val amount: Long,
    val note: String = ""
)

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val title: String,
    val completed: Boolean = false,
    val source: String = "manual"
)

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plateNumber: String,
    val driver: String = "",
    val unit: String = "",
    val active: Boolean = true
)

@Entity(tableName = "fuel_transactions")
data class FuelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateTime: String,
    val quantityLiters: Double,
    val unitPrice: Long = 0,
    val amount: Long = 0,
    val note: String = ""
)

@Dao
interface PhanKhoDao {
    @Query("SELECT * FROM people WHERE active = 1 ORDER BY category, name COLLATE NOCASE")
    fun observePeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun observeAttendance(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM menus WHERE date = :date LIMIT 1")
    fun observeMenu(date: String): Flow<MenuEntity?>

    @Query("SELECT * FROM finance_transactions WHERE date = :date ORDER BY id DESC")
    fun observeFinance(date: String): Flow<List<FinanceEntity>>

    @Query("SELECT * FROM todos WHERE date = :date ORDER BY completed, id")
    fun observeTodos(date: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM vehicles WHERE active = 1 ORDER BY plateNumber")
    fun observeVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM fuel_transactions ORDER BY dateTime DESC")
    fun observeFuel(): Flow<List<FuelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(item: PersonEntity): Long

    @Update
    suspend fun updatePerson(item: PersonEntity)

    @Query("UPDATE people SET active = 0 WHERE id = :id")
    suspend fun deactivatePerson(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(item: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenu(item: MenuEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinance(item: FinanceEntity)

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteFinance(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(item: TodoEntity)

    @Update
    suspend fun updateTodo(item: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodo(id: Long)

    @Query("SELECT * FROM people ORDER BY id")
    suspend fun allPeople(): List<PersonEntity>

    @Query("SELECT * FROM attendance ORDER BY date, personId")
    suspend fun allAttendance(): List<AttendanceEntity>

    @Query("SELECT * FROM menus ORDER BY date")
    suspend fun allMenus(): List<MenuEntity>

    @Query("SELECT * FROM finance_transactions ORDER BY date, id")
    suspend fun allFinance(): List<FinanceEntity>

    @Query("SELECT * FROM todos ORDER BY date, id")
    suspend fun allTodos(): List<TodoEntity>

    @Query("DELETE FROM attendance")
    suspend fun clearAttendance()

    @Query("DELETE FROM menus")
    suspend fun clearMenus()

    @Query("DELETE FROM finance_transactions")
    suspend fun clearFinance()

    @Query("DELETE FROM todos")
    suspend fun clearTodos()

    @Query("DELETE FROM people")
    suspend fun clearPeople()
}

@Database(
    entities = [
        PersonEntity::class,
        AttendanceEntity::class,
        MenuEntity::class,
        FinanceEntity::class,
        TodoEntity::class,
        VehicleEntity::class,
        FuelEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PhanKhoDatabase : RoomDatabase() {
    abstract fun dao(): PhanKhoDao

    companion object {
        fun create(context: Context): PhanKhoDatabase = Room.databaseBuilder(
            context.applicationContext,
            PhanKhoDatabase::class.java,
            "phan-kho-native.db"
        ).build()
    }
}
