package modules.junhan.com.bluetoothapp.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

public class JBluetoothManager {

    private String UUID = "00000000-0000-1000-8000-00805F9B34FB";
    private BluetoothAdapter mBluetoothAdapter = null;
    private Activity activity;
    private BroadcastReceiver mReceiver = null;
    private IntentFilter filter = null;
    private ConnectedThread connectedThread = null;
    private BluetoothSocket mmSocket;

    public final int MESSAGE_READ = 1;
    public final int REQUEST_ENABLE_BT = 101;

    public JBluetoothManager(Activity activity, String UUID) {
        this.activity = activity;
        this.UUID = UUID;
        initializeBluetooth();
    }

    /*
    function: initializeBluetooth
    return: void
    remark: 블루투스 어댑터를 반환받는 초기화 함수
    date: 2018-05-22
    by: JH
     */
    public void initializeBluetooth(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.e("error", "bluetooth is not supported.");
        }
    }

    /*
    function: enableBluetooth
    return: bool
    remark: 블루투스가 활성화 상태이면 false를 반환, 활성화 상태가 아니면 활성화 request를 보낸 후 true 반환
    date: 2018-05-22
    by: JH
     */
    public boolean enableBluetooth(){
        boolean res = false;
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            res = true;
        }
        else
        {
            res = false;
        }
        return res;
    }

    /*
    function: getPairedDevices
    return: Set<BluetoothDevice>
    remark: 페어링된 디바이스 반환, 만약 페어링된 디바이스가 없다면 null을 반환
    date: 2018-05-22
    by: JH
     */
    public Set<BluetoothDevice> getPairedDevices()
    {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();

        if(devices.size() > 0)
            return devices;
        else
            return null;
    }

    /*
    function: registerDeviceScanner
    return: void
    remark: 주변 디바이스 검색시 사용할 BroadcastReceiver 등록
    date: 2018-05-22
    by: JH
     */
    public void registerDeviceScanner(final JBluetoothDeviceFoundListener listener)
    {
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    listener.update(device);
                }
            }
        };

        // Register the BroadcastReceiver
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    /*
    function: unregisterDeviceScanner
    return: void
    remark: BroadcastReceiver 해제
    date: 2018-05-22
    by: JH
     */
    public void unregisterDeviceScanner()
    {
        if(mReceiver != null && filter != null)
            activity.registerReceiver(mReceiver, filter);
        else
            return;
    }

    /*
    function: discoveryDevices
    return: void
    remark: 주변 기기 검색 시작. 12초정도 유지.
    date: 2018-05-22
    by: JH
     */
    public void discoveryDevices()
    {
        if(mReceiver == null || filter == null){
            Log.e("ERROR","register not found");
            return;
        }
        else
            mBluetoothAdapter.startDiscovery();
    }

    /*
    function: stopDiscovery
    return: void
    remark: 주변기기를 검색중이라면 검색 중지.
    date: 2018-05-22
    by: JH
     */
    public void stopDiscovery()
    {
        if(mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
    }

    public void connectDevice(String address, Handler mHandler)
    {
        stopDiscovery();
        if(isConnected()){
           Log.e("ERROR","already connected.");
           return;
        }
        else
        {
            ConnectThread connectThread = new ConnectThread(mBluetoothAdapter.getRemoteDevice(address), UUID, mHandler);
            connectThread.start();
        }
    }

    public boolean isConnected()
    {
        boolean res = false;
        if(mmSocket == null)
        {
            res = false;
        }
        else
        {
            if(mmSocket.isConnected() && connectedThread != null)
                res = true;
            else
                res = false;
        }

        return res;
    }

    public void disconnectDevice()
    {
        if(isConnected())
            connectedThread.cancel();
    }

    public void write(final int parm)
    {
        if(isConnected())
        {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putInt(parm);
            connectedThread.write(bb.array());
        }
    }

    public void write(final float parm)
    {
        if(isConnected())
        {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putFloat(parm);
            connectedThread.write(bb.array());
        }
    }

    public void write(final char parm)
    {
        if(isConnected())
        {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putChar(parm);
            connectedThread.write(bb.array());
        }
    }

    private class ConnectThread extends Thread {

        private BluetoothDevice mmDevice;
        private Handler mHandler = null;

        public ConnectThread(BluetoothDevice device, final String UUID, Handler mHandler) {
            this.mmDevice = device;
            this.mHandler = mHandler;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                mmSocket = device.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID));
            } catch (IOException e) { }
        }

        public void run() {

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            connectedThread = new ConnectedThread(mmSocket, mHandler);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private Handler mHandler = null;


        public ConnectedThread(BluetoothSocket socket, Handler mHandler) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            this.mHandler = mHandler;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
