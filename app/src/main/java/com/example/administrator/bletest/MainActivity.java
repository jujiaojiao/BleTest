package com.example.administrator.bletest;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SCAN_TIME= 1000;
    private List<BluetoothDevice> list;
    private ListAdapter adapter;
    private  BluetoothReceiver blueRe;
    private  Handler handler = new Handler();
    BluetoothAdapter mBluetoothAdapter;
//    private BluetoothAdapter.LeScanCallback lecallback  = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            Log.i("JJJ=============", "onLeScan: "+device.getAddress());
//        }
//    };
    private Button btn;
    private static ListView listview;
    private TextView connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        checkBlePermission();//检查蓝牙权限
        isSupportBle();//判断是否支持ble,若支持，打开蓝牙
    }

    private void initView(){
        btn = ((Button) findViewById(R.id.btn));
        listview = ((ListView) findViewById(R.id.listview));
        connect = ((TextView) findViewById(R.id.connect));
        list = new ArrayList<>();
        btn.setOnClickListener(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//获取蓝牙适配器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        blueRe = new BluetoothReceiver();
        registerReceiver(blueRe, intentFilter);
    }
    /**
     *  扫描设备
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanDevice(){
        if (mBluetoothAdapter.isEnabled()){
           //可见进行扫描，10秒后停止扫描,用Handler进行延时
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.cancelDiscovery();
                }
            },SCAN_TIME);
            mBluetoothAdapter.startDiscovery();
        }else {
            //不可见，直接停止扫描
            mBluetoothAdapter.cancelDiscovery();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                Toast.makeText(this, "蓝牙已启用", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, "蓝牙未启用", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * 判断设备是否支持蓝牙ble
     * @return
     */
    private boolean isSupportBle(){
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持ble", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            return true;
        }
    }

    /**
     * 检查蓝牙权限
     */
    public void checkBlePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        } else {
            Log.i("tag","已申请权限");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // 如果请求被取消，则结果数组为空。
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("tag","同意申请");
                } else {
                    Log.i("tag","拒绝申请");
                }
                return;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    list.clear();
                    scanDevice();
                }
                break;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blueRe);     //注销广播接收器
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            //如果想要取消已经配对的设备，只需要将creatBond改为removeBond
            Method method = BluetoothDevice.class.getMethod("createBond");
            Log.e(getPackageName(), "开始配对");
            BluetoothDevice bluetoothDevice = list.get(position);

            method.invoke(bluetoothDevice);
            switch (bluetoothDevice.getBondState()) {
                case BluetoothDevice.BOND_NONE:
                    Toast.makeText(this, "取消配对", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothDevice.BOND_BONDING:
                    Toast.makeText(this, "配对中", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothDevice.BOND_BONDED:
                    Toast.makeText(this, "配对成功", Toast.LENGTH_SHORT).show();
                    list.remove(list.get(position).getName());
                    connect.setVisibility(View.VISIBLE);
                    connect.setText(list.get(position).getName());
                    adapter.notifyDataSetChanged();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e("JJJ=======", "str========: "+device.getName());
                list.add(device);
                adapter = new ListAdapter(list,context);
                listview.setAdapter(adapter);
                listview.setOnItemClickListener(MainActivity.this);
            }
        }
    }
}
