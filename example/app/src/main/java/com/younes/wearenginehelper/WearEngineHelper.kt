/**
 * WearEngineHelper - A wrapper for Huawei WearEngine SDK
 *
 * This helper simplifies communication between a mobile app and Huawei watches,
 * supporting both legacy and next-generation HarmonyOS devices.
 *
 * Copyright (c) 2025
 * Younes Megaache
 * Created 10 March 2025
 */

package com.younes.wearenginehelper

import android.content.Context
import android.util.Log
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.WearEngineException
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.AuthClient
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.common.WearEngineErrorCode
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * WearEngineHelper provides a simplified interface for communication with Huawei watches.
 *
 * Features:
 * - Send and receive messages between mobile and watch apps
 * - Support for both legacy and next-gen Huawei watch devices
 * - Automatic permission handling
 * - Builder pattern for configuration
 *
 * @param context Application context
 */
class WearEngineHelper private constructor(
    private val context: Context,
    private val config: WearEngineConfig
) {
    /**
     * Configuration class for WearEngineHelper
     */
    data class WearEngineConfig(
        /** Package name for legacy/lite watch devices */
        val watchPackageName: String,
        /** Debug fingerprint for legacy/lite devices */
        val watchFingerprintDebug: String,
        /** Release fingerprint for legacy/lite devices */
        val watchFingerprintRelease: String,
        /** Package name for next-gen watches (null uses legacy name) */
        val watchPackageNameNext: String? = null,
        /** App ID for next-gen watches */
        val watchAppId: String? = null,
        /** List of required permissions */
        val permissions: Array<Permission> = arrayOf(Permission.DEVICE_MANAGER),
        /** Debug mode flag (true for development, false for production) */
        val isDebugMode: Boolean = false,
        /** Custom log tag */
        val logTag: String = DEFAULT_LOG_TAG,
        /** Enable detailed logging */
        val verboseLogging: Boolean = false
    )

    /**
     * Builder for WearEngineHelper configuration
     */
    class Builder(private val context: Context) {
        private var watchPackageName: String? = null
        private var watchFingerprintDebug: String? = null
        private var watchFingerprintRelease: String? = null
        private var watchPackageNameNext: String? = null
        private var watchAppId: String? = null
        private var permissions: Array<Permission> = arrayOf(Permission.DEVICE_MANAGER)
        private var isDebugMode: Boolean = false
        private var logTag: String = DEFAULT_LOG_TAG
        private var verboseLogging: Boolean = false

        /**
         * Sets the package name for legacy/lite watch devices.
         * @param packageName The watch app's package name
         */
        fun setWatchPackageName(packageName: String) = apply {
            this.watchPackageName = packageName
        }

        /**
         * Sets the debug fingerprint for legacy/lite watch devices.
         * @param fingerprint The debug version fingerprint
         */
        fun setWatchFingerprintDebug(fingerprint: String) = apply {
            this.watchFingerprintDebug = fingerprint
        }

        /**
         * Sets the release fingerprint for legacy/lite watch devices.
         * @param fingerprint The release version fingerprint
         */
        fun setWatchFingerprintRelease(fingerprint: String) = apply {
            this.watchFingerprintRelease = fingerprint
        }

        /**
         * Sets the package name for next-gen watch devices.
         * Required only for supporting both legacy and new watch devices.
         * @param packageName The next-gen watch app's package name
         */
        fun setWatchPackageNameNextGen(packageName: String) = apply {
            this.watchPackageNameNext = packageName
        }

        /**
         * Sets the app ID for next-gen watch devices.
         * @param appId The next-gen watch app's ID
         */
        fun setWatchAppId(appId: String) = apply {
            this.watchAppId = appId
        }

        fun setPermissions(permissions: Array<Permission>) = apply {
            this.permissions = permissions
        }

        /**
         * Sets whether the watch app is in debug mode.
         * @param isDebug True for debug mode, false for release
         */
        fun setDebugMode(isDebug: Boolean) = apply {
            this.isDebugMode = isDebug
        }

        /**
         * Sets a custom log tag for the helper.
         * @param tag Custom log tag
         */
        fun setLogTag(tag: String) = apply {
            this.logTag = tag
        }

        /**
         * Enables or disables verbose logging.
         * @param enable True to enable verbose logging
         */
        fun enableVerboseLogging(enable: Boolean) = apply {
            this.verboseLogging = enable
        }

        /**
         * Builds a WearEngineHelper instance with the configured parameters.
         * @throws IllegalArgumentException if required parameters are missing
         */
        fun build(): WearEngineHelper {
            // Validate required parameters
            val packageName =
                watchPackageName ?: throw IllegalArgumentException("Watch package name must be set")
            val fingerprintDebug = watchFingerprintDebug
                ?: throw IllegalArgumentException("Debug fingerprint must be set")
            val fingerprintRelease = watchFingerprintRelease
                ?: throw IllegalArgumentException("Release fingerprint must be set")

            val config = WearEngineConfig(
                watchPackageName = packageName,
                watchFingerprintDebug = fingerprintDebug,
                watchFingerprintRelease = fingerprintRelease,
                watchPackageNameNext = watchPackageNameNext,
                watchAppId = watchAppId,
                permissions = permissions,
                isDebugMode = isDebugMode,
                logTag = logTag,
                verboseLogging = verboseLogging
            )

            return WearEngineHelper(context, config)
        }
    }

    // Lazy-initialized clients
    private val authClient: AuthClient by lazy { HiWear.getAuthClient(context) }
    private val deviceClient: DeviceClient by lazy { HiWear.getDeviceClient(context) }
    private val p2pClient: P2pClient by lazy { initP2pClient() }

    private var connectedDevice: Device? = null
    private var receiver: Receiver? = null

    // Client initialization
    private fun initP2pClient(): P2pClient {
        return HiWear.getP2pClient(context).apply {
            val isNextGen = connectedDevice?.let {
                isNextGenDevice(it.model)
            } ?: false

            log("Initializing P2pClient, isNextGen: $isNextGen")

            setPeerPkgName(
                if (isNextGen && config.watchPackageNameNext != null)
                    config.watchPackageNameNext
                else
                    config.watchPackageName
            )

            setPeerFingerPrint(
                if (isNextGen && config.watchAppId != null)
                    config.watchAppId
                else if (config.isDebugMode)
                    config.watchFingerprintDebug
                else
                    config.watchFingerprintRelease
            )
        }
    }

    /**
     * Checks if a device is a next-generation watch device.
     * @param deviceModel The device model string
     * @return true if it's a next-gen device, false otherwise
     */
    private fun isNextGenDevice(deviceModel: String): Boolean {
        val keywords = listOf("rts", "soc", "rates")
        val model = deviceModel.lowercase()
        return keywords.any { keyword -> model.contains(keyword) }
    }

    /**
     * Sends a string message to the connected Huawei watch.
     *
     * @param data The data message to send (must be less than 1KB)
     * @param onDeviceConnected Callback when device is connected, provides connected watch name
     * @param onSuccess Callback on successful message delivery
     * @param onError Callback when an error occurs with error message and code
     */
    fun sendMessageToWatch(
        data: String,
        onDeviceConnected: (String) -> Unit,
        onSuccess: () -> Unit,
        onError: (String, Int) -> Unit
    ) {
        // Convert data to bytes
        val messageBytes = data.toByteArray()

        // Check message size
        if (messageBytes.size > MAX_MESSAGE_SIZE) {
            onError("Message size exceeds the ${MAX_MESSAGE_SIZE} byte limit", -1)
            return
        }

        // Handle permissions and send message
        checkAndRequestPermissions { permissionsGranted ->
            if (permissionsGranted) {
                sendMessageToWatchImpl(
                    messageBytes,
                    onDeviceConnected,
                    onSuccess,
                    onError
                )
            } else {
                onError(
                    "Permissions not granted",
                    WearEngineErrorCode.ERROR_CODE_USER_UNAUTHORIZED_IN_HEALTH
                )
            }
        }
    }

    /**
     * Checks if the user has a compatible Huawei watch connected.
     *
     * @param onResult Callback with the result (true if available)
     */
    fun hasConnectedWatch(onResult: (Boolean) -> Unit) {
        deviceClient.hasAvailableDevices()
            .addOnSuccessListener {
                log("Has available devices: $it")
                onResult(it)
            }
            .addOnFailureListener { e ->
                logError("Failed to get available devices", e)
                onResult(false)
            }
    }

    /**
     * Registers a receiver for incoming messages from the watch.
     *
     * @param onDeviceConnected Callback when device is connected
     * @param onMessageReceived Callback for received messages
     * @param onError Callback for errors
     */
    fun registerReceiver(
        onDeviceConnected: (String) -> Unit,
        onMessageReceived: (String) -> Unit,
        onError: (String, Int) -> Unit
    ) {
        checkAndRequestPermissions { permissionsGranted ->
            if (permissionsGranted) {
                getConnectedDevice(
                    onDeviceConnected = onDeviceConnected,
                    onError = onError,
                    onSuccess = {
                        if (connectedDevice == null) {
                            onError(
                                "No connected watch found",
                                WearEngineErrorCode.ERROR_CODE_DEVICE_IS_NOT_CONNECTED
                            )
                            return@getConnectedDevice
                        }

                        // Unregister existing receiver if any
                        unregisterReceiver()

                        // Create new receiver
                        receiver = Receiver { message ->
                            log("Received message: $message")
                            message.data?.let {
                                val messageString = it.toString(Charsets.UTF_8)
                                onMessageReceived(messageString)
                            }
                        }

                        // Register the receiver
                        p2pClient.registerReceiver(connectedDevice, receiver)
                            .addOnSuccessListener {
                                log("Receiver registered successfully")
                            }
                            .addOnFailureListener { e ->
                                logError("Failed to register receiver", e)

                                onError(
                                    getErrorMessage(e, "Failed to register receiver"),
                                    getErrorCode(e)
                                )
                            }
                    }
                )
            } else {
                onError(
                    "Permissions not granted",
                    WearEngineErrorCode.ERROR_CODE_USER_UNAUTHORIZED_IN_HEALTH
                )
            }
        }
    }

    /**
     * Unregisters the receiver when no longer needed.
     */
    fun unregisterReceiver() {
        receiver?.let {
            try {
                p2pClient.unregisterReceiver(it)
                log("Receiver unregistered")
            } catch (e: Exception) {
                logError("Error unregistering receiver", e)
            }
            receiver = null
        }
    }

    /**
     * Releases resources when the helper is no longer needed.
     * Call this method in your Activity/Fragment onDestroy().
     */
    fun release() {
        unregisterReceiver()
        connectedDevice = null
        log("WearEngineHelper resources released")
    }

    /**
     * Coroutine-friendly version of hasConnectedWatch.
     * @return true if a compatible Huawei watch is connected
     */
    suspend fun hasConnectedWatchAsync(): Boolean = suspendCancellableCoroutine { continuation ->
        hasConnectedWatch { result ->
            continuation.resume(result)
        }
    }

    /**
     * Coroutine-friendly version of checkAndRequestPermissions.
     * @return true if permissions are granted
     */
    suspend fun checkAndRequestPermissionsAsync(): Boolean =
        suspendCancellableCoroutine { continuation ->
            checkAndRequestPermissions { result ->
                continuation.resume(result)
            }
        }

    /**************************** PRIVATE METHODS ************************/

    /**
     * Checks and requests required permissions.
     */
    private fun checkAndRequestPermissions(
        onPermissionResult: (Boolean) -> Unit
    ) {
        // Check existing permissions
        checkPermissions { permissionsGranted ->
            if (!permissionsGranted) {
                // Request permissions if not granted
                requestPermissions { permissionGranted ->
                    onPermissionResult(permissionGranted)
                }
            } else {
                // Permissions already granted
                onPermissionResult(true)
            }
        }

    }

    /**
     * Internal implementation for sending messages to the watch.
     */
    private fun sendMessageToWatchImpl(
        messageBytes: ByteArray,
        onDeviceConnected: (String) -> Unit,
        onSuccess: () -> Unit,
        onError: (String, Int) -> Unit
    ) {
        getConnectedDevice(onDeviceConnected, onError) {
            // Check if watch app is installed and running
            checkWatchAppStatus(
                onAppRunning = {
                    // Send message to watch
                    dispatchMessage(
                        messageBytes,
                        onSuccess,
                        onError
                    )
                },
                onError = onError
            )
        }
    }

    /**
     * Gets the currently connected Huawei watch device.
     */
    private fun getConnectedDevice(
        onDeviceConnected: (String) -> Unit,
        onError: (String, Int) -> Unit,
        onSuccess: () -> Unit
    ) {
        deviceClient.bondedDevices
            .addOnSuccessListener { devices ->
                // Get the first connected device
                val device = devices.firstOrNull { it.isConnected }

                if (device == null) {
                    onError(
                        "No connected watch found",
                        WearEngineErrorCode.ERROR_CODE_DEVICE_IS_NOT_CONNECTED
                    )
                    return@addOnSuccessListener
                }

                log("Connected device: ${device.name} (${device.model})")
                connectedDevice = device

                // Notify caller about connected device
                onDeviceConnected(device.name)
                onSuccess()
            }
            .addOnFailureListener { e ->
                logError("Failed to get bonded devices", e)
                onError(getErrorMessage(e, "There are no bound devices"), getErrorCode(e))
            }
    }

    /**
     * Checks if required permissions are granted.
     */
    private fun checkPermissions(callback: (Boolean) -> Unit) {
        try {
            authClient.checkPermissions(config.permissions)
                .addOnSuccessListener { result ->
                    val allPermissionsGranted = result.all { it == true }

                    log("Permission check result: $allPermissionsGranted")
                    callback(allPermissionsGranted)
                }
                .addOnFailureListener { e ->
                    logError("Error checking permissions", e)
                    callback(false)
                }
        } catch (e: Exception) {
            logError("Exception while checking permissions", e)
            callback(false)
        }
    }

    /**
     * Requests required permissions.
     */
    private fun requestPermissions(callback: (Boolean) -> Unit) {
        try {
            authClient.requestPermission(object : AuthCallback {
                override fun onOk(permissions: Array<out Permission>?) {
                    log("Permissions granted: ${permissions?.joinToString()}")
                    callback(permissions?.isNotEmpty() == true)
                }

                override fun onCancel() {
                    log("Permission request cancelled")
                    callback(false)
                }
            }, *config.permissions)
                .addOnSuccessListener {
                    log("Permission request initiated successfully")
                }
                .addOnFailureListener { e ->
                    logError("Error initiating permission request", e)
                    callback(false)
                }
        } catch (e: Exception) {
            logError("Exception while requesting permissions", e)
            callback(false)
        }
    }

    /**
     * Checks if the watch app is installed and running.
     */
    private fun checkWatchAppStatus(
        onAppRunning: () -> Unit,
        onError: (String, Int) -> Unit
    ) {
        if (connectedDevice == null) {
            onError(
                "No connected watch found",
                WearEngineErrorCode.ERROR_CODE_DEVICE_IS_NOT_CONNECTED
            )
            return
        }

        try {
            p2pClient.ping(connectedDevice) { resultCode ->
                log("Ping result: $resultCode")
                when (resultCode) {
                    WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_RUNNING -> {
                        log("Watch app is running")
                        onAppRunning()
                    }

                    WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_NOT_RUNNING -> {
                        log("Watch app is installed but not running")
                        onError(
                            "Watch app is installed but not running",
                            WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_NOT_RUNNING
                        )
                    }

                    WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_NOT_EXIT -> {
                        log("Watch app is not installed")
                        onError(
                            "Watch app is not installed",
                            WearEngineErrorCode.ERROR_CODE_P2P_WATCH_APP_NOT_EXIT
                        )
                    }

                    else -> {
                        log("Ping failed with code: $resultCode")
                        onError("Watch app check failed", resultCode)
                    }
                }
            }.addOnFailureListener { e ->
                logError("Error pinging watch app", e)
                onError(
                    "Error checking watch app status",
                    if (e is WearEngineException) e.errorCode else WearEngineErrorCode.ERROR_CODE_GENERIC
                )
            }
        } catch (e: Exception) {
            logError("Exception while checking watch app status", e)
            onError(getErrorMessage(e, "Error checking watch app status"), getErrorCode(e))
        }
    }

    /**
     * Dispatches a message to the connected watch device.
     */
    private fun dispatchMessage(
        messageBytes: ByteArray,
        onSuccess: () -> Unit,
        onError: (String, Int) -> Unit
    ) {
        if (connectedDevice == null) {
            onError(
                "No connected watch found",
                WearEngineErrorCode.ERROR_CODE_DEVICE_IS_NOT_CONNECTED
            )
            return
        }

        try {
            // Create the message
            val payload = Message.Builder()
                .setPayload(messageBytes)
                .build()

            log("Sending message to watch (${messageBytes.size} bytes)")

            p2pClient.send(connectedDevice, payload, object : SendCallback {
                override fun onSendProgress(progress: Long) {
                    if (config.verboseLogging) {
                        log("Send progress: $progress")
                    }
                }

                override fun onSendResult(code: Int) {
                    log("Message sending result: $code")
                    if (code == WearEngineErrorCode.ERROR_CODE_COMM_SUCCESS) {
                        log("Message sent successfully")
                        onSuccess()
                    } else {
                        log("Failed to send message, code: $code")
                        onError("Failed to send message", code)
                    }
                }
            })
        } catch (e: Exception) {
            logError("Error sending message to device", e)
            onError(getErrorMessage(e), getErrorCode(e))
        }
    }

    /**
     * Returns error message from an exception.
     */
    private fun getErrorMessage(
        e: Exception,
        defaultMessage: String = SOMETHING_WENT_WRONG
    ) = (if (e is WearEngineException) e.message else null) ?: defaultMessage

    private fun getErrorCode(
        e: Exception,
        defaultCode: Int = WearEngineErrorCode.ERROR_CODE_GENERIC
    ) = if (e is WearEngineException) e.errorCode else defaultCode

    /**
     * Logs a message if logging is enabled.
     */
    private fun log(message: String) {
        Log.d(config.logTag, message)
    }

    /**
     * Logs an error message with exception.
     */
    private fun logError(message: String, e: Exception? = null) {
        if (e != null) {
            Log.e(config.logTag, message, e)
        } else {
            Log.e(config.logTag, message)
        }
    }

    companion object {
        /** Default tag for logging */
        const val DEFAULT_LOG_TAG = "WearEngineHelper"

        /** Maximum message size in bytes (1KB) */
        const val MAX_MESSAGE_SIZE = 1024

        /** Default error message */
        const val SOMETHING_WENT_WRONG = "Something went wrong"
    }
}