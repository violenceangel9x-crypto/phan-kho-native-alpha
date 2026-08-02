@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package vn.quanlyphankho.nativealpha

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class AppTab(val title: String, val icon: String) {
    Today("Hôm nay", "⌂"),
    Attendance("Chấm", "✓"),
    Finance("Sổ", "₫"),
    Menu("Thực đơn", "≡"),
    More("Khác", "•••")
}

@Composable
fun PhanKhoApp(vm: PhanKhoViewModel) {
    var selectedTab by remember { mutableStateOf(AppTab.Today) }
    val status by vm.status.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(
                                text = "PHÂN KHO",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                lineHeight = 22.sp
                            )
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.height(70.dp)) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.icon, fontSize = 19.sp, fontWeight = FontWeight.Bold) },
                        label = { Text(tab.title, fontSize = 10.sp, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            AppTab.Today -> TodayScreen(vm, Modifier.padding(padding))
            AppTab.Attendance -> AttendanceScreen(vm, Modifier.padding(padding))
            AppTab.Finance -> FinanceScreen(vm, Modifier.padding(padding))
            AppTab.Menu -> MenuScreen(vm, Modifier.padding(padding))
            AppTab.More -> MoreScreen(vm, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ScreenList(
    modifier: Modifier,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun DateHeader(
    date: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = onPrevious,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(46.dp)
        ) { Text("‹", fontSize = 24.sp) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(displayDate(date), fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(dayName(LocalDate.parse(date)), style = MaterialTheme.typography.labelMedium)
        }

        FilledTonalButton(
            onClick = onNext,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(46.dp)
        ) { Text("›", fontSize = 24.sp) }
    }
}

@Composable
private fun WeekSelector(selectedDate: String, onSelect: (String) -> Unit) {
    val selected = LocalDate.parse(selectedDate)
    val start = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
    val dates = (0L..6L).map(start::plusDays)

    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(dates) { date ->
            FilterChip(
                selected = date == selected,
                onClick = { onSelect(date.toString()) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(shortDayName(date), fontSize = 10.sp)
                        Text(date.dayOfMonth.toString(), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionCard(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                kicker,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Text(title, fontSize = 21.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
private fun MoneyCell(label: String, value: Long, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (emphasize) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                money(value),
                fontWeight = FontWeight.Black,
                fontSize = if (emphasize) 20.sp else 16.sp,
                color = if (emphasize) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TodayScreen(vm: PhanKhoViewModel, modifier: Modifier) {
    val date by vm.date.collectAsStateWithLifecycle()
    val attendance by vm.attendance.collectAsStateWithLifecycle()
    val finance by vm.finance.collectAsStateWithLifecycle()
    val todos by vm.todos.collectAsStateWithLifecycle()
    val prices by vm.mealPrices.collectAsStateWithLifecycle()
    var todoText by remember { mutableStateOf("") }

    val morning = attendance.count { it.morning }
    val noon = attendance.count { it.noon }
    val evening = attendance.count { it.evening }
    val totalMeals = morning + noon + evening
    val planned = morning * prices.morning + noon * prices.noon + evening * prices.evening
    val actual = finance.sumOf { it.amount }
    val remaining = planned - actual

    ScreenList(modifier) {
        item { DateHeader(date, vm::previousDay, vm::nextDay) }
        item {
            SectionCard("TÀI CHÍNH", "Chi tiêu trong ngày: ${displayDate(date)}") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyCell("Dự chi", planned, Modifier.weight(1f))
                    MoneyCell("Thực chi", actual, Modifier.weight(1f))
                }
                MoneyCell("Còn lại", remaining, Modifier.fillMaxWidth(), emphasize = true)
            }
        }
        item {
            SectionCard("SUẤT ĂN", "Đã chấm $totalMeals suất") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealSummaryCell("Sáng", morning, morning * prices.morning, Modifier.weight(1f))
                    MealSummaryCell("Trưa", noon, noon * prices.noon, Modifier.weight(1f))
                    MealSummaryCell("Tối", evening, evening * prices.evening, Modifier.weight(1f))
                }
            }
        }
        item {
            SectionCard("VIỆC CẦN LÀM", "Mua đến đâu đánh dấu đến đó") {
                OutlinedTextField(
                    value = todoText,
                    onValueChange = { todoText = it },
                    label = { Text("Thêm việc") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = { vm.addTodo(todoText); todoText = "" },
                    enabled = todoText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Thêm vào danh sách") }

                if (todos.isEmpty()) {
                    Text("Chưa có việc cần làm.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    todos.forEach { todo ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(todo.completed, { vm.toggleTodo(todo) })
                            Text(todo.title, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.deleteTodo(todo) }) { Text("Xóa") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealSummaryCell(label: String, count: Int, amount: Long, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(money(amount), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun AttendanceScreen(vm: PhanKhoViewModel, modifier: Modifier) {
    val date by vm.date.collectAsStateWithLifecycle()
    val people by vm.people.collectAsStateWithLifecycle()
    val attendance by vm.attendance.collectAsStateWithLifecycle()
    val prices by vm.mealPrices.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPerson by remember { mutableStateOf<PersonEntity?>(null) }

    val morning = attendance.count { it.morning }
    val noon = attendance.count { it.noon }
    val evening = attendance.count { it.evening }
    val planned = morning * prices.morning + noon * prices.noon + evening * prices.evening
    val morningAll = people.isNotEmpty() && people.all { person -> attendance.firstOrNull { it.personId == person.id }?.morning == true }
    val noonAll = people.isNotEmpty() && people.all { person -> attendance.firstOrNull { it.personId == person.id }?.noon == true }
    val eveningAll = people.isNotEmpty() && people.all { person -> attendance.firstOrNull { it.personId == person.id }?.evening == true }

    ScreenList(modifier) {
        item { DateHeader(date, vm::previousDay, vm::nextDay) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showAddDialog = true }, modifier = Modifier.weight(1f)) {
                    Text("+ Thành viên")
                }
                OutlinedButton(onClick = vm::clearAttendanceForDay, modifier = Modifier.weight(1f)) {
                    Text("Bỏ chấm")
                }
            }
        }
        item {
            SectionCard("TỔNG HỢP", "${morning + noon + evening} suất · ${money(planned)}") {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(
                        selected = morningAll,
                        onClick = { vm.setAllMeal("morning", !morningAll) },
                        label = { Text("Sáng $morning") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = noonAll,
                        onClick = { vm.setAllMeal("noon", !noonAll) },
                        label = { Text("Trưa $noon") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = eveningAll,
                        onClick = { vm.setAllMeal("evening", !eveningAll) },
                        label = { Text("Tối $evening") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Chạm vào từng nhãn trên để chọn hoặc bỏ chọn toàn bộ bữa.", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (people.isEmpty()) {
            item {
                SectionCard("CHẤM CƠM", "Chưa có thành viên") {
                    Text("Bấm “+ Thành viên” để tạo danh sách người ăn trong Room.")
                }
            }
        }

        items(people, key = { it.id }) { person ->
            val row = attendance.firstOrNull { it.personId == person.id }
                ?: AttendanceEntity(date, person.id)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(19.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(person.name.take(1).uppercase(), fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(person.name, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text(
                                listOf(person.role, person.category).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { editingPerson = person }) { Text("Sửa") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        MealChip("Sáng", row.morning, Modifier.weight(1f)) { vm.setMeal(person.id, "morning", !row.morning) }
                        MealChip("Trưa", row.noon, Modifier.weight(1f)) { vm.setMeal(person.id, "noon", !row.noon) }
                        MealChip("Tối", row.evening, Modifier.weight(1f)) { vm.setMeal(person.id, "evening", !row.evening) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        PersonDialog(
            person = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, role, category ->
                vm.addPerson(name, role, category)
                showAddDialog = false
            },
            onHide = null
        )
    }

    editingPerson?.let { person ->
        PersonDialog(
            person = person,
            onDismiss = { editingPerson = null },
            onSave = { name, role, category ->
                vm.updatePerson(person, name, role, category)
                editingPerson = null
            },
            onHide = {
                vm.deactivatePerson(person)
                editingPerson = null
            }
        )
    }
}

@Composable
private fun MealChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(if (selected) "✓ $label" else label) },
        modifier = modifier
    )
}

@Composable
private fun PersonDialog(
    person: PersonEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onHide: (() -> Unit)?
) {
    var name by remember(person?.id) { mutableStateOf(person?.name.orEmpty()) }
    var role by remember(person?.id) { mutableStateOf(person?.role.orEmpty()) }
    var category by remember(person?.id) { mutableStateOf(person?.category ?: "Cán bộ") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (person == null) "Thêm thành viên" else "Sửa thành viên") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Họ tên") }, singleLine = true)
                OutlinedTextField(role, { role = it }, label = { Text("Chức vụ") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == "Cán bộ",
                        onClick = { category = "Cán bộ" },
                        label = { Text("Cán bộ") }
                    )
                    FilterChip(
                        selected = category == "Chiến sĩ",
                        onClick = { category = "Chiến sĩ" },
                        label = { Text("Chiến sĩ") }
                    )
                }
                if (onHide != null) {
                    TextButton(onClick = onHide) {
                        Text("Ẩn khỏi danh sách", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, role, category) }, enabled = name.isNotBlank()) {
                Text("Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun FinanceScreen(vm: PhanKhoViewModel, modifier: Modifier) {
    val date by vm.date.collectAsStateWithLifecycle()
    val attendance by vm.attendance.collectAsStateWithLifecycle()
    val finance by vm.finance.collectAsStateWithLifecycle()
    val prices by vm.mealPrices.collectAsStateWithLifecycle()
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Thực phẩm") }

    val morning = attendance.count { it.morning }
    val noon = attendance.count { it.noon }
    val evening = attendance.count { it.evening }
    val planned = morning * prices.morning + noon * prices.noon + evening * prices.evening
    val actual = finance.sumOf { it.amount }

    ScreenList(modifier) {
        item { DateHeader(date, vm::previousDay, vm::nextDay) }
        item {
            SectionCard("TÀI CHÍNH", "Chi tiêu trong ngày: ${displayDate(date)}") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyCell("Dự chi", planned, Modifier.weight(1f))
                    MoneyCell("Thực chi", actual, Modifier.weight(1f))
                }
                MoneyCell("Còn lại", planned - actual, Modifier.fillMaxWidth(), emphasize = true)
            }
        }
        item {
            SectionCard("NHẬP NHANH", "Thêm khoản chi") {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit) },
                    label = { Text("Số tiền") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nội dung") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(listOf("Thực phẩm", "Nhiên liệu", "Khác")) { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item) }
                        )
                    }
                }
                Button(
                    onClick = {
                        vm.addExpense(amountText.toLongOrNull() ?: 0, note, category)
                        amountText = ""
                        note = ""
                    },
                    enabled = (amountText.toLongOrNull() ?: 0) > 0,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Lưu khoản chi") }
            }
        }
        item {
            Text("GIAO DỊCH TRONG NGÀY", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        if (finance.isEmpty()) {
            item { Text("Chưa có giao dịch.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(finance, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(17.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.note.ifBlank { item.category }, fontWeight = FontWeight.Bold)
                        Text(item.category, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(money(item.amount), fontWeight = FontWeight.Black)
                        TextButton(onClick = { vm.deleteExpense(item) }) {
                            Text("Xóa", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuScreen(vm: PhanKhoViewModel, modifier: Modifier) {
    val date by vm.date.collectAsStateWithLifecycle()
    val menu by vm.menu.collectAsStateWithLifecycle()
    var breakfast by remember { mutableStateOf("") }
    var lunch by remember { mutableStateOf("") }
    var dinner by remember { mutableStateOf("") }

    LaunchedEffect(date, menu) {
        breakfast = menu?.breakfast.orEmpty()
        lunch = menu?.lunch.orEmpty()
        dinner = menu?.dinner.orEmpty()
    }

    ScreenList(modifier) {
        item { WeekSelector(date, vm::setDate) }
        item { DateHeader(date, vm::previousDay, vm::nextDay) }
        item {
            Text("LẬP THỰC ĐƠN THEO NGÀY", fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
        item {
            OutlinedTextField(
                breakfast,
                { breakfast = it },
                label = { Text("Bữa sáng") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }
        item {
            OutlinedTextField(
                lunch,
                { lunch = it },
                label = { Text("Bữa trưa") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }
        item {
            OutlinedTextField(
                dinner,
                { dinner = it },
                label = { Text("Bữa tối") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }
        item {
            Button(
                onClick = { vm.saveMenu(breakfast, lunch, dinner) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Lưu thực đơn") }
        }
    }
}

@Composable
private fun MoreScreen(vm: PhanKhoViewModel, modifier: Modifier) {
    val people by vm.people.collectAsStateWithLifecycle()
    val prices by vm.mealPrices.collectAsStateWithLifecycle()
    var morningText by remember { mutableStateOf(prices.morning.toString()) }
    var noonText by remember { mutableStateOf(prices.noon.toString()) }
    var eveningText by remember { mutableStateOf(prices.evening.toString()) }
    var showAddPerson by remember { mutableStateOf(false) }
    var editingPerson by remember { mutableStateOf<PersonEntity?>(null) }

    LaunchedEffect(prices) {
        morningText = prices.morning.toString()
        noonText = prices.noon.toString()
        eveningText = prices.evening.toString()
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(vm::backup) }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(vm::restore) }

    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri -> uri?.let(vm::exportXlsx) }

    ScreenList(modifier) {
        item {
            SectionCard("CẤU HÌNH", "Đơn giá bữa ăn") {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PriceField("Sáng", morningText, { morningText = it.filter(Char::isDigit) }, Modifier.weight(1f))
                    PriceField("Trưa", noonText, { noonText = it.filter(Char::isDigit) }, Modifier.weight(1f))
                    PriceField("Tối", eveningText, { eveningText = it.filter(Char::isDigit) }, Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        vm.saveMealPrices(
                            morningText.toLongOrNull() ?: 0,
                            noonText.toLongOrNull() ?: 0,
                            eveningText.toLongOrNull() ?: 0
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Lưu đơn giá") }
            }
        }
        item {
            SectionCard("DỮ LIỆU", "Sao lưu, khôi phục và Excel") {
                Button(
                    onClick = { backupLauncher.launch("phan-kho-native-backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sao lưu JSON") }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Khôi phục JSON") }
                OutlinedButton(
                    onClick = { xlsxLauncher.launch("phan-kho-native.xlsx") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Xuất Excel .xlsx") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("QUẢN LÝ THÀNH VIÊN", fontWeight = FontWeight.Black)
                    Text("${people.size} người đang hoạt động", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { showAddPerson = true }) { Text("+ Thêm") }
            }
        }
        items(people, key = { it.id }) { person ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(17.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(person.name, fontWeight = FontWeight.Bold)
                        Text(
                            listOf(person.role, person.category).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = { editingPerson = person }) { Text("Sửa") }
                }
            }
        }
        item {
            HorizontalDivider()
            Text(
                "Native Alpha 0.2 · Kotlin + Jetpack Compose + Room + SAF",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showAddPerson) {
        PersonDialog(
            person = null,
            onDismiss = { showAddPerson = false },
            onSave = { name, role, category ->
                vm.addPerson(name, role, category)
                showAddPerson = false
            },
            onHide = null
        )
    }

    editingPerson?.let { person ->
        PersonDialog(
            person = person,
            onDismiss = { editingPerson = null },
            onSave = { name, role, category ->
                vm.updatePerson(person, name, role, category)
                editingPerson = null
            },
            onHide = {
                vm.deactivatePerson(person)
                editingPerson = null
            }
        )
    }
}

@Composable
private fun PriceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        singleLine = true
    )
}

private fun displayDate(date: String): String =
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

private fun dayName(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("vi", "VN"))
        .replaceFirstChar { it.uppercase(Locale("vi", "VN")) }

private fun shortDayName(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "T2"
    2 -> "T3"
    3 -> "T4"
    4 -> "T5"
    5 -> "T6"
    6 -> "T7"
    else -> "CN"
}

private fun money(value: Long): String = String.format(Locale("vi", "VN"), "%,d ₫", value)
