package com.redbear.simplecontrols;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class SimpleControls extends Activity {
	private final static String TAG = SimpleControls.class.getSimpleName();

	private Button connectBtn = null;
	private TextView rssiValue = null;
	private TextView AnalogInValue = null;
	private ToggleButton digitalOutBtn, digitalInBtn, AnalogInBtn;
	private SeekBar servoSeekBar, PWMSeekBar;

	private BluetoothGattCharacteristic characteristicTx = null;
	private RBLService mBluetoothLeService;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mDevice = null;
	private String mDeviceAddress;

	private boolean flag = true;
	private boolean connState = false;
	private boolean scanFlag = false;

	private byte[] data = new byte[3];
	private static final int REQUEST_ENABLE_BT = 1;
	private static final long SCAN_PERIOD = 2000;

	final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    EditText accelXTextView;
    EditText accelYTextView;
    EditText accelZTextView;
    EditText curCadenceTextView;
    TextView avgCadenceTextView;
    TextView distTextView;
    TextView curSpeedTextView;
    TextView avgSpeedTextView;
    TextView avgSpeedUnitTextView;
    TextView curSpeedUnitTextView;
    TextView distUnitTextView;
    boolean empirical = false;
    float curSpeedX = 0;
    float curSpeedY = 0;
    float curSpeedZ = 0;
    float curSpeed = 0;
    float oldSpeed = 0;
    float avgSpeed = 0;
    float avgCadence = 0;
    float distTraveled = 0;
    int updates = 1; // number of times data has been updated, used for averaging.
    float step = 1; // time between each measurement in seconds
    float kmToMi = (float) 0.621371;
    float msTokmh = (float) 3.6;

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((RBLService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
				Toast.makeText(getApplicationContext(), "Disconnected",
						Toast.LENGTH_SHORT).show();
				setButtonDisable();
			} else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				Toast.makeText(getApplicationContext(), "Connected",
						Toast.LENGTH_SHORT).show();

				getGattService(mBluetoothLeService.getSupportedGattService());
			} else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
				data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);

			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);

        accelXTextView = (EditText) findViewById(R.id.accelXTextView);
        accelYTextView = (EditText) findViewById(R.id.accelYTextView);
        accelZTextView = (EditText) findViewById(R.id.accelZTextView);
        curCadenceTextView = (EditText) findViewById(R.id.curCadenceTextView);
        avgCadenceTextView = (TextView) findViewById(R.id.avgCadenceTextView);
        curSpeedTextView = (TextView) findViewById(R.id.curSpeedTextView);
        avgSpeedTextView = (TextView) findViewById(R.id.avgSpeedTextView);
        distTextView = (TextView) findViewById(R.id.distTextView);
        curSpeedUnitTextView = (TextView) findViewById(R.id.curSpeedUnitTextView);
        avgSpeedUnitTextView = (TextView) findViewById(R.id.avgSpeedUnitTextView);
        distUnitTextView = (TextView) findViewById(R.id.distUnitTextView);
        Button distBtn = (Button) findViewById(R.id.distBtn);
        Button reset = (Button) findViewById(R.id.reset);
        CheckBox units = (CheckBox) findViewById(R.id.units);

		connectBtn = (Button) findViewById(R.id.connect);
		connectBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (scanFlag == false) {
					scanLeDevice();

					Timer mTimer = new Timer();
					mTimer.schedule(new TimerTask() {

						@Override
						public void run() {
							if (mDevice != null) {
								mDeviceAddress = mDevice.getAddress();
								mBluetoothLeService.connect(mDeviceAddress);
								scanFlag = true;
							} else {
								runOnUiThread(new Runnable() {
									public void run() {
										Toast toast = Toast
												.makeText(
														SimpleControls.this,
														"Couldn't search Ble Shiled device!",
														Toast.LENGTH_SHORT);
										toast.setGravity(0, 0, Gravity.CENTER);
										toast.show();
									}
								});
							}
						}
					}, SCAN_PERIOD);
				}

				System.out.println(connState);
				if (connState == false) {
					mBluetoothLeService.connect(mDeviceAddress);
				} else {
					mBluetoothLeService.disconnect();
					mBluetoothLeService.close();
					setButtonDisable();
				}
			}
		});



		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
					.show();
			finish();
			return;
		}

		Intent gattServiceIntent = new Intent(SimpleControls.this,
				RBLService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        units.setOnClickListener(checkboxClickListener);
        distBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                float accelerationX = Float.parseFloat(accelXTextView.getText().toString());
                float accelerationY = Float.parseFloat(accelYTextView.getText().toString());
                float accelerationZ = Float.parseFloat(accelZTextView.getText().toString());
                float curCadence = Float.parseFloat(curCadenceTextView.getText().toString());
                curSpeedX += accelerationX*step; //in m/s
                curSpeedY += accelerationY*step;
                curSpeedZ += accelerationZ*step;
                oldSpeed = curSpeed;
                curSpeed = (float) Math.sqrt(Math.pow(curSpeedX,2) + Math.pow(curSpeedY, 2) + Math.pow(curSpeedZ, 2))*msTokmh; // in kmh
                distTraveled += (((oldSpeed + curSpeed)/2)*step)/3600; //speed in kmh, step in sec so convert (WILL CHANGE)
                avgSpeed = (avgSpeed*updates+curSpeed)/(updates+1);
                avgCadence = (avgCadence*updates+curCadence)/(updates+1);
                avgCadenceTextView.setText(Float.toString(avgCadence));
                if (!empirical) {
                    curSpeedTextView.setText(Float.toString(curSpeed));
                    avgSpeedTextView.setText(Float.toString(avgSpeed));
                    distTextView.setText(Float.toString(distTraveled));
                } else {
                    curSpeedTextView.setText(Float.toString(curSpeed*kmToMi));
                    avgSpeedTextView.setText(Float.toString(avgSpeed*kmToMi));
                    distTextView.setText(Float.toString(distTraveled*kmToMi));
                }
                updates += 1;
            }
        });
        reset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                avgSpeed = 0;
                avgSpeedTextView.setText(Float.toString(avgSpeed));
                distTraveled = 0;
                distTextView.setText(Float.toString(distTraveled));
                avgCadence = 0;
                avgCadenceTextView.setText(Float.toString(avgCadence));
                updates = 1;
            }
        });
	}
	View.OnClickListener checkboxClickListener = new View.OnClickListener() {
		@Override
        public void onClick(View view) {
            boolean checked = ((CheckBox) view).isChecked();
            if(checked){
                curSpeedUnitTextView.setText("mi/hr");
                curSpeedTextView.setText(Float.toString(curSpeed*kmToMi));
                avgSpeedUnitTextView.setText("mi/hr");
                avgSpeedTextView.setText(Float.toString(avgSpeed*kmToMi));
                distTextView.setText(Float.toString(distTraveled*kmToMi));
                distUnitTextView.setText("mi");
                empirical = true;
            }
            else {
                curSpeedUnitTextView.setText("km/hr");
                curSpeedTextView.setText(Float.toString(curSpeed));
                avgSpeedUnitTextView.setText("km/hr");
                avgSpeedTextView.setText(Float.toString(avgSpeed));
                distTextView.setText(Float.toString(distTraveled));
                distUnitTextView.setText("km");
                empirical = false;
            }
        }
	};
	protected void onResume() {
		super.onResume();

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	private void setButtonEnable() {
		flag = true;
		connState = true;

		digitalOutBtn.setEnabled(flag);
		AnalogInBtn.setEnabled(flag);
		servoSeekBar.setEnabled(flag);
		PWMSeekBar.setEnabled(flag);
		connectBtn.setText("Disconnect");
	}

	private void setButtonDisable() {
		flag = false;
		connState = false;

		digitalOutBtn.setEnabled(flag);
		AnalogInBtn.setEnabled(flag);
		servoSeekBar.setEnabled(flag);
		PWMSeekBar.setEnabled(flag);
		connectBtn.setText("Connect");
	}

	private void startReadRssi() {
		new Thread() {
			public void run() {

				while (flag) {
					mBluetoothLeService.readRssi();
					try {
						sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	private void getGattService(BluetoothGattService gattService) {
		if (gattService == null)
			return;

		setButtonEnable();
		startReadRssi();

		characteristicTx = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

		BluetoothGattCharacteristic characteristicRx = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
		mBluetoothLeService.setCharacteristicNotification(characteristicRx,
				true);
		mBluetoothLeService.readCharacteristic(characteristicRx);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

		return intentFilter;
	}

	private void scanLeDevice() {
		new Thread() {

			@Override
			public void run() {
				mBluetoothAdapter.startLeScan(mLeScanCallback);

				try {
					Thread.sleep(SCAN_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				mBluetoothAdapter.stopLeScan(mLeScanCallback);
			}
		}.start();
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				final byte[] scanRecord) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					byte[] serviceUuidBytes = new byte[16];
					String serviceUuid = "";
					for (int i = 32, j = 0; i >= 17; i--, j++) {
						serviceUuidBytes[j] = scanRecord[i];
					}
					serviceUuid = bytesToHex(serviceUuidBytes);
					if (stringToUuidString(serviceUuid).equals(
							RBLGattAttributes.BLE_SHIELD_SERVICE
									.toUpperCase(Locale.ENGLISH))) {
						mDevice = device;
					}
				}
			});
		}
	};

	private String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	private String stringToUuidString(String uuid) {
		StringBuffer newString = new StringBuffer();
		newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
		newString.append("-");
		newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
		newString.append("-");
		newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
		newString.append("-");
		newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
		newString.append("-");
		newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

		return newString.toString();
	}

	@Override
	protected void onStop() {
		super.onStop();

		flag = false;

		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mServiceConnection != null)
			unbindService(mServiceConnection);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}
}
