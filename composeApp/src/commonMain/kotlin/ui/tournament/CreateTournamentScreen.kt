package ui.tournament

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import data.model.BracketType
import data.model.CreatorType
import data.model.Tournament
import data.model.TournamentStatus
import kotlinx.coroutines.launch
import navigation.Home
import org.koin.compose.koinInject
import ui.components.ExpectedDatePickerDialog
import ui.components.formatPlatformDate
import viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = koinInject()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDateString by remember { mutableStateOf("") }
    var endDateString by remember { mutableStateOf("") }
    var maxTeams by remember { mutableStateOf("8") }
    var selectedBracketType by remember { mutableStateOf(BracketType.SINGLE_ELIMINATION) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }

    // Get current date in milliseconds for date validation
    val currentDateMillis = remember { kotlinx.datetime.Clock.System.now().toEpochMilliseconds() }

    val dateFormat = "yyyy-MM-dd"

    val isLoading by adminViewModel.isLoading.collectAsState()
    val errorMessage by adminViewModel.errorMessage.collectAsState()
    val currentUser by adminViewModel.currentUser.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var formSubmitted by remember { mutableStateOf(false) }


    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearError()
        }
    }

    ExpectedDatePickerDialog(
        show = showStartDatePicker,
        initialDateMillis = startDateMillis ?: currentDateMillis,
        onDismissRequest = { showStartDatePicker = false },
        onDateSelected = { millis ->
            // Validate that selected date is not in the past
            if (millis < currentDateMillis) {
                scope.launch {
                    snackbarHostState.showSnackbar("Cannot select a past date for tournament start")
                }
                // Don't update the date if it's invalid
                return@ExpectedDatePickerDialog
            }
            startDateMillis = millis
            startDateString = formatPlatformDate(millis, dateFormat)
            showStartDatePicker = false
        }
    )

    ExpectedDatePickerDialog(
        show = showEndDatePicker,
        initialDateMillis = endDateMillis ?: (startDateMillis ?: currentDateMillis),
        onDismissRequest = { showEndDatePicker = false },
        onDateSelected = { millis ->
            // Validate that selected date is not in the past
            if (millis < currentDateMillis) {
                scope.launch {
                    snackbarHostState.showSnackbar("Cannot select a past date for tournament end")
                }
                // Don't update the date if it's invalid
                return@ExpectedDatePickerDialog
            }
            endDateMillis = millis
            endDateString = formatPlatformDate(millis, dateFormat)
            showEndDatePicker = false
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tournament Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = name.isBlank() && (startDateString.isNotBlank() || endDateString.isNotBlank())
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
                isError = formSubmitted && startDateString.isBlank()  // Only show error after submit
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
                isError = formSubmitted && endDateString.isBlank()  // Only show error after submit
            )

            OutlinedTextField(
                value = maxTeams,
                onValueChange = { if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() > 0)) maxTeams = it },
                label = { Text("Maximum Teams") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Tournament Format")
            Column {
                BracketType.entries.forEach { bracketType ->
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
                    // Set formSubmitted to true to display validation errors
                    formSubmitted = true

                    if (name.isBlank() || startDateString.isBlank() || endDateString.isBlank() || (maxTeams.toIntOrNull() ?: 0) <= 0) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fill all fields correctly.")
                        }
                        return@Button
                    }

                    if (startDateMillis != null && endDateMillis != null && startDateMillis!! >= endDateMillis!!) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Start date must be before end date.")
                        }
                        return@Button
                    }

                    val tournament = Tournament(
                        name = name,
                        description = description,
                        creatorId = currentUser?.id ?: "",
                        creatorType = CreatorType.ADMIN,
                        startDate = startDateString,
                        endDate = endDateString,
                        status = TournamentStatus.UPCOMING,
                        maxTeams = maxTeams.toIntOrNull() ?: 8,
                        bracketType = selectedBracketType
                    )

                    scope.launch {
                        val success = adminViewModel.createTournamentInDb(tournament)
                        if (success) {
                            // Simply go back to the previous screen (admin panel)
                            navController.popBackStack()
                        }
                        // Error message will be shown by the LaunchedEffect from ViewModel
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && currentUser != null
            ) {
                Text("Create Tournament")
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}