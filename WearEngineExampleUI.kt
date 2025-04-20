@file:OptIn(ExperimentalMaterial3Api::class)

package com.younes.wearenginehelper

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.younes.wearenginehelper.ui.theme.Grey300
import com.younes.wearenginehelper.ui.theme.PurpleGrey80
import com.younes.wearenginehelper.ui.theme.WearEngineHelperTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Represents the various states in the pairing process flow
 */
enum class PairingStep {
    INITIAL,      // Initial state when not paired
    CONNECTING,   // Currently attempting to pair
    SUCCESS,      // Pairing completed successfully
    ERROR         // Pairing attempt failed
}

/**
 * Data class representing the UI state for the pairing screen
 */
data class PairingUiState(
    val pairingStep: PairingStep = PairingStep.INITIAL,
    val error: String? = null
)

/**
 * Main composable for demonstrating the pairing process
 */
@Composable
fun PairingScreenExample() {

    var state by remember { mutableStateOf(PairingUiState()) }
    val scope = rememberCoroutineScope()

    PairingScreenContent(
        uiState = state,
        onConnect = {
            // Update UI to show pairing in progress
            state = state.copy(pairingStep = PairingStep.CONNECTING)

            scope.launch {
                // Simulate pinging and message sending delay
                delay(3000)

                // Simulate random success/failure
                val isSuccessful = (0..1).random() == 1
                state = if (isSuccessful) {
                    state.copy(pairingStep = PairingStep.SUCCESS)
                } else {
                    state.copy(
                        pairingStep = PairingStep.ERROR,
                        error = "Could not connect to watch"
                    )
                }
            }
        },
        onErrorDismiss = {
            state = state.copy(pairingStep = PairingStep.INITIAL)
        },
        onSuccessDismiss = {
            state = state.copy(pairingStep = PairingStep.INITIAL)
        }
    )
}

/**
 * Base scaffold for the pairing screen with consistent styling
 */
@Composable
fun PairingScaffold(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Wear Engine demo UI", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back"
                        )
                    }
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            content()
        }
    }
}

/**
 * Content container that displays different UI based on current pairing state
 */
@Composable
fun PairingScreenContent(
    uiState: PairingUiState,
    onConnect: () -> Unit = {},
    onErrorDismiss: () -> Unit = {},
    onSuccessDismiss: () -> Unit = {}
) {
    PairingScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Display different UI components based on current pairing step
            when (uiState.pairingStep) {
                PairingStep.INITIAL -> InitialPairingScreen(onConnect)
                PairingStep.CONNECTING -> ConnectingScreen()
                PairingStep.SUCCESS -> SuccessScreen(onSuccessDismiss)
                PairingStep.ERROR -> ErrorScreen(uiState.error, onErrorDismiss)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Initial state UI when no pairing is in progress
 */
@Composable
fun InitialPairingScreen(onConnect: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WatchIllustration(showBox = false)

        Spacer(modifier = Modifier.height(64.dp))

        HowToUseTips()

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onConnect) {
            Text("Connect")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * UI shown when pairing attempt has failed
 */
@Composable
fun ErrorScreen(
    error: String?,
    onErrorDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WatchIllustration(
            showBox = false,
            statusIcon = Icons.Filled.Close,
            iconColor = MaterialTheme.colorScheme.error
        )

        Text(
            text = "Failed to connect",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        error?.let {
            Text(
                text = it,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        val troubleshootingTips = listOf(
            "Make sure Bluetooth and Wifi are enabled.",
            "Make sure that the watch is connected in Huawei Health.",
            "Make sure that the app is running on the watch."
        )

        TipsList(
            title = "Troubleshooting Tips",
            tips = troubleshootingTips,
            backgroundColor = PurpleGrey80
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onErrorDismiss) {
            Text("Close")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * UI shown when pairing is successful
 */
@Composable
fun SuccessScreen(onSuccessDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WatchIllustration(
            showBox = false,
            statusIcon = Icons.Filled.Check,
            iconColor = Color.Green
        )

        Text(
            text = "Watch connected successfully",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enjoy the app on your Huawei watch.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        HowToUseTips()

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onSuccessDismiss) {
            Text("Close")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * UI shown while pairing is in progress
 */
@Composable
fun ConnectingScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WatchAndPhoneIllustration()

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connecting to Watch... this should take a minute.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        HowToUseTips()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Component displaying usage tips for the watch
 */
@Composable
fun HowToUseTips() {
    val tips = listOf(
        "Watch feature description 1",
        "Watch feature description 2",
        "Watch feature description 3"
    )
    TipsList(title = "How to use", tips = tips)
}

/**
 * Reusable component for displaying a list of tips
 */
@Composable
fun TipsList(
    title: String,
    tips: List<String>,
    backgroundColor: Color = Grey300
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    tips.forEach { tip ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * UI component showing watch illustration with optional status icon
 */
@Composable
fun WatchIllustration(
    modifier: Modifier = Modifier,
    showBox: Boolean = true,
    statusIcon: ImageVector? = null,
    iconColor: Color? = null
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .background(
                if (showBox) Color.Gray.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(150.dp),
            painter = painterResource(R.drawable.watch_icon),
            contentDescription = null
        )

        statusIcon?.let {
            Icon(
                modifier = Modifier
                    .padding(bottom = 16.dp, start = 16.dp)
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(4.dp)
                    .align(Alignment.BottomStart),
                imageVector = statusIcon,
                tint = iconColor ?: Color.Green,
                contentDescription = null
            )
        }
    }
}

/**
 * UI component showing watch and phone illustrations
 */
@Composable
fun WatchAndPhoneIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier.size(150.dp),
                painter = painterResource(R.drawable.watch_icon),
                contentDescription = "Watch"
            )

            Spacer(modifier = Modifier.width(12.dp))

            Image(
                modifier = Modifier.size(150.dp),
                painter = painterResource(R.drawable.phone_icon),
                contentDescription = "Phone"
            )
        }
    }
}

// Preview Composables
@Preview(showBackground = true)
@Composable
private fun Preview_Initial() {
    WearEngineHelperTheme {
        PairingScreenContent(
            uiState = PairingUiState(pairingStep = PairingStep.INITIAL)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Connecting() {
    WearEngineHelperTheme {
        PairingScreenContent(
            uiState = PairingUiState(pairingStep = PairingStep.CONNECTING)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Success() {
    WearEngineHelperTheme {
        PairingScreenContent(
            uiState = PairingUiState(pairingStep = PairingStep.SUCCESS)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Error() {
    WearEngineHelperTheme {
        PairingScreenContent(
            uiState = PairingUiState(
                pairingStep = PairingStep.ERROR,
                error = "App is not installed on the watch"
            )
        )
    }
}