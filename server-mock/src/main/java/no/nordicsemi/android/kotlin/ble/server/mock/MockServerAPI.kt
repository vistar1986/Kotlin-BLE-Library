package no.nordicsemi.android.kotlin.ble.server.mock

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import no.nordicsemi.android.common.core.DataByteArray
import no.nordicsemi.android.kotlin.ble.core.ClientDevice
import no.nordicsemi.android.kotlin.ble.core.MockServerDevice
import no.nordicsemi.android.kotlin.ble.core.data.BleGattPhy
import no.nordicsemi.android.kotlin.ble.core.data.PhyOption
import no.nordicsemi.android.kotlin.ble.core.wrapper.IBluetoothGattCharacteristic
import no.nordicsemi.android.kotlin.ble.mock.MockEngine
import no.nordicsemi.android.kotlin.ble.server.api.GattServerAPI
import no.nordicsemi.android.kotlin.ble.server.api.ServerGattEvent

/**
 * A class for communication with [MockEngine]. It allows for sending responses to connected client
 * devices.
 *
 * @property mockEngine An instance of a [MockEngine].
 * @property serverDevice A server device.
 */
class MockServerAPI(
    private val mockEngine: MockEngine,
    private val serverDevice: MockServerDevice
) : GattServerAPI {

    //todo verify reply side-effects
    private val _event = MutableSharedFlow<ServerGattEvent>(replay = 10, extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val event: SharedFlow<ServerGattEvent> = _event.asSharedFlow()

    override fun onEvent(event: ServerGattEvent) {
        _event.tryEmit(event)
    }

    override fun sendResponse(device: ClientDevice, requestId: Int, status: Int, offset: Int, value: DataByteArray?) {
        mockEngine.sendResponse(device, requestId, status, offset, value)
    }

    override fun notifyCharacteristicChanged(
        device: ClientDevice,
        characteristic: IBluetoothGattCharacteristic,
        confirm: Boolean,
        value: DataByteArray
    ) {
        mockEngine.notifyCharacteristicChanged(device, characteristic, confirm, value)
    }

    override fun close() {
        mockEngine.unregisterServer(serverDevice)
    }

    override fun cancelConnection(device: ClientDevice) {
        mockEngine.cancelConnection(serverDevice, device)
    }

    override fun connect(device: ClientDevice, autoConnect: Boolean) {
        mockEngine.connect(device, autoConnect)
    }

    override fun readPhy(device: ClientDevice) {
        mockEngine.readPhy(device)
    }

    override fun requestPhy(device: ClientDevice, txPhy: BleGattPhy, rxPhy: BleGattPhy, phyOption: PhyOption) {
        mockEngine.requestPhy(device, txPhy, rxPhy, phyOption)
    }
}
