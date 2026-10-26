package com.sakurasao.teatime

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// --- PALET WARNA COZY, SOFT & CUTE ---
private val BgCanvas = Color(0xFFFAF7F2)
private val CardWhite = Color(0xFFFFFFFF)
private val PastelGreen = Color(0xFF88AB8E)
private val SoftMatcha = Color(0xFFEEF5EE)
private val SoftPeach = Color(0xFFF9EAE1)
private val TextDark = Color(0xFF232724)
private val TextMuted = Color(0xFF858E88)
private val LineBorder = Color(0xFFEBE5DC)

// ========================================================================================
// ADAPTIVE UI: KELAS UKURAN LAYAR
// Compact < 600 dp, Medium 600-839 dp, Expanded >= 840 dp
// ========================================================================================
enum class WindowSize(val label: String) {
    Compact("COMPACT · PONSEL"),
    Medium("MEDIUM · LAYAR SEDANG"),
    Expanded("EXPANDED · TABLET/LAPTOP")
}

fun windowSizeFor(width: Dp): WindowSize = when {
    width < 600.dp -> WindowSize.Compact
    width < 840.dp -> WindowSize.Medium
    else -> WindowSize.Expanded
}

private val tabItems = listOf(
    "TeaTime" to "🍵",
    "Seduh" to "🍃",
    "Jadwal" to "📅",
    "Catatan" to "✏️"
)

data class AlarmItem(
    val id: String,
    val time: String,
    val note: String,
    val isActive: Boolean
)

data class DrinkChoice(
    val name: String,
    val icon: String,
    val temp: String,
    val seconds: Int,
    val tip: String
)

data class DiaryNote(
    val mood: String,
    val text: String,
    val time: String
)

val drinkList = listOf(
    DrinkChoice("Teh", "☕", "85°C", 120, "Seduhan teh manis hangat klasik yang ramah di lambung dan menenangkan."),
    DrinkChoice("Teh Hijau", "🍵", "80°C", 150, "Kaya antioksidan alami, bantu menjernihkan pikiran yang lelah."),
    DrinkChoice("Teh Melati", "🌼", "85°C", 180, "Aroma bunga melati asli yang harum, ampuh melepas stres kerja."),
    DrinkChoice("Teh Hitam", "🫖", "90°C", 210, "Rasa pekat berkarakter, pas untuk mengembalikan fokus saat ngantuk."),
    DrinkChoice("Es Teh", "🧊", "Dingin", 60, "Segar dan melegakan tenggorokan saat cuaca gerah di siang hari.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BgCanvas) {
                    TeaTimeApp()
                }
            }
        }
    }
}

@Composable
fun TeaTimeApp(initialTab: String = "TeaTime") {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(initialTab) }

    var cupsToday by remember { mutableIntStateOf(2) }
    var mindfulMins by remember { mutableIntStateOf(10) }
    var showAlarmPopup by remember { mutableStateOf(false) }

    // --- STATE TIMER DIANGKAT KE SINI AGAR TIDAK HILANG SAAT PINDAH TAB ---
    var selectedDrink by remember { mutableStateOf(drinkList[0]) }
    var timeLeft by remember { mutableIntStateOf(drinkList[0].seconds) }
    var maxTime by remember { mutableIntStateOf(drinkList[0].seconds) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var dunkCount by remember { mutableIntStateOf(0) }

    // COROUTINE TIMER GLOBAL (JALAN TERUS MESKI BUKA TAB LAIN)
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (isTimerRunning && timeLeft > 0) {
                delay(1000L.milliseconds)
                timeLeft--
            }
            if (isTimerRunning && timeLeft == 0) {
                isTimerRunning = false
                cupsToday += 1
                mindfulMins += (maxTime / 60)
                timeLeft = maxTime
                Toast.makeText(context, "Selesai seduh ${selectedDrink.name}! Pikiran lebih segar ✨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var alarms by remember {
        mutableStateOf(
            listOf(
                AlarmItem("1", "10:30", "Rehat mata & minum", true),
                AlarmItem("2", "14:00", "Jeda siang sejenak", true),
                AlarmItem("3", "16:30", "Sore santai", true)
            )
        )
    }

    var notes by remember {
        mutableStateOf(
            listOf(
                DiaryNote("🌿 Tenang", "Pundak terasa lebih enteng setelah rehat sejenak.", "10:35")
            )
        )
    }

    if (showAlarmPopup) {
        AlertDialog(
            onDismissRequest = { showAlarmPopup = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⏰", fontSize = 20.sp)
                    Text("Waktunya Rehat", fontWeight = FontWeight.Bold, color = TextDark)
                }
            },
            text = {
                Text(
                    "Matamu sudah cukup lama menatap layar. Yuk berdiri, seduh minuman hangat sebentar.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAlarmPopup = false
                        currentTab = "Seduh"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PastelGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mulai Rehat", color = CardWhite, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmPopup = false }) {
                    Text("Nanti Dulu", color = TextMuted)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // --- ADAPTIVE UI: LAYOUT BERUBAH SESUAI LEBAR RUANG YANG TERSEDIA ---
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowSize = windowSizeFor(maxWidth)
        val showSideRail = windowSize == WindowSize.Expanded

        Scaffold(
            containerColor = BgCanvas,
            bottomBar = {
                // Compact & Medium: navigasi di bawah. Expanded: pindah ke side rail.
                if (!showSideRail) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardWhite,
                        border = BorderStroke(1.dp, LineBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .height(58.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabItems.forEach { (title, icon) ->
                                CuteTabItem(title, icon, currentTab == title) { currentTab = title }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                if (showSideRail) {
                    SideRail(currentTab = currentTab, onSelect = { currentTab = it })
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (currentTab) {
                        "TeaTime" -> HomeScreen(
                            windowSize = windowSize,
                            cupsToday = cupsToday,
                            mindfulMins = mindfulMins,
                            nextAlarm = alarms.firstOrNull { it.isActive }?.time ?: "--:--",
                            onTriggerTest = { showAlarmPopup = true },
                            onStartBreak = { currentTab = "Seduh" }
                        )
                        "Seduh" -> BrewScreen(
                            windowSize = windowSize,
                            selectedDrink = selectedDrink,
                            options = drinkList,
                            timeLeft = timeLeft,
                            maxTime = maxTime,
                            isRunning = isTimerRunning,
                            dunkCount = dunkCount,
                            onSelectDrink = { drink ->
                                selectedDrink = drink
                                isTimerRunning = false
                                timeLeft = drink.seconds
                                maxTime = drink.seconds
                                dunkCount = 0
                            },
                            onToggleRunning = { isTimerRunning = !isTimerRunning },
                            onAdjustTime = { delta ->
                                if (!isTimerRunning) {
                                    val newTime = timeLeft + delta
                                    if (newTime >= 30) {
                                        timeLeft = newTime
                                        maxTime = newTime
                                    }
                                }
                            },
                            onReset = {
                                isTimerRunning = false
                                timeLeft = maxTime
                                dunkCount = 0
                            },
                            onDunk = { dunkCount++ }
                        )
                        "Jadwal" -> ScheduleScreen(
                            windowSize = windowSize,
                            alarms = alarms,
                            onToggle = { id ->
                                alarms = alarms.map { if (it.id == id) it.copy(isActive = !it.isActive) else it }
                            },
                            onAdd = { time, label ->
                                val newId = (alarms.size + 1).toString()
                                alarms = alarms + AlarmItem(newId, time, label, true)
                                Toast.makeText(context, "Alarm baru ditambahkan!", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { id ->
                                alarms = alarms.filter { it.id != id }
                            },
                            onTestAlarm = { showAlarmPopup = true }
                        )
                        "Catatan" -> DiaryScreen(
                            windowSize = windowSize,
                            notes = notes,
                            onSaveNote = { newNote ->
                                notes = listOf(newNote) + notes
                                Toast.makeText(context, "Catatan tersimpan 🌿", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ========================================================================================
// ADAPTIVE UI: SIDE RAIL (MUNCUL HANYA SAAT LAYAR EXPANDED) & BANNER MODE LAYOUT
// ========================================================================================
@Composable
fun SideRail(currentTab: String, onSelect: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .width(96.dp)
            .fillMaxHeight(),
        color = CardWhite,
        border = BorderStroke(1.dp, LineBorder)
    ) {
        Column(
            modifier = Modifier.padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            tabItems.forEach { (title, icon) ->
                CuteTabItem(title, icon, currentTab == title) { onSelect(title) }
            }
        }
    }
}

@Composable
fun WindowClassBanner(windowSize: WindowSize) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SoftMatcha,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LineBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📐", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Mode layout: ${windowSize.label}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
        }
    }
}

// ========================================================================================
// 1. TEATIME (BERANDA)
// ========================================================================================
@Composable
fun HomeScreen(
    windowSize: WindowSize,
    cupsToday: Int,
    mindfulMins: Int,
    nextAlarm: String,
    onTriggerTest: () -> Unit,
    onStartBreak: () -> Unit
) {
    val isWide = windowSize != WindowSize.Compact

    val nextAlarmCard: @Composable () -> Unit = {
        CozyCard(backgroundColor = CardWhite) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("JADWAL BREAK BERIKUTNYA", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(nextAlarm, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Button(
                        onClick = onTriggerTest,
                        colors = ButtonDefaults.buttonColors(containerColor = SoftMatcha),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🔔 Tes Alarm", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    val statsRow: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox("Cangkir Hari Ini", "$cupsToday", "sesi", Modifier.weight(1f))
            StatBox("Waktu Rehat", "$mindfulMins", "menit", Modifier.weight(1f))
        }
    }

    val tipCard: @Composable (Modifier) -> Unit = { cardModifier ->
        CozyCard(modifier = cardModifier, backgroundColor = SoftMatcha) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mata Terasa Lelah? ✨", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(
                        "Meluangkan jeda 2 menit membantu menjaga fokus tetap jernih dan badan tidak tegang.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        lineHeight = 20.sp
                    )
                }

                Button(
                    onClick = onStartBreak,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PastelGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Mulai Istirahat Sekarang", fontWeight = FontWeight.SemiBold, color = CardWhite)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("TeaTime", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text("Waktunya bernapas sejenak 🌿", fontSize = 14.sp, color = TextMuted)
            }
            Surface(
                color = SoftPeach,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🍵", fontSize = 20.sp)
                }
            }
        }

        WindowClassBanner(windowSize)

        if (isWide) {
            // Medium & Expanded: dua kolom berdampingan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    nextAlarmCard()
                    statsRow()
                }
                tipCard(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            // Compact: satu kolom ke bawah
            nextAlarmCard()
            statsRow()
            tipCard(Modifier.weight(1f))
        }
    }
}

// ========================================================================================
// 2. SEDUH (TIMER KONTINU, TIDAK RESET SAAT PINDAH TAB)
// ========================================================================================
@Composable
fun BrewScreen(
    windowSize: WindowSize,
    selectedDrink: DrinkChoice,
    options: List<DrinkChoice>,
    timeLeft: Int,
    maxTime: Int,
    isRunning: Boolean,
    dunkCount: Int,
    onSelectDrink: (DrinkChoice) -> Unit,
    onToggleRunning: () -> Unit,
    onAdjustTime: (Int) -> Unit,
    onReset: () -> Unit,
    onDunk: () -> Unit
) {
    val isWide = windowSize != WindowSize.Compact

    val header: @Composable () -> Unit = {
        Column {
            Text("Seduh & Tenang", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text("Atur waktu seduh dan nikmati jedamu.", fontSize = 13.sp, color = TextMuted)
        }
    }

    // 5 Kotak Pilihan Minuman Simetris
    val picker: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { drink ->
                val isSelected = drink.name == selectedDrink.name
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clickable { onSelectDrink(drink) },
                    color = if (isSelected) PastelGreen else CardWhite,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isSelected) PastelGreen else LineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(drink.icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = drink.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 12.sp,
                            color = if (isSelected) CardWhite else TextDark
                        )
                    }
                }
            }
        }
    }

    // Kartu Utama Seduh
    val mainCard: @Composable (Modifier) -> Unit = { cardModifier ->
        CozyCard(
            modifier = cardModifier,
            backgroundColor = CardWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedDrink.name.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Surface(
                        color = SoftMatcha,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "SUHU: ${selectedDrink.temp}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }

                // Cangkir Interaktif
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDunk() }
                ) {
                    Text(
                        if (isRunning) "♨️ 🍵 🫧" else "🍵",
                        fontSize = 44.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (dunkCount > 0) "[ Dicelup $dunkCount kali ]" else "[ Ketuk cangkir untuk celup ]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }

                // Timer Besar
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%02d:%02d".format(timeLeft / 60, timeLeft % 60),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                        color = TextDark
                    )
                    Text(
                        text = if (isRunning) "Sedang menyeduh... tarik napas perlahan" else "Siap untuk menyeduh",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                // Tombol +/- Waktu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAdjustTime(-30) },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("-30s", fontSize = 11.sp, color = TextDark)
                    }

                    Text(
                        "Target: ${maxTime / 60}m ${maxTime % 60}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )

                    OutlinedButton(
                        onClick = { onAdjustTime(30) },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("+30s", fontSize = 11.sp, color = TextDark)
                    }
                }

                // Progress Bar
                val progress = if (maxTime > 0) (1f - (timeLeft.toFloat() / maxTime.toFloat())).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(6.dp),
                    color = PastelGreen,
                    trackColor = SoftMatcha
                )

                // Tombol Aksi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onToggleRunning,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) SoftPeach else PastelGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isRunning) "Jeda" else "Mulai Seduh",
                            color = if (isRunning) TextDark else CardWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LineBorder)
                    ) {
                        Text("Reset", color = TextDark, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Catatan Manfaat
    val tipCard: @Composable () -> Unit = {
        CozyCard(backgroundColor = SoftMatcha) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Catatan Seduh & Manfaat:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(
                    selectedDrink.tip,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }

    if (isWide) {
        // Medium & Expanded: pilihan minuman di kiri, kartu timer di kanan
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                header()
                picker()
                tipCard()
            }
            mainCard(
                Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            )
        }
    } else {
        // Compact: semua ditumpuk ke bawah
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            header()
            picker()
            mainCard(Modifier.weight(1f))
            tipCard()
        }
    }
}

// ========================================================================================
// 3. JADWAL
// ========================================================================================
@Composable
fun ScheduleScreen(
    windowSize: WindowSize,
    alarms: List<AlarmItem>,
    onToggle: (String) -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onTestAlarm: () -> Unit
) {
    val isWide = windowSize != WindowSize.Compact
    var newTime by remember { mutableStateOf("15:00") }
    var newLabel by remember { mutableStateOf("Rehat Sore") }

    val header: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Jadwal Rehat", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text("Pengingat agar tidak lupa istirahat.", fontSize = 13.sp, color = TextMuted)
            }
            TextButton(onClick = onTestAlarm) {
                Text("🔔 Uji", color = PastelGreen, fontWeight = FontWeight.Bold)
            }
        }
    }

    val alarmList: @Composable (Modifier) -> Unit = { listModifier ->
        LazyColumn(
            modifier = listModifier,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(alarms) { item ->
                CozyCard(backgroundColor = CardWhite) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                item.time,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isActive) TextDark else TextMuted.copy(alpha = 0.5f)
                            )
                            Text(item.note, fontSize = 12.sp, color = TextMuted)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Switch(
                                checked = item.isActive,
                                onCheckedChange = { onToggle(item.id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CardWhite,
                                    checkedTrackColor = PastelGreen,
                                    uncheckedThumbColor = CardWhite,
                                    uncheckedTrackColor = LineBorder
                                )
                            )
                            Text(
                                "✕",
                                fontSize = 14.sp,
                                color = TextMuted,
                                modifier = Modifier
                                    .clickable { onDelete(item.id) }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    val addForm: @Composable () -> Unit = {
        CozyCard(backgroundColor = CardWhite) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tambah Pengingat Baru ⏰", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTime,
                        onValueChange = { newTime = it },
                        modifier = Modifier.weight(0.9f),
                        label = { Text("Jam (14:30)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        modifier = Modifier.weight(1.1f),
                        label = { Text("Label", fontSize = 11.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Button(
                    onClick = {
                        if (newTime.isNotBlank() && newLabel.isNotBlank()) {
                            onAdd(newTime, newLabel)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PastelGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ Pasang Pengingat", fontSize = 13.sp, color = CardWhite)
                }
            }
        }
    }

    if (isWide) {
        // Medium & Expanded: daftar alarm di kiri, form tambah di kanan
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                header()
                alarmList(Modifier.weight(1f))
            }
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
            ) {
                addForm()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            header()
            alarmList(Modifier.weight(1f))
            addForm()
        }
    }
}

// ========================================================================================
// 4. CATATAN
// ========================================================================================
@Composable
fun DiaryScreen(
    windowSize: WindowSize,
    notes: List<DiaryNote>,
    onSaveNote: (DiaryNote) -> Unit
) {
    val isWide = windowSize != WindowSize.Compact
    val moods = listOf("🌿 Tenang", "✨ Segar", "☁️ Rileks")
    var selectedMood by remember { mutableStateOf(moods[0]) }
    var inputText by remember { mutableStateOf("") }

    val header: @Composable () -> Unit = {
        Column {
            Text("Catatan Rehat", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text("Ceritakan sedikit rasa tenang setelah jeda.", fontSize = 13.sp, color = TextMuted)
        }
    }

    val formCard: @Composable () -> Unit = {
        CozyCard(backgroundColor = CardWhite) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    moods.forEach { mood ->
                        val isSelected = mood == selectedMood
                        Surface(
                            modifier = Modifier.clickable { selectedMood = mood },
                            color = if (isSelected) SoftMatcha else BgCanvas,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                mood,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = TextDark
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Bagaimana perasaanmu setelah rehat?", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSaveNote(DiaryNote(selectedMood, inputText, "Hari ini"))
                            inputText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PastelGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan Cerita ✨", fontSize = 13.sp, color = CardWhite)
                }
            }
        }
    }

    val noteList: @Composable (Modifier) -> Unit = { listModifier ->
        LazyColumn(modifier = listModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(notes) { note ->
                CozyCard(backgroundColor = CardWhite) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(note.mood, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text(note.time, fontSize = 11.sp, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("“${note.text}”", fontSize = 13.sp, color = TextDark)
                    }
                }
            }
        }
    }

    if (isWide) {
        // Medium & Expanded: form di kiri, daftar catatan di kanan
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                header()
                formCard()
            }
            noteList(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            header()
            formCard()
            noteList(Modifier.weight(1f))
        }
    }
}

// ========================================================================================
// ITEM NAVBAR
// ========================================================================================
@Composable
fun CuteTabItem(
    title: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) TextDark else TextMuted
        )
    }
}

@Composable
fun CozyCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardWhite,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, LineBorder)
    ) {
        Column(content = content)
    }
}

@Composable
fun StatBox(label: String, value: String, unit: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, LineBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(unit, fontSize = 11.sp, color = TextMuted)
        }
    }
}

// ========================================================================================
// PREVIEW: PONSEL (COMPACT), LAYAR SEDANG (MEDIUM), TABLET (EXPANDED)
// ========================================================================================
@Composable
private fun PreviewFrame(tab: String) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = BgCanvas) {
            TeaTimeApp(initialTab = tab)
        }
    }
}

// --- Ponsel · Compact ---
@Preview(name = "Ponsel 1. TeaTime", showBackground = true, widthDp = 393, heightDp = 850)
@Composable
fun PreviewPhoneTeaTime() = PreviewFrame("TeaTime")

@Preview(name = "Ponsel 2. Seduh", showBackground = true, widthDp = 393, heightDp = 850)
@Composable
fun PreviewPhoneSeduh() = PreviewFrame("Seduh")

@Preview(name = "Ponsel 3. Jadwal", showBackground = true, widthDp = 393, heightDp = 850)
@Composable
fun PreviewPhoneJadwal() = PreviewFrame("Jadwal")

@Preview(name = "Ponsel 4. Catatan", showBackground = true, widthDp = 393, heightDp = 850)
@Composable
fun PreviewPhoneCatatan() = PreviewFrame("Catatan")

// --- Layar Sedang · Medium ---
@Preview(name = "Medium TeaTime", showBackground = true, widthDp = 700, heightDp = 900)
@Composable
fun PreviewMediumTeaTime() = PreviewFrame("TeaTime")

// --- Tablet · Expanded (dengan side rail) ---
@Preview(name = "Tablet 1. TeaTime", showBackground = true, widthDp = 1000, heightDp = 800)
@Composable
fun PreviewTabletTeaTime() = PreviewFrame("TeaTime")

@Preview(name = "Tablet 2. Seduh", showBackground = true, widthDp = 1000, heightDp = 800)
@Composable
fun PreviewTabletSeduh() = PreviewFrame("Seduh")

@Preview(name = "Tablet 3. Jadwal", showBackground = true, widthDp = 1000, heightDp = 800)
@Composable
fun PreviewTabletJadwal() = PreviewFrame("Jadwal")

@Preview(name = "Tablet 4. Catatan", showBackground = true, widthDp = 1000, heightDp = 800)
@Composable
fun PreviewTabletCatatan() = PreviewFrame("Catatan")