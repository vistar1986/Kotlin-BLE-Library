/*
 * Copyright (c) 2022, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.kotlin.ble.core.server.callback

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import no.nordicsemi.android.kotlin.ble.core.RealClientDevice
import no.nordicsemi.android.kotlin.ble.core.data.BleGattOperationStatus
import no.nordicsemi.android.kotlin.ble.core.data.BleGattPhy
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState
import no.nordicsemi.android.kotlin.ble.core.server.GattServerEvent
import no.nordicsemi.android.kotlin.ble.core.server.OnCharacteristicReadRequest
import no.nordicsemi.android.kotlin.ble.core.server.OnCharacteristicWriteRequest
import no.nordicsemi.android.kotlin.ble.core.server.OnClientConnectionStateChanged
import no.nordicsemi.android.kotlin.ble.core.server.OnDescriptorReadRequest
import no.nordicsemi.android.kotlin.ble.core.server.OnDescriptorWriteRequest
import no.nordicsemi.android.kotlin.ble.core.server.OnExecuteWrite
import no.nordicsemi.android.kotlin.ble.core.server.OnMtuChanged
import no.nordicsemi.android.kotlin.ble.core.server.OnNotificationSent
import no.nordicsemi.android.kotlin.ble.core.server.OnServerPhyRead
import no.nordicsemi.android.kotlin.ble.core.server.OnServerPhyUpdate
import no.nordicsemi.android.kotlin.ble.core.server.OnServiceAdded

internal class BleGattServerCallback : BluetoothGattServerCallback() {

    private val _event = MutableSharedFlow<GattServerEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val event = _event.asSharedFlow()

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        characteristic: BluetoothGattCharacteristic?
    ) {
        _event.tryEmit(OnCharacteristicReadRequest(RealClientDevice(device!!), requestId, offset, characteristic!!))
    }

    override fun onCharacteristicWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        _event.tryEmit(
            OnCharacteristicWriteRequest(
                RealClientDevice(device!!),
                requestId,
                characteristic!!,
                preparedWrite,
                responseNeeded,
                offset,
                value!!
            )
        )
    }

    override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        val operationStatus = BleGattOperationStatus.create(status)
        val state = GattConnectionState.create(newState)
        _event.tryEmit(OnClientConnectionStateChanged(RealClientDevice(device!!), operationStatus, state))
    }

    override fun onDescriptorReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        descriptor: BluetoothGattDescriptor?
    ) {
        _event.tryEmit(OnDescriptorReadRequest(RealClientDevice(device!!), requestId, offset, descriptor!!))
    }

    override fun onDescriptorWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        descriptor: BluetoothGattDescriptor?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        _event.tryEmit(
            OnDescriptorWriteRequest(
                RealClientDevice(device!!),
                requestId,
                descriptor!!,
                preparedWrite,
                responseNeeded,
                offset,
                value!!
            )
        )
    }

    override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
        _event.tryEmit(OnExecuteWrite(RealClientDevice(device!!), requestId, execute))
    }

    override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
        _event.tryEmit(OnMtuChanged(RealClientDevice(device!!), mtu))
    }

    override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
        _event.tryEmit(OnNotificationSent(RealClientDevice(device!!), BleGattOperationStatus.create(status)))
    }

    override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
        _event.tryEmit(
            OnServerPhyRead(
                RealClientDevice(device!!),
                BleGattPhy.create(txPhy),
                BleGattPhy.create(rxPhy),
                BleGattOperationStatus.create(status)
            )
        )
    }

    override fun onPhyUpdate(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
        _event.tryEmit(
            OnServerPhyUpdate(
                RealClientDevice(device!!),
                BleGattPhy.create(txPhy),
                BleGattPhy.create(rxPhy),
                BleGattOperationStatus.create(status)
            )
        )
    }

    override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
        _event.tryEmit(OnServiceAdded(service!!, BleGattOperationStatus.create(status)))
    }

    fun onEvent(event: GattServerEvent) {
        _event.tryEmit(event)
    }
}
