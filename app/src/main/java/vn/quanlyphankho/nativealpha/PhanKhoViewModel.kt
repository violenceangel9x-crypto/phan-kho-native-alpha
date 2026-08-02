package vn.quanlyphankho.nativealpha

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


data class MealPrices(
    val morning: Long = 20_000,
    val noon: Long = 35_000,
    val evening: Long = 35_000
)

class PhanKhoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PhanKhoApplication
    private val dao = app.database.dao()
    private val prefs = app.getSharedPreferences("phan_kho_native_settings", Context.MODE_PRIVATE)

    private val selectedDate = MutableStateFlow(LocalDate.now().toString())
    private val _status = MutableStateFlow("Native Alpha 0.2")
    private val _mealPrices = MutableStateFlow(
        MealPrices(
            morning = prefs.getLong("price_morning", 20_000),
            noon = prefs.getLong("price_noon", 35_000),
            evening = prefs.getLong("price_evening", 35_000)
        )
    )

    val date: StateFlow<String> = selectedDate
    val status: StateFlow<String> = _status
    val mealPrices: StateFlow<MealPrices> = _mealPrices

    val people = dao.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val attendance = selectedDate.flatMapLatest { dao.observeAttendance(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val menu = selectedDate.flatMapLatest { dao.observeMenu(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val finance = selectedDate.flatMapLatest { dao.observeFinance(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val todos = selectedDate.flatMapLatest { dao.observeTodos(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDate(value: String) {
        runCatching { LocalDate.parse(value) }.onSuccess { selectedDate.value = it.toString() }
    }

    fun previousDay() {
        selectedDate.value = LocalDate.parse(selectedDate.value).minusDays(1).toString()
    }

    fun nextDay() {
        selectedDate.value = LocalDate.parse(selectedDate.value).plusDays(1).toString()
    }

    fun addPerson(name: String, role: String, category: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        dao.insertPerson(
            PersonEntity(
                name = name.trim(),
                role = role.trim(),
                category = category.ifBlank { "Cán bộ" }
            )
        )
        _status.value = "Đã thêm ${name.trim()}"
    }

    fun updatePerson(person: PersonEntity, name: String, role: String, category: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        dao.updatePerson(person.copy(name = name.trim(), role = role.trim(), category = category))
        _status.value = "Đã cập nhật thành viên"
    }

    fun deactivatePerson(person: PersonEntity) = viewModelScope.launch {
        dao.deactivatePerson(person.id)
        _status.value = "Đã ẩn ${person.name}"
    }

    fun setMeal(personId: Long, meal: String, checked: Boolean) = viewModelScope.launch {
        val current = attendance.value.firstOrNull { it.personId == personId }
            ?: AttendanceEntity(selectedDate.value, personId)
        val updated = when (meal) {
            "morning" -> current.copy(morning = checked)
            "noon" -> current.copy(noon = checked)
            else -> current.copy(evening = checked)
        }
        dao.insertAttendance(updated)
    }

    fun setAllMeal(meal: String, checked: Boolean) = viewModelScope.launch {
        people.value.forEach { person ->
            val current = attendance.value.firstOrNull { it.personId == person.id }
                ?: AttendanceEntity(selectedDate.value, person.id)
            val updated = when (meal) {
                "morning" -> current.copy(morning = checked)
                "noon" -> current.copy(noon = checked)
                else -> current.copy(evening = checked)
            }
            dao.insertAttendance(updated)
        }
        _status.value = if (checked) "Đã chọn tất cả" else "Đã bỏ chọn tất cả"
    }

    fun clearAttendanceForDay() = viewModelScope.launch {
        people.value.forEach { person ->
            dao.insertAttendance(AttendanceEntity(selectedDate.value, person.id))
        }
        _status.value = "Đã bỏ chấm toàn bộ ngày"
    }

    fun saveMenu(breakfast: String, lunch: String, dinner: String) = viewModelScope.launch {
        dao.insertMenu(
            MenuEntity(
                date = selectedDate.value,
                breakfast = breakfast.trim(),
                lunch = lunch.trim(),
                dinner = dinner.trim()
            )
        )
        _status.value = "Đã lưu thực đơn ${displayDate(selectedDate.value)}"
    }

    fun addExpense(amount: Long, note: String, category: String) = viewModelScope.launch {
        if (amount <= 0) return@launch
        dao.insertFinance(
            FinanceEntity(
                date = selectedDate.value,
                type = "expense",
                category = category.ifBlank { "Thực phẩm" },
                amount = amount,
                note = note.trim()
            )
        )
        _status.value = "Đã lưu khoản chi"
    }

    fun deleteExpense(item: FinanceEntity) = viewModelScope.launch {
        dao.deleteFinance(item.id)
        _status.value = "Đã xóa giao dịch"
    }

    fun addTodo(title: String) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        dao.insertTodo(TodoEntity(date = selectedDate.value, title = title.trim()))
    }

    fun toggleTodo(item: TodoEntity) = viewModelScope.launch {
        dao.updateTodo(item.copy(completed = !item.completed))
    }

    fun deleteTodo(item: TodoEntity) = viewModelScope.launch {
        dao.deleteTodo(item.id)
    }

    fun saveMealPrices(morning: Long, noon: Long, evening: Long) {
        val prices = MealPrices(
            morning = morning.coerceAtLeast(0),
            noon = noon.coerceAtLeast(0),
            evening = evening.coerceAtLeast(0)
        )
        prefs.edit()
            .putLong("price_morning", prices.morning)
            .putLong("price_noon", prices.noon)
            .putLong("price_evening", prices.evening)
            .apply()
        _mealPrices.value = prices
        _status.value = "Đã lưu đơn giá bữa ăn"
    }

    fun backup(uri: Uri) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val root = JSONObject()
                    .put("format", "phan-kho-native-alpha")
                    .put("version", 2)
                    .put("exportedAt", System.currentTimeMillis())
                    .put(
                        "settings",
                        JSONObject()
                            .put("morningPrice", _mealPrices.value.morning)
                            .put("noonPrice", _mealPrices.value.noon)
                            .put("eveningPrice", _mealPrices.value.evening)
                    )
                    .put("people", JSONArray().apply {
                        dao.allPeople().forEach { person ->
                            put(
                                JSONObject()
                                    .put("id", person.id)
                                    .put("name", person.name)
                                    .put("role", person.role)
                                    .put("category", person.category)
                                    .put("active", person.active)
                            )
                        }
                    })
                    .put("attendance", JSONArray().apply {
                        dao.allAttendance().forEach { item ->
                            put(
                                JSONObject()
                                    .put("date", item.date)
                                    .put("personId", item.personId)
                                    .put("morning", item.morning)
                                    .put("noon", item.noon)
                                    .put("evening", item.evening)
                            )
                        }
                    })
                    .put("menus", JSONArray().apply {
                        dao.allMenus().forEach { item ->
                            put(
                                JSONObject()
                                    .put("date", item.date)
                                    .put("breakfast", item.breakfast)
                                    .put("lunch", item.lunch)
                                    .put("dinner", item.dinner)
                            )
                        }
                    })
                    .put("finance", JSONArray().apply {
                        dao.allFinance().forEach { item ->
                            put(
                                JSONObject()
                                    .put("id", item.id)
                                    .put("date", item.date)
                                    .put("type", item.type)
                                    .put("category", item.category)
                                    .put("amount", item.amount)
                                    .put("note", item.note)
                            )
                        }
                    })
                    .put("todos", JSONArray().apply {
                        dao.allTodos().forEach { item ->
                            put(
                                JSONObject()
                                    .put("id", item.id)
                                    .put("date", item.date)
                                    .put("title", item.title)
                                    .put("completed", item.completed)
                                    .put("source", item.source)
                            )
                        }
                    })

                app.contentResolver.openOutputStream(uri, "w")!!.bufferedWriter().use {
                    it.write(root.toString(2))
                }
            }
        }.onSuccess { _status.value = "Đã sao lưu toàn bộ dữ liệu" }
            .onFailure { _status.value = "Lỗi sao lưu: ${it.message}" }
    }

    fun restore(uri: Uri) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val text = app.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                val idMap = mutableMapOf<Long, Long>()

                dao.clearAttendance()
                dao.clearMenus()
                dao.clearFinance()
                dao.clearTodos()
                dao.clearPeople()

                root.optJSONArray("people")?.let { array ->
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        val oldId = item.optLong("id", 0)
                        val inserted = dao.insertPerson(
                            PersonEntity(
                                id = oldId,
                                name = item.optString("name"),
                                role = item.optString("role"),
                                category = item.optString("category", "Cán bộ"),
                                active = item.optBoolean("active", true)
                            )
                        )
                        idMap[oldId] = if (oldId > 0) oldId else inserted
                    }
                }

                root.optJSONArray("attendance")?.let { array ->
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        val oldPersonId = item.optLong("personId")
                        val personId = idMap[oldPersonId] ?: oldPersonId
                        dao.insertAttendance(
                            AttendanceEntity(
                                date = item.optString("date"),
                                personId = personId,
                                morning = item.optBoolean("morning"),
                                noon = item.optBoolean("noon"),
                                evening = item.optBoolean("evening")
                            )
                        )
                    }
                }

                root.optJSONArray("menus")?.let { array ->
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        dao.insertMenu(
                            MenuEntity(
                                date = item.optString("date"),
                                breakfast = item.optString("breakfast"),
                                lunch = item.optString("lunch"),
                                dinner = item.optString("dinner")
                            )
                        )
                    }
                }

                root.optJSONArray("finance")?.let { array ->
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        dao.insertFinance(
                            FinanceEntity(
                                id = item.optLong("id", 0),
                                date = item.optString("date"),
                                type = item.optString("type", "expense"),
                                category = item.optString("category", "Thực phẩm"),
                                amount = item.optLong("amount"),
                                note = item.optString("note")
                            )
                        )
                    }
                }

                root.optJSONArray("todos")?.let { array ->
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        dao.insertTodo(
                            TodoEntity(
                                id = item.optLong("id", 0),
                                date = item.optString("date", selectedDate.value),
                                title = item.optString("title"),
                                completed = item.optBoolean("completed"),
                                source = item.optString("source", "manual")
                            )
                        )
                    }
                }

                root.optJSONObject("settings")?.let { settings ->
                    saveMealPrices(
                        settings.optLong("morningPrice", 20_000),
                        settings.optLong("noonPrice", 35_000),
                        settings.optLong("eveningPrice", 35_000)
                    )
                }
            }
        }.onSuccess { _status.value = "Đã khôi phục dữ liệu" }
            .onFailure { _status.value = "Lỗi khôi phục: ${it.message}" }
    }

    fun exportXlsx(uri: Uri) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val financeRows = mutableListOf<List<Any?>>()
                financeRows += listOf("Ngày", "Loại", "Nhóm", "Nội dung", "Số tiền")
                dao.allFinance().forEach {
                    financeRows += listOf(it.date, it.type, it.category, it.note, it.amount)
                }

                val menuRows = mutableListOf<List<Any?>>()
                menuRows += listOf("Ngày", "Bữa sáng", "Bữa trưa", "Bữa tối")
                dao.allMenus().forEach {
                    menuRows += listOf(it.date, it.breakfast, it.lunch, it.dinner)
                }

                val peopleById = dao.allPeople().associateBy { it.id }
                val attendanceRows = mutableListOf<List<Any?>>()
                attendanceRows += listOf("Ngày", "Họ tên", "Phân loại", "Sáng", "Trưa", "Tối")
                dao.allAttendance().forEach {
                    val person = peopleById[it.personId]
                    attendanceRows += listOf(
                        it.date,
                        person?.name.orEmpty(),
                        person?.category.orEmpty(),
                        if (it.morning) 1 else 0,
                        if (it.noon) 1 else 0,
                        if (it.evening) 1 else 0
                    )
                }

                ZipOutputStream(app.contentResolver.openOutputStream(uri, "w")!!).use { zip ->
                    putZip(zip, "[Content_Types].xml", contentTypesXml())
                    putZip(zip, "_rels/.rels", rootRelsXml())
                    putZip(zip, "xl/workbook.xml", workbookXml())
                    putZip(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
                    putZip(zip, "xl/worksheets/sheet1.xml", sheetXml(financeRows))
                    putZip(zip, "xl/worksheets/sheet2.xml", sheetXml(menuRows))
                    putZip(zip, "xl/worksheets/sheet3.xml", sheetXml(attendanceRows))
                }
            }
        }.onSuccess { _status.value = "Đã xuất Excel .xlsx" }
            .onFailure { _status.value = "Lỗi xuất Excel: ${it.message}" }
    }

    private fun putZip(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        </Types>""".trimIndent()

    private fun rootRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>""".trimIndent()

    private fun workbookXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Giao dịch" sheetId="1" r:id="rId1"/>
            <sheet name="Thực đơn" sheetId="2" r:id="rId2"/>
            <sheet name="Chấm cơm" sheetId="3" r:id="rId3"/>
          </sheets>
        </workbook>""".trimIndent()

    private fun workbookRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
          <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
        </Relationships>""".trimIndent()

    private fun sheetXml(rows: List<List<Any?>>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
        rows.forEachIndexed { rowIndex, row ->
            append("<row r=\"${rowIndex + 1}\">")
            row.forEachIndexed { columnIndex, value ->
                val ref = "${columnName(columnIndex + 1)}${rowIndex + 1}"
                when (value) {
                    is Number -> append("<c r=\"$ref\"><v>${value}</v></c>")
                    else -> append("<c r=\"$ref\" t=\"inlineStr\"><is><t>${xml(value?.toString().orEmpty())}</t></is></c>")
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun columnName(index: Int): String {
        var number = index
        val result = StringBuilder()
        while (number > 0) {
            number--
            result.append(('A'.code + number % 26).toChar())
            number /= 26
        }
        return result.reverse().toString()
    }

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun displayDate(value: String): String =
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}
