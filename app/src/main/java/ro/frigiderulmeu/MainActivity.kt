package ro.frigiderulmeu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ro.frigiderulmeu.domain.CalendarMonth
import ro.frigiderulmeu.domain.Today
import ro.frigiderulmeu.ui.AddScreen
import ro.frigiderulmeu.ui.CalendarScreen
import ro.frigiderulmeu.ui.DetailScreen
import ro.frigiderulmeu.ui.FridgeScreen
import ro.frigiderulmeu.ui.FridgeViewModel
import ro.frigiderulmeu.ui.FrigiderTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrigiderTheme { App() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Data se recalculează la fiecare redeschidere a aplicației.
        Today.refresh()
    }
}

private const val TAB_CALENDAR = 0
private const val TAB_FRIDGE = 1
private const val TAB_ADD = 2

@Composable
private fun App(vm: FridgeViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(TAB_CALENDAR) }
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var addDateEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Schimbarea zilei la miezul nopții, cât timp aplicația este deschisă.
    LaunchedEffect(Unit) {
        while (true) {
            delay(Today.millisUntilMidnight())
            Today.refresh()
        }
    }

    BackHandler(enabled = detailId == null && tab != TAB_CALENDAR) { tab = TAB_CALENDAR }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (detailId == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == TAB_CALENDAR,
                        onClick = { tab = TAB_CALENDAR },
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        label = { Text("Calendar") }
                    )
                    NavigationBarItem(
                        selected = tab == TAB_FRIDGE,
                        onClick = { tab = TAB_FRIDGE },
                        icon = { Icon(painterResource(R.drawable.ic_fridge), contentDescription = null) },
                        label = { Text("Frigider") }
                    )
                    NavigationBarItem(
                        selected = tab == TAB_ADD,
                        onClick = { addDateEpoch = null; tab = TAB_ADD },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        label = { Text("Adaugă") }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val openId = detailId
            if (openId != null) {
                DetailScreen(
                    vm = vm,
                    itemId = openId,
                    onBack = { detailId = null },
                    onOpenedSplit = { opened ->
                        scope.launch {
                            val result = snackbar.showSnackbar(
                                message = "Bucata deschisă e acum o intrare separată: expiră ${CalendarMonth.shortDate(opened.expiryDate, Today.date.value)}",
                                actionLabel = "Vezi",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) detailId = opened.id
                        }
                    },
                    onConsumed = { item ->
                        scope.launch {
                            val result = snackbar.showSnackbar(
                                message = "„${item.name}” a fost scos din frigider",
                                actionLabel = "Anulează",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) vm.restore(item)
                        }
                    }
                )
            } else {
                when (tab) {
                    TAB_CALENDAR -> CalendarScreen(
                        vm = vm,
                        onOpenItem = { detailId = it },
                        onAddForDate = { addDateEpoch = it.toEpochDay(); tab = TAB_ADD },
                        onShowExpired = { tab = TAB_FRIDGE }
                    )
                    TAB_FRIDGE -> FridgeScreen(vm = vm, onOpenItem = { detailId = it })
                    else -> AddScreen(
                        vm = vm,
                        initialDate = addDateEpoch?.let(LocalDate::ofEpochDay),
                        onSaved = { name, expiry ->
                            scope.launch {
                                snackbar.showSnackbar(
                                    "Salvat: $name · expiră ${CalendarMonth.shortDate(expiry, Today.date.value)}",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
