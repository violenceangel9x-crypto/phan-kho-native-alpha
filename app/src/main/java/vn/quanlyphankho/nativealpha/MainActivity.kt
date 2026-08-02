@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package vn.quanlyphankho.nativealpha

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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

class PhanKhoApplication : Application() {
    val database by lazy { PhanKhoDatabase.create(this) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhanKhoTheme {
                val vm: PhanKhoViewModel = viewModel()
                PhanKhoApp(vm)
            }
        }
    }
}

class PhanKhoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PhanKhoApplication
    private val dao = app.database.dao()
    private val selectedDate = MutableStateFlow(LocalDate.now().toString())
    private val _status = MutableStateFlow("Kotlin + Compose + Room")

    val date: StateFlow<String> = selectedDate
    val status: StateFlow<String> = _status
    val people = dao.observePeople().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val attendance = selectedDate.flatMapLatest(dao::observeAttendance)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val menu = selectedDate.flatMapLatest(dao::observeMenu)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val finance = selectedDate.flatMapLatest(dao::observeFinance)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val todos = selectedDate.flatMapLatest(dao::observeTodos)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun previousDay() { selectedDate.value = LocalDate.parse(selectedDate.value).minusDays(1).toString() }
    fun nextDay() { selectedDate.value = LocalDate.parse(selectedDate.value).plusDays(1).toString() }

    fun addPerson(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) dao.insertPerson(PersonEntity(name = name.trim()))
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

    fun saveMenu(breakfast: String, lunch: String, dinner: String) = viewModelScope.launch {
        dao.insertMenu(MenuEntity(selectedDate.value, breakfast.trim(), lunch.trim(), dinner.trim()))
        _status.value = "Đã lưu thực đơn ${formatDate(selectedDate.value)}"
    }

    fun addFinance(amount: Long, note: String) = viewModelScope.launch {
        if (amount > 0) {
            dao.insertFinance(FinanceEntity(date = selectedDate.value, type = "expense", category = "Thực phẩm", amount = amount, note = note.trim()))
            _status.value = "Đã lưu khoản chi"
        }
    }

    fun addTodo(title: String) = viewModelScope.launch {
        if (title.isNotBlank()) dao.insertTodo(TodoEntity(date = selectedDate.value, title = title.trim()))
    }

    fun toggleTodo(item: TodoEntity) = viewModelScope.launch {
        dao.updateTodo(item.copy(completed = !item.completed))
    }

    fun backup(uri: Uri) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val root = JSONObject()
                root.put("format", "phan-kho-native-alpha")
                root.put("exportedAt", System.currentTimeMillis())
                root.put("people", JSONArray().apply {
                    dao.allPeople().forEach { put(JSONObject().put("name", it.name).put("role", it.role).put("category", it.category)) }
                })
                root.put("todos", JSONArray().apply {
                    dao.allTodos().forEach { put(JSONObject().put("date", it.date).put("title", it.title).put("completed", it.completed)) }
                })
                root.put("menus", JSONArray().apply {
                    dao.allMenus().forEach { put(JSONObject().put("date", it.date).put("breakfast", it.breakfast).put("lunch", it.lunch).put("dinner", it.dinner)) }
                })
                root.put("finance", JSONArray().apply {
                    dao.allFinance().forEach { put(JSONObject().put("date", it.date).put("amount", it.amount).put("note", it.note)) }
                })
                app.contentResolver.openOutputStream(uri, "w")!!.bufferedWriter().use { it.write(root.toString(2)) }
            }
        }.onSuccess { _status.value = "Đã sao lưu JSON" }
            .onFailure { _status.value = "Lỗi sao lưu: ${it.message}" }
    }

    fun restore(uri: Uri) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val raw = app.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                val root = JSONObject(raw)
                root.optJSONArray("people")?.let { array ->
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        dao.insertPerson(PersonEntity(name = item.optString("name"), role = item.optString("role"), category = item.optString("category", "Cán bộ")))
                    }
                }
                root.optJSONArray("todos")?.let { array ->
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        dao.insertTodo(TodoEntity(date = item.optString("date", selectedDate.value), title = item.optString("title"), completed = item.optBoolean("completed")))
                    }
                }
            }
        }.onSuccess { _status.value = "Đã khôi phục dữ liệu Alpha" }
            .onFailure { _status.value = "Lỗi khôi phục: ${it.message}" }
    }

    fun exportExcelCsv(uri: Uri) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val rows = buildString {
                    append("Ngày,Nội dung,Số tiền\n")
                    dao.allFinance().forEach {
                        append(csv(it.date)).append(',').append(csv(it.note.ifBlank { it.category })).append(',').append(it.amount).append('\n')
                    }
                    append("\nNgày,Bữa sáng,Bữa trưa,Bữa tối\n")
                    dao.allMenus().forEach {
                        append(csv(it.date)).append(',').append(csv(it.breakfast)).append(',').append(csv(it.lunch)).append(',').append(csv(it.dinner)).append('\n')
                    }
                }
                app.contentResolver.openOutputStream(uri, "w")!!.use { output ->
                    output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    output.write(rows.toByteArray(Charsets.UTF_8))
                }
            }
        }.onSuccess { _status.value = "Đã xuất tệp mở bằng Excel" }
            .onFailure { _status.value = "Lỗi xuất dữ liệu: ${it.message}" }
    }

    private fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""
}

private enum class Tab(val title: String, val icon: String) {
    Today("Hôm nay", "⌂"), Attendance("Chấm cơm", "✓"), Finance("Sổ sách", "₫"), Menu("Thực đơn", "≡"), More("Khác", "•••")
}

@Composable
fun PhanKhoApp(vm: PhanKhoViewModel) {
    var selectedTab by remember { mutableStateOf(Tab.Today) }
    val status by vm.status.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("PHÂN KHO NATIVE", fontWeight = FontWeight.Black)
                    Text(status, style = MaterialTheme.typography.labelSmall)
                }
            })
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.icon, fontSize = 20.sp) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                Tab.Today -> TodayScreen(vm)
                Tab.Attendance -> AttendanceScreen(vm)
                Tab.Finance -> FinanceScreen(vm)
                Tab.Menu -> MenuScreen(vm)
                Tab.More -> MoreScreen(vm)
            }
        }
    }
}

@Composable
private fun DateHeader(date: String, previous: () -> Unit, next: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(onClick = previous) { Text("‹") }
        Text(formatDate(date), fontWeight = FontWeight.Bold)
        FilledTonalButton(onClick = next) { Text("›") }
    }
}

@Composable
private fun TodayScreen(vm: PhanKhoViewModel) {
    val date by vm.date.collectAsStateWithLifecycle()
    val attendance by vm.attendance.collectAsStateWithLifecycle()
    val finance by vm.finance.collectAsStateWithLifecycle()
    val todos by vm.todos.collectAsStateWithLifecycle()
    val morning = attendance.count { it.morning }
    val noon = attendance.count { it.noon }
    val evening = attendance.count { it.evening }
    val actual = finance.sumOf { it.amount }
    var todoText by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { DateHeader(date, vm::previousDay, vm::nextDay) }
        item { NativeCard("TÀI CHÍNH", "Chi tiêu trong ngày: ${formatDate(date)}") { Text(money(actual), fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) } }
        item { NativeCard("SUẤT ĂN", "Đã chấm hôm nay") { Text("${morning + noon + evening} tổng suất", fontSize = 28.sp, fontWeight = FontWeight.Black); Text("Sáng $morning · Trưa $noon · Tối $evening") } }
        item {
            NativeCard("VIỆC CẦN LÀM", "Mua đến đâu đánh dấu đến đó") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(todoText, { todoText = it }, label = { Text("Thêm việc") }, modifier = Modifier.weight(1f), singleLine = true)
                    Button(onClick = { vm.addTodo(todoText); todoText = "" }, enabled = todoText.isNotBlank()) { Text("+") }
                }
                todos.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(item.completed, { vm.toggleTodo(item) })
                        Text(item.title)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceScreen(vm: PhanKhoViewModel) {
    val date by vm.date.collectAsStateWithLifecycle()
    val people by vm.people.collectAsStateWithLifecycle()
    val attendance by vm.attendance.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DateHeader(date, vm::previousDay, vm::nextDay)
        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Thêm người ăn") }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(people, key = { it.id }) { person ->
                val row = attendance.firstOrNull { it.personId == person.id } ?: AttendanceEntity(date, person.id)
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(person.name, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MealCheck("Sáng", row.morning) { vm.setMeal(person.id, "morning", it) }
                            MealCheck("Trưa", row.noon) { vm.setMeal(person.id, "noon", it) }
                            MealCheck("Tối", row.evening) { vm.setMeal(person.id, "evening", it) }
                        }
                    }
                }
            }
        }
    }
    if (showDialog) AddPersonDialog(onDismiss = { showDialog = false }) { vm.addPerson(it); showDialog = false }
}

@Composable
private fun MealCheck(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, onChecked); Text(label) }
}

@Composable
private fun AddPersonDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm người ăn") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Họ tên") }) },
        confirmButton = { Button(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun FinanceScreen(vm: PhanKhoViewModel) {
    val date by vm.date.collectAsStateWithLifecycle()
    val finance by vm.finance.collectAsStateWithLifecycle()
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DateHeader(date, vm::previousDay, vm::nextDay)
        OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Số tiền") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("Nội dung") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.addFinance(amount.toLongOrNull() ?: 0, note); amount = ""; note = "" }, modifier = Modifier.fillMaxWidth()) { Text("Lưu khoản chi") }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(finance, key = { it.id }) { item ->
                Card { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(item.note.ifBlank { item.category }); Text(money(item.amount), fontWeight = FontWeight.Bold) } }
            }
        }
    }
}

@Composable
private fun MenuScreen(vm: PhanKhoViewModel) {
    val date by vm.date.collectAsStateWithLifecycle()
    val saved by vm.menu.collectAsStateWithLifecycle()
    var breakfast by remember(date, saved) { mutableStateOf(saved?.breakfast.orEmpty()) }
    var lunch by remember(date, saved) { mutableStateOf(saved?.lunch.orEmpty()) }
    var dinner by remember(date, saved) { mutableStateOf(saved?.dinner.orEmpty()) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DateHeader(date, vm::previousDay, vm::nextDay)
        Text("LẬP THỰC ĐƠN THEO NGÀY", fontSize = 24.sp, fontWeight = FontWeight.Black)
        OutlinedTextField(breakfast, { breakfast = it }, label = { Text("Bữa sáng") }, modifier = Modifier.fillMaxWidth().weight(1f))
        OutlinedTextField(lunch, { lunch = it }, label = { Text("Bữa trưa") }, modifier = Modifier.fillMaxWidth().weight(1f))
        OutlinedTextField(dinner, { dinner = it }, label = { Text("Bữa tối") }, modifier = Modifier.fillMaxWidth().weight(1f))
        Button(onClick = { vm.saveMenu(breakfast, lunch, dinner) }, modifier = Modifier.fillMaxWidth()) { Text("Lưu thực đơn") }
    }
}

@Composable
private fun MoreScreen(vm: PhanKhoViewModel) {
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::backup) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::restore) }
    val excel = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(vm::exportExcelCsv) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DỮ LIỆU & TỆP", fontSize = 25.sp, fontWeight = FontWeight.Black)
        Button(onClick = { backup.launch("phan-kho-native-backup.json") }, modifier = Modifier.fillMaxWidth()) { Text("Sao lưu JSON bằng SAF") }
        OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Khôi phục dữ liệu") }
        OutlinedButton(onClick = { excel.launch("phan-kho-native-excel.csv") }, modifier = Modifier.fillMaxWidth()) { Text("Xuất tệp mở bằng Excel") }
        NativeCard("NATIVE ALPHA", "Cài song song với bản V58") { Text("Room lưu dữ liệu nội bộ; SAF cho phép chọn nơi lưu mà không cần quyền truy cập toàn bộ bộ nhớ.") }
    }
}

@Composable
private fun NativeCard(kicker: String, title: String, content: @Composable Column.() -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(kicker, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
private fun PhanKhoTheme(content: @Composable () -> Unit) {
    val colors = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF0878DF),
        secondary = Color(0xFF37B9EF),
        background = Color(0xFFF5F7FA),
        surface = Color.White
    )
    MaterialTheme(colorScheme = colors, content = content)
}

private fun formatDate(date: String): String = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
private fun money(value: Long): String = String.format("%,d ₫", value)
