package no.nordicsemi.android.kotlin.ble.app.mock.screen.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel
import no.nordicsemi.android.kotlin.ble.app.mock.BlinkyDestinationId
import no.nordicsemi.android.kotlin.ble.app.mock.screen.repository.BlinkyButtonParser
import no.nordicsemi.android.kotlin.ble.app.mock.screen.repository.BlinkyLedParser
import no.nordicsemi.android.kotlin.ble.app.mock.screen.view.BlinkyViewState
import no.nordicsemi.android.kotlin.ble.core.ServerDevice
import no.nordicsemi.android.kotlin.ble.core.client.service.BleGattCharacteristic
import no.nordicsemi.android.kotlin.ble.core.client.service.BleGattServices
import java.util.*
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class BlinkyViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val navigator: Navigator,
    private val savedStateHandle: SavedStateHandle
) : SimpleNavigationViewModel(navigator, savedStateHandle) {

    private val _device = MutableStateFlow<ServerDevice?>(null)
    val device = _device.asStateFlow()

    private val _state = MutableStateFlow(BlinkyViewState())
    val state = _state.asStateFlow()

    init {
        val blinkyDevice = parameterOf(BlinkyDestinationId)
        _device.value = blinkyDevice
        startGattClient(blinkyDevice)
    }

    private lateinit var ledCharacteristic: BleGattCharacteristic
    private lateinit var buttonCharacteristic: BleGattCharacteristic

    private fun startGattClient(blinkyDevice: ServerDevice) = viewModelScope.launch {
        //Connect a Bluetooth LE device.
        val client = blinkyDevice.connect(context)

        //Discover services on the Bluetooth LE Device.
        client.services
            .filterNotNull()
            .onEach { configureGatt(it) }
            .launchIn(viewModelScope)
    }

    private suspend fun configureGatt(services: BleGattServices) {
        //Remember needed service and characteristics which are used to communicate with the DK.
        val service = services.findService(BlinkySpecifications.UUID_SERVICE_DEVICE)!!
        ledCharacteristic = service.findCharacteristic(BlinkySpecifications.UUID_LED_CHAR)!!
        buttonCharacteristic = service.findCharacteristic(BlinkySpecifications.UUID_BUTTON_CHAR)!!

        //Observe button characteristics which detects when button is pressed.
        buttonCharacteristic.notification.onEach {
            _state.value = _state.value.copy(isButtonPressed = BlinkyButtonParser.isButtonPressed(it))
        }.launchIn(viewModelScope)

        //Enables notifications on DK.
        buttonCharacteristic.enableNotifications()

        //Check initial state of the Led.
        val isLedOn = BlinkyLedParser.isLedOn(ledCharacteristic.read())
        _state.value = _state.value.copy(isLedOn = isLedOn)
    }

    @SuppressLint("NewApi")
    fun turnLed() {
        viewModelScope.launch {
            if (state.value.isLedOn) {
                _state.value = _state.value.copy(isLedOn = false)
                ledCharacteristic.write(byteArrayOf(0x00))
            } else {
                _state.value = _state.value.copy(isLedOn = true)
                ledCharacteristic.write(byteArrayOf(0x01))
            }
        }
    }
}
