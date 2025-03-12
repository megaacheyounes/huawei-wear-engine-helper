package com.younes.wearenginehelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.huawei.wearengine.auth.Permission
import com.younes.wearenginehelper.ui.theme.WearEngineHelperTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object Constants {
    const val DEFAULT_JSON = """
        {
            "type": "account_sync",
            "data": {
                "name": "John Doe",
                "access_token": "asdf1234",
                "refresh_token": "asdf1234"
            }
        }
    """

    const val TAB_SEND = 0
    const val TAB_RECEIVE = 1
}

/**
 * Data class representing the UI state of the application
 */
data class UiState(
    val isLoading: Boolean = false,
    val message: String = Constants.DEFAULT_JSON,
    val error: String? = null,
    val watchName: String? = null,
    val isSuccessful: Boolean = false,
    val receivedMessage: String? = null,
    val selectedTab: Int = Constants.TAB_SEND
)

/**
 * Main activity for the WearEngineHelper demo application
 */
class MainActivity : ComponentActivity() {

    private var state by mutableStateOf(UiState())

    //TODO: add your app information
    private val wearEngineHelper = WearEngineHelper.Builder(MyApp.instance)
        .setWatchPackageName("com.example.watchapp")
        .setWatchFingerprintDebug("WATCH_APP_DEBUG_FINGERPRINT")
        .setWatchFingerprintRelease("WATCH_APP_RELEASE_FINGERPRINT")
        .setPermissions(arrayOf(Permission.DEVICE_MANAGER)) //Wear Engine permission applied for
        .setWatchPackageNameNextGen("com.example.watchapp.nextgen") // For next-gen watches
        .setWatchAppId("WATCH_APP_ID") // For next-gen watches
        /**
         * if your debug and release mobile app have different package names
         * set the setDebugMode flag to TRUE during development and testing, then change it manually to FALSE before creating a release build.
         * TODO: review before publishing
         */
        .setDebugMode(true)
        .setLogTag("WearEngineHelper")
        .enableVerboseLogging(true)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WearEngineHelperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(
                        modifier = Modifier.padding(innerPadding),
                        state = state,
                        onSendMessage = ::sendDataToWatch,
                        onTabSelected = { newTab ->
                            state = state.copy(selectedTab = newTab)
                        },
                        onMessageTextChanged = { newText ->
                            state = state.copy(message = newText)
                        }
                    )
                }
            }
        }
    }

    /**
     * Sends data to the connected watch
     */
    private fun sendDataToWatch() {
        // Reset state before sending
        state = state.copy(
            isLoading = true,
            error = null,
            isSuccessful = false
        )

        wearEngineHelper.sendMessageToWatch(
            state.message,
            onDeviceConnected = { deviceName ->
                state = state.copy(watchName = deviceName)
            },
            onSuccess = {
                state = state.copy(
                    isLoading = false,
                    isSuccessful = true
                )
            },
            onError = { errorMessage, errorCode ->
                state = state.copy(
                    error = "ERROR $errorCode: $errorMessage",
                    isLoading = false
                )
            }
        )
    }

    override fun onResume() {
        super.onResume()
        wearEngineHelper.hasConnectedWatch { hasWatch ->
            if (hasWatch)
                registerWearEngineReceiver()
        }
    }

    /**
     * Registers the receiver for messages from the watch
     */
    private fun registerWearEngineReceiver() {
        wearEngineHelper.registerReceiver(
            onDeviceConnected = { deviceName ->
                state = state.copy(watchName = deviceName)
            },
            onMessageReceived = { message ->
                // Add timestamp and new message to existing messages
                val formattedMessage = """
                    |--- received at ${getCurrentTime()} ---
                    |$message 
                    |${state.receivedMessage ?: ""}
                """.trimMargin()

                state = state.copy(
                    selectedTab = Constants.TAB_RECEIVE,
                    receivedMessage = formattedMessage
                )
            },
            onError = { errorMessage, errorCode ->
                state = state.copy(
                    error = "Failed to register receiver: ERROR $errorCode: $errorMessage",
                )
            }
        )
    }

    /**
     * Returns the current time formatted as HH:mm:ss
     */
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onStop() {
        super.onStop()
        wearEngineHelper.unregisterReceiver()
    }

}

/**
 * Main content composable for the application
 */
@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    state: UiState,
    onSendMessage: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onMessageTextChanged: (String) -> Unit = {}
) {
    Column(
        modifier = modifier.padding(16.dp),
    ) {
        HeaderSection()

        Spacer(Modifier.height(16.dp))

        MessageTabs(
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelected,
            jsonMessage = state.message,
            receivedMessage = state.receivedMessage,
            onMessageTextChanged = onMessageTextChanged
        )

        Spacer(Modifier.height(32.dp))

        ConnectedWatchInfo(watchName = state.watchName)

        Spacer(Modifier.height(32.dp))

        SendMessageButton(
            isLoading = state.isLoading,
            onSendMessage = onSendMessage
        )

        Spacer(Modifier.height(16.dp))

        // Show error or success message
        state.error?.let {
            StatusAlert(isError = true, message = it)
        }

        if (state.isSuccessful) {
            StatusAlert(isError = false, message = "Message sent successfully")
        }
    }
}

/**
 * Displays the header section with the title
 */
@Composable
private fun HeaderSection() {
    Text(
        text = "WearEngineHelper Demo",
        style = MaterialTheme.typography.titleMedium
    )
}

/**
 * Composable that displays the send/receive tabs
 */
@Composable
fun MessageTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    jsonMessage: String,
    receivedMessage: String?,
    onMessageTextChanged: (String) -> Unit
) {
    val tabs = listOf("Send", "Receive")

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            modifier = Modifier.padding(horizontal = 24.dp),
            selectedTabIndex = selectedTab
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) }
                )
            }
        }

        when (selectedTab) {
            Constants.TAB_SEND -> {
                MessageTextField(
                    label = "Message to send",
                    value = jsonMessage,
                    readOnly = false,
                    onValueChange = onMessageTextChanged
                )
            }

            Constants.TAB_RECEIVE -> {
                MessageTextField(
                    label = "Received messages",
                    value = receivedMessage ?: "Waiting for the watch...",
                    readOnly = true,
                    onValueChange = {}
                )
            }
        }
    }
}

/**
 * Reusable text field for messages
 */
@Composable
private fun MessageTextField(
    label: String,
    value: String,
    readOnly: Boolean,
    onValueChange: (String) -> Unit
) {
    TextField(
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .clip(RoundedCornerShape(16.dp)),
        maxLines = 10,
        value = value,
        readOnly = readOnly,
        onValueChange = onValueChange
    )
}

/**
 * Displays a status alert (error or success)
 */
@Composable
fun StatusAlert(isError: Boolean, message: String) {
    val backgroundColor = if (isError) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
    } else {
        Color.Green.copy(alpha = 0.25f)
    }

    val icon = if (isError) Icons.Filled.Warning else Icons.Filled.Check

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(24.dp),
            imageVector = icon,
            contentDescription = if (isError) "Error" else "Success"
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Displays information about the connected watch
 */
@Composable
fun ConnectedWatchInfo(watchName: String?) {
    Row(modifier = Modifier) {
        Image(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Filled.Watch,
            contentDescription = "Smartwatch icon"
        )

        Spacer(Modifier.width(8.dp))

        Column {
            if (watchName != null) {
                Text(
                    text = "Watch connected",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = watchName,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("Watch not connected")
            }
        }
    }
}

/**
 * Button to send a message to the watch
 */
@Composable
fun SendMessageButton(
    isLoading: Boolean,
    onSendMessage: () -> Unit
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSendMessage,
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(16.dp))
        } else {
            Box(Modifier.size(16.dp))
        }

        Spacer(Modifier.width(8.dp))

        Text("Send message to watch")

        // Spacer to keep text centered
        Box(Modifier.size(16.dp))
    }
}

/************************* PREVIEW functions *************************************/

@Preview(showBackground = true)
@Composable
private fun Preview_Default() {
    WearEngineHelperTheme {
        MainContent(state = UiState())
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Loading() {
    WearEngineHelperTheme {
        MainContent(
            state = UiState(
                watchName = "HUAWEI WATCH GT5 Pro",
                isLoading = true
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_SendSuccess() {
    WearEngineHelperTheme {
        MainContent(
            state = UiState(
                watchName = "HUAWEI WATCH GT5 Pro",
                isSuccessful = true
            )
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_5)
@Composable
private fun Preview_SendFailed() {
    WearEngineHelperTheme {
        MainContent(
            state = UiState(
                error = "Error 1: Lorem Ipsum Dolor Sit Amet!"
            )
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_5)
@Composable
private fun Preview_ReceivedData() {
    WearEngineHelperTheme {
        MainContent(
            state = UiState(
                receivedMessage = """
                {
                    "type": "device_sync",
                    "data": {
                        "device_name": "Huawei Watch GT 5 Pro",
                        "device_id": "1234-1234-1234-1234"
                    }
                }
                """.trimIndent(),
                selectedTab = Constants.TAB_RECEIVE
            )
        )
    }
}