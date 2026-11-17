package ro.frigiderulmeu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ro.frigiderulmeu.data.Item
import ro.frigiderulmeu.domain.CalendarMonth
import ro.frigiderulmeu.domain.Expiry
import ro.frigiderulmeu.domain.ExpiryState
import ro.frigiderulmeu.image.ProductImage
import java.time.LocalDate
import java.time.YearMonth

private const val PAGE_COUNT = 1200      // 100 de ani de navigare
private const val CENTER_PAGE = PAGE_COUNT / 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    vm: FridgeViewModel,
    onOpenItem: (Long) -> Unit,
    onAddForDate: (LocalDate) -> Unit,
    onShowExpired: () -> Unit,
) {
    val today by vm.today.collectAsStateWithLifecycle()
    val byDate by vm.itemsByDate.collectAsStateWithLifecycle()
    val baseMonth = remember { YearMonth.now() }
    val pager = rememberPagerState(initialPage = CENTER_PAGE) { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    var selectedEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val shownMonth = baseMonth.plusMonths((pager.currentPage - CENTER_PAGE).toLong())
    val expiredCount = byDate.filterKeys { it.isBefore(today) }.values.sumOf { it.size }

    Column(Modifier.fillMaxSize()) {
        // Antet: lună, navigare, mod compact/extins
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Luna anterioară")
            }
            Text(
                CalendarMonth.title(shownMonth),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { scope.launch { pager.animateScrollToPage(CENTER_PAGE) } }
            )
            IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Luna următoare")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = vm::toggleCalendarMode) {
                Text(if (vm.calendarExpanded) "Compact" else "Extins")
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (shownMonth != YearMonth.from(today)) {
                AssistChip(onClick = { scope.launch { pager.animateScrollToPage(CENTER_PAGE) } }, label = { Text("Azi") })
                Spacer(Modifier.size(8.dp))
            }
            if (expiredCount > 0) {
                AssistChip(
                    onClick = onShowExpired,
                    label = { Text(if (expiredCount == 1) "! 1 produs expirat" else "! $expiredCount produse expirate") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = StatusColors.background(ExpiryState.RED),
                        labelColor = StatusColors.text(ExpiryState.RED)
                    ),
                    border = null
                )
            }
        }

        // Zilele săptămânii, începând cu luni
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            CalendarMonth.WEEKDAYS.forEachIndexed { i, d ->
                Text(
                    d,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (i >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val month = baseMonth.plusMonths((page - CENTER_PAGE).toLong())
            MonthGrid(
                month = month,
                byDate = byDate,
                today = today,
                expanded = vm.calendarExpanded,
                onDayClick = { selectedEpochDay = it.toEpochDay() }
            )
        }

        Legend(Modifier.padding(vertical = 6.dp))
    }

    selectedEpochDay?.let { epochDay ->
        val day = LocalDate.ofEpochDay(epochDay)
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(onDismissRequest = { selectedEpochDay = null }, sheetState = sheetState) {
            DaySheetContent(
                day = day,
                items = byDate[day].orEmpty(),
                today = today,
                onOpenItem = { selectedEpochDay = null; onOpenItem(it) },
                onAdd = { selectedEpochDay = null; onAddForDate(day) }
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    byDate: Map<LocalDate, List<Item>>,
    today: LocalDate,
    expanded: Boolean,
    onDayClick: (LocalDate) -> Unit,
) {
    val cells = remember(month) { CalendarMonth.cells(month) }
    val cellHeight = if (expanded) 132.dp else 80.dp
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 3.dp)
    ) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val cellModifier = Modifier.weight(1f).height(cellHeight).padding(1.5.dp)
                    if (date == null) {
                        Spacer(cellModifier)
                    } else {
                        DayCell(date, byDate[date].orEmpty(), today, expanded, cellModifier) { onDayClick(date) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    items: List<Item>,
    today: LocalDate,
    expanded: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val state = Expiry.worstState(items, today)
    val shape = RoundedCornerShape(8.dp)
    // Compact: până la 4 miniaturi (2×2). Extins: până la 2 miniaturi mari, una sub alta.
    val maxThumbs = if (expanded) 2 else 4
    val hidden = items.size - maxThumbs

    Column(
        modifier
            .clip(shape)
            .background(StatusColors.background(state))
            .border(if (state != null) 1.5.dp else 0.5.dp, StatusColors.border(state), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.5.dp, vertical = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
            DayNumber(date, isToday = date == today)
            Spacer(Modifier.weight(1f))
            if (state == ExpiryState.RED) {
                Text("!", color = StatusColors.text(state), fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            if (hidden > 0) {
                Text("+$hidden", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusColors.text(state))
            }
        }
        Spacer(Modifier.height(2.dp))
        if (items.isNotEmpty()) {
            val shown = items.take(maxThumbs)
            if (expanded) {
                Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    shown.forEach { CalendarThumb(it, 14.dp, Modifier.fillMaxWidth().weight(1f)) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    shown.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            pair.forEach { CalendarThumb(it, 9.dp, Modifier.weight(1f).aspectRatio(1f)) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayNumber(date: LocalDate, isToday: Boolean) {
    if (isToday) {
        Box(
            Modifier.size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text("${date.dayOfMonth}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Text("${date.dayOfMonth}", fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 2.dp))
    }
}

/** Miniatura unui produs în căsuța calendarului, cu indicatorul „deschis” în colț. */
@Composable
private fun CalendarThumb(item: Item, dotSize: Dp, modifier: Modifier) {
    Box(modifier) {
        ProductImage(item.imagePath, item.name, Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp)))
        if (item.isOpened) {
            OpenedDot(dotSize, Modifier.align(Alignment.TopEnd).padding(1.dp))
        }
    }
}

@Composable
private fun DaySheetContent(
    day: LocalDate,
    items: List<Item>,
    today: LocalDate,
    onOpenItem: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(CalendarMonth.longDate(day), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (items.isEmpty()) {
            Text("Nimic nu expiră în această zi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                if (items.size == 1) "1 produs" else "${items.size} produse",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            items.forEach { item -> ItemRow(item, today) { onOpenItem(item.id) } }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text("Adaugă un produs care expiră în această zi")
        }
    }
}
