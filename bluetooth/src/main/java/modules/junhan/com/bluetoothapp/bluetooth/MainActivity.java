package modules.junhan.com.bluetoothapp.bluetooth;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Button searchBtn;
    private Button connectBtn;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchBtn = (Button)findViewById(R.id.l_btn_search);
        connectBtn = (Button)findViewById(R.id.l_btn_connect);
        listView = (ListView)findViewById(R.id.l_list_1);

        ArrayList<String> arrayList = new ArrayList<String>();

        JBluetoothManager bluetoothManager = new JBluetoothManager(this,"00000000-0000-1000-8000-00805F9B34FB" );

        Handler mHandler = new Handler();



    }
}
