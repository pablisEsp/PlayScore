// composeApp/src/commonMain/kotlin/ui/tournament/EditTournamentScreen.kt
package ui.tournament

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.BracketType
import data.model.Tournament
import data.model.TournamentStatus
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ui.components.ExpectedDatePickerDialog
import ui.components.formatPlatformDate
import viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTournamentScreen(
    navController: NavController,
    tournamentId: String,
    adminViewModel: AdminViewModel = koinInject()
) {
    var tournament by remember { mutableStateOf<Tournament?>(null) }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDateString by remember { mutableStateOf("") }
    var endDateString by remember { mutableStateOf("") }
    var maxTeams by remember { mutableStateOf("8") }
    var status by remember { mutableStateOf(TournamentStatus.UPCOMING) }
    var selectedBracketType by remember { mutableStateOf(BracketType.SINGLE_ELIMINATION) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }

    val dateFormat = "yyyy-MM-dd"

    val isLoading by adminViewModel.isLoading.collectAsState()
    val errorMessage by adminViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load the tournament data
    LaunchedEffect(tournamentId) {
        scope.launch {
            val loadedTournament = adminViewModel.tournamentRepository.getTournamentById(tournamentId)
            if (loadedTournament != null) {
                tournament = loadedTournament
                name = loadedTournament.name
                description = loadedTournament.description
                startDateString = loadedTournament.startDate
                endDateString = loadedTournament.endDate
                maxTeams = loadedTournament.maxTeams.toString()
                status = loadedTournament.status
                selectedBracketType = loadedTournament.bracketType
            } else {
                snackbarHostState.showSnackbar("Tournament not found")
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearError()
        }
    }

    ExpectedDatePickerDialog(
        show = showStartDatePicker,
        initialDateMillis = startDateMillis,
        onDismissRequest = { showStartDatePicker = false },
        onDateSelected = { millis ->
            startDateMillis = millis
            startDateString = formatPlatformDate(millis, dateFormat)
            showStartDatePicker = false
        }
    )

    ExpectedDatePickerDialog(
        show = showEndDatePicker,
        initialDateMillis = endDateMillis,
        onDismissRequest = { showEndDatePicker = false },
        onDateSelected = { millis ->
            endDateMillis = millis
            endDateString = formatPlatformDate(millis, dateFormat)
            showEndDatePicker = false
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Tournament") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tournament Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = name.isBlank()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = startDateString,
                onValueChange = { /* Read-only */ },
                label = { Text("Start Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartDatePicker = true },
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Start Date")
                    }
                },
                readOnly = true,
                isError = startDateString.isBlank()
            )

            OutlinedTextField(
                value = endDateString,
                onValueChange = { /* Read-only */ },
                label = { Text("End Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndDatePicker = true },
                trailingIcon = {
                    IconButton(onClick = { showEndDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select End Date")
                    }
                },
                readOnly = true,
                isError = endDateString.isBlank()
            )

            OutlinedTextField(
                value = maxTeams,
                onValueChange = { if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() > 0)) maxTeams = it },
                label = { Text("Maximum Teams") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Tournament Status")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TournamentStatus.values().forEach { statusOption ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        RadioButton(
                            selected = status == statusOption,
                            onClick = { status = statusOption }
                        )
                        Text(
                            text = statusOption.name.replace('_', ' '),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text("Tournament Format")
            Column {
                BracketType.values().forEach { bracketType ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBracketType = bracketType }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedBracketType == bracketType,
                            onClick = { selectedBracketType = bracketType }
                        )
                        Text(
                            text = bracketType.name.replace('_', ' '),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || startDateString.isBlank() || endDateString.isBlank() || (maxTeams.toIntOrNull() ?: 0) <= 0) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fill all fields correctly")
                        }
                        return@Button
                    }

                    if (startDateMillis != null && endDateMillis != null && startDateMillis!! >= endDateMillis!!) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Start date must be before end date")
                        }
                        return@Button
                    }

                    tournament?.let {
                        val updatedTournament = it.copy(
                            name = name,
                            description = description,
                            startDate = startDateString,
                            endDate = endDateString,
                            maxTeams = maxTeams.toIntOrNull() ?: 8,
                            status = status,
                            bracketType = selectedBracketType
                        )

                        scope.launch {
                            val success = adminViewModel.tournamentRepository.updateTournament(updatedTournament)
                            if (success) {
                                snackbarHostState.showSnackbar("Tournament updated successfully")
                                navController.popBackStack()
                            } else {
                                snackbarHostState.showSnackbar("Failed to update tournament")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && tournament != null
            ) {
                Text("Save Changes")
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}