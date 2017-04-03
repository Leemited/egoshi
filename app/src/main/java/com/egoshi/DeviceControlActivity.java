/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.egoshi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tsengvn.typekit.Typekit;
import com.tsengvn.typekit.TypekitContextWrapper;


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    
    byte sendByte[] = new byte[20]; 
	
	Button nextBtn, /*sideBtn,*/ preBtn, onoff, reset;

    ImageView ani_img01,ani_img02,ani_img03,ani_img04,ani_img05,ani_img06,ani_img07,sub01,sub02,sub03,sub04,sub05,sub06;

    Animation animation;

	String Mode = "MODE";
    String SubMode = "SUB";
    int flag = 0;
    int cnt = 0;
    int cnts = 0;

    private Vibrator vibrator;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            	getDeviceSetting();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] sendByte = intent.getByteArrayExtra("init");

            	if((sendByte[0] == 0x55) && (sendByte[1] == 0x33)){
            		Log.d(TAG,"======= Init Setting Data ");
            		updateCommandState("Init Data");

                	Handler mHandler = new Handler();
                	mHandler.postDelayed(new Runnable() {

            			@Override
            			public void run() {
            		    	// notification enable
            		    	final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(3).get(1);
            		        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        }
                	   
                	}, 600);
               	}

	        	if((sendByte[0] == 0x55) && (sendByte[1] == 0x03)){
	        		Log.d(TAG,"======= SPP READ NOTIFY ");
            		updateCommandState("SPP READ");
	        		
	        		byte notifyValue = sendByte[2];
	        	}
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }

            //thread = new dataThread();
            //thread.start();
        }
    };

    private void getDeviceSetting(){
    	if(mGattCharacteristics != null){ 
    		final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(6).get(0);
    		mBluetoothLeService.readCharacteristic(characteristic);
    	}
    }
    
   
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }
    
    public static String bytesToHex(byte bytedata) {
        char[] hexChars = new char[2];

        int v = bytedata & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];

        return new String(hexChars);
    }

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        Log.d(TAG,"TEST");
                        Log.d(TAG, "Selected uuid:" + characteristic.getUuid().toString());
                        
                        //Log.d("BLE", "UUID selected: ".append(characteristic.))
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            
                            
                            Log.d(TAG, "KN Selected uuid:" + mNotifyCharacteristic.getUuid().toString());
                            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };

    private void clearUI() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);
        Typekit.getInstance().addNormal(Typekit.createFromAsset(this,"gulim.ttc")).addBold(Typekit.createFromAsset(this,"gulim.ttc"));

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("");
        actionBar.setLogo(getResources().getDrawable(R.drawable.actionbar_logo));
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_bg));
        animation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fadeout_ani);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        
        if(mDeviceName.equals("FBL770 v2.0.0"))
        {
        	mDeviceName = " BLE - UART";
        }

        ani_img01 = (ImageView) findViewById(R.id.ani_img0);
        ani_img02 = (ImageView) findViewById(R.id.ani_img1);
        ani_img03 = (ImageView) findViewById(R.id.ani_img2);
        ani_img04 = (ImageView) findViewById(R.id.ani_img3);
        ani_img05 = (ImageView) findViewById(R.id.ani_img4);
        ani_img06 = (ImageView) findViewById(R.id.ani_img5);
        ani_img07 = (ImageView) findViewById(R.id.ani_img6);
        sub01 = (ImageView) findViewById(R.id.sub01);
        sub02 = (ImageView) findViewById(R.id.sub02);
        sub03 = (ImageView) findViewById(R.id.sub03);
        sub04 = (ImageView) findViewById(R.id.sub04);
        sub05 = (ImageView) findViewById(R.id.sub05);
        sub06 = (ImageView) findViewById(R.id.sub06);

        nextBtn = (Button) this.findViewById(R.id.nextBtn);
        nextBtn.setOnClickListener(new ClickEvent());
		//btnSend.setEnabled(true);

        preBtn = (Button) this.findViewById(R.id.preBtn);
        preBtn.setOnClickListener(new ClickEvent());

        /*sideBtn = (Button) findViewById(R.id.sideBtn);
        sideBtn.setOnClickListener(new ClickEvent());*/

        reset = (Button) findViewById(R.id.refresh);
        reset.setOnClickListener(new ClickEvent());

        onoff = (Button) this.findViewById(R.id.onoff);
        onoff.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(!mConnected) return false;
                vibrator.vibrate(1000);

                if(flag==1){
                    try {
                        sendData("BOFF");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    ani_img01.setVisibility(View.GONE);
                    ani_img02.setVisibility(View.GONE);
                    ani_img03.setVisibility(View.GONE);
                    ani_img04.setVisibility(View.GONE);
                    ani_img05.setVisibility(View.GONE);
                    ani_img06.setVisibility(View.GONE);
                    ani_img07.setVisibility(View.GONE);
                    sub01.setVisibility(View.GONE);
                    sub02.setVisibility(View.GONE);
                    sub03.setVisibility(View.GONE);
                    sub04.setVisibility(View.GONE);
                    sub05.setVisibility(View.GONE);
                    sub06.setVisibility(View.GONE);
                    onoff.setBackgroundResource(R.drawable.btn_off);
                    flag=0;
                    cnt=6;
                }else if(flag==0){
                    ani_img01.setVisibility(View.VISIBLE);
                    ani_img02.setVisibility(View.VISIBLE);
                    ani_img02.startAnimation(animation);
                    onoff.setBackgroundResource(R.drawable.btn_on);
                    try {
                        sendData("BON");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    flag=1;
                    cnt=0;
                }
                return true;
            }
        });

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        if(mConnected){
            sendData("MODES");
        }else{
            if(mBluetoothLeService!=null) {
                mBluetoothLeService.connect(mDeviceAddress);
            }else{
                Log.d(TAG, "onCreate: null");
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.infor:
                Intent intent = new Intent(this,InformationActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {

    }

    private void updateCommandState(final String str) {
      runOnUiThread(new Runnable() {
          @Override
          public void run() {
              //mConnectionState.setText(str);
              Log.d(TAG, "run: updata?" + str);
          }
      });
    }
    
    private void displayData(String data) {
        if (data != null) {
            String[] datas = data.split(" ");
            hiddenImg();
            if(data.length() < 60) {
                flag=1;
                if (datas[4].equals("30")) {
                    cnt = 0;
                    ani_img02.setVisibility(View.VISIBLE);
                    onoff.setBackgroundResource(R.drawable.btn_on);
                } else if (datas[4].equals("31")) {
                    cnt = 1;
                    ani_img05.setVisibility(View.VISIBLE);
                    onoff.setBackgroundResource(R.drawable.btn_on);
                } else if (datas[4].equals("32")) {
                    cnt = 2;
                    ani_img04.setVisibility(View.VISIBLE);
                    onoff.setBackgroundResource(R.drawable.btn_on);
                } else if (datas[4].equals("33")) {
                    cnt = 3;
                    ani_img03.setVisibility(View.VISIBLE);
                    onoff.setBackgroundResource(R.drawable.btn_on);
                } else if (datas[4].equals("34")) {
                    cnt = 4;
                    ani_img06.setVisibility(View.VISIBLE);
                    onoff.setBackgroundResource(R.drawable.btn_on);
                } else if (datas[4].equals("35")) {
                    cnt = 5;
                    ani_img07.setVisibility(View.VISIBLE);
                    onoff.setBackgroundResource(R.drawable.btn_on);
                } else if (datas[4].equals("36")) {
                    flag=0;
                    onoff.setBackgroundResource(R.drawable.btn_off);
                }
            }else{
                sendData("MODES");
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                Log.d(TAG,"gattCharacteristic uuid : " + uuid);
                
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }
    
    
	class ClickEvent implements View.OnClickListener {
		@Override
		public void onClick(View v) {
            ani_img02.clearAnimation();
            ani_img03.clearAnimation();
            ani_img04.clearAnimation();
            ani_img05.clearAnimation();
            ani_img06.clearAnimation();
            ani_img07.clearAnimation();
            sub01.clearAnimation();
            sub02.clearAnimation();
            sub03.clearAnimation();
            sub04.clearAnimation();
            sub05.clearAnimation();
            sub06.clearAnimation();
            if (v == nextBtn) {
				if(!mConnected) return;

                if(flag==1) {
                    ani_img01.setVisibility(View.VISIBLE);
                    vibrator.vibrate(300);
                    if(cnt < 5 && cnt >= 0){
                        cnt++;
                    }else if(cnt >= 5){
                        cnt = 0;
                    }
                    sendData(Mode + cnt);

                    showImg(cnt);
                }
            }
            if (v == preBtn) {
                if(!mConnected) return;

                if(flag==1) {
                    /*ani_img01.setVisibility(View.VISIBLE);
                    vibrator.vibrate(300);
                    if(cnt <= 6 && cnt > 0){
                        cnt--;
                    }else if(cnt <= 0){
                        cnt = 5;
                    }

                    sendData(Mode + cnt);

                    showImg(cnt);*/
                    ani_img01.setVisibility(View.VISIBLE);
                    vibrator.vibrate(300);
                    if(cnts < 7){
                        cnts++;
                    }else if(cnts > 0 && cnts < 8){
                        cnts = 0;
                    }
                    sendData(SubMode + cnts);
                    showImgs(cnts);

                }
            }
           /* if (v == sideBtn) {
                if(!mConnected) return;

                if(flag==1) {
                    ani_img01.setVisibility(View.VISIBLE);
                    vibrator.vibrate(300);
                    if(cnts < 7){
                        cnts++;
                    }else if(cnts > 0 && cnts < 8){
                        cnts = 0;
                    }
                    sendData(SubMode + cnts);
                    showImgs(cnts);
                }
            }*/
            if (v == reset) {
                if(!mConnected) return;

                if(mConnected) {
                    //if(flag==1) {
                        sendData("MODES");
                        Handler handle = new Handler();
                        handle.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sendData("SUBS");
                            }
                        }, 600);
                    //}
                }
            }
        }
	}    
    

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
    
    
    private void sendData(String data){
        if(mGattCharacteristics != null){
    	    final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(3).get(0);
    		mBluetoothLeService.writeCharacteristics(characteristic, data);
    	}    	
    }

    public void hiddenImg(){
        ani_img02.setVisibility(View.GONE);
        ani_img03.setVisibility(View.GONE);
        ani_img04.setVisibility(View.GONE);
        ani_img05.setVisibility(View.GONE);
        ani_img06.setVisibility(View.GONE);
        ani_img07.setVisibility(View.GONE);
        sub01.setVisibility(View.GONE);
        sub02.setVisibility(View.GONE);
        sub03.setVisibility(View.GONE);
        sub04.setVisibility(View.GONE);
        sub05.setVisibility(View.GONE);
        sub06.setVisibility(View.GONE);
    }
    public void showImg(int cnt){
        if (cnt == 1) {
            ani_img02.setVisibility(View.GONE);
            ani_img05.setVisibility(View.VISIBLE);
            ani_img04.setVisibility(View.GONE);
            ani_img05.startAnimation(animation);
        } else if (cnt == 2) {
            ani_img05.setVisibility(View.GONE);
            ani_img04.setVisibility(View.VISIBLE);
            ani_img03.setVisibility(View.GONE);
            ani_img04.startAnimation(animation);
        } else if (cnt == 3) {
            ani_img04.setVisibility(View.GONE);
            ani_img03.setVisibility(View.VISIBLE);
            ani_img06.setVisibility(View.GONE);
            ani_img03.startAnimation(animation);
        } else if (cnt == 4) {
            ani_img03.setVisibility(View.GONE);
            ani_img06.setVisibility(View.VISIBLE);
            ani_img07.setVisibility(View.GONE);
            ani_img06.startAnimation(animation);
        } else if (cnt == 5) {
            ani_img06.setVisibility(View.GONE);
            ani_img07.setVisibility(View.VISIBLE);
            ani_img02.setVisibility(View.GONE);
            ani_img07.startAnimation(animation);
        } else if (cnt == 0) {
            ani_img07.setVisibility(View.GONE);
            ani_img02.setVisibility(View.VISIBLE);
            ani_img05.setVisibility(View.GONE);
            ani_img02.startAnimation(animation);
        }
    }
    public void showImgs(int cnts){
        if (cnts == 1) {
            ani_img01.setVisibility(View.GONE);
            sub01.setVisibility(View.VISIBLE);
            sub01.startAnimation(animation);
        } else if (cnts == 2) {
            sub01.setVisibility(View.GONE);
            sub02.setVisibility(View.VISIBLE);
            sub02.startAnimation(animation);
        } else if (cnts == 3) {
            sub02.setVisibility(View.GONE);
            sub03.setVisibility(View.VISIBLE);
            sub03.startAnimation(animation);
        } else if (cnts == 4) {
            sub03.setVisibility(View.GONE);
            sub04.setVisibility(View.VISIBLE);
            sub04.startAnimation(animation);
        } else if (cnts == 5) {
            sub04.setVisibility(View.GONE);
            sub05.setVisibility(View.VISIBLE);
            sub05.startAnimation(animation);
        } else if (cnts == 6) {
            sub05.setVisibility(View.GONE);
            sub06.setVisibility(View.VISIBLE);
            sub06.startAnimation(animation);
        } else if (cnts == 7) {
            sub06.setVisibility(View.GONE);
            ani_img01.setVisibility(View.GONE);
        }
    }
}
