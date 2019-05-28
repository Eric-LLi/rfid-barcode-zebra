
package com.reactlibrary;

import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Callback;

public class RNRfidBarcodeZebraModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

	private final ReactApplicationContext reactContext;
	private RNRfidBarcodeZebraThread scannerthread = null;

	public RNRfidBarcodeZebraModule(ReactApplicationContext reactContext) {
		super(reactContext);
		this.reactContext = reactContext;
		this.reactContext.addLifecycleEventListener(this);

		this.scannerthread = new RNRfidBarcodeZebraThread(this.reactContext) {

			@Override
			public void dispatchEvent(String name, WritableMap data) {
				RNRfidBarcodeZebraModule.this.reactContext
						.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(name, data);
			}

			@Override
			public void dispatchEvent(String name, String data) {
				RNRfidBarcodeZebraModule.this.reactContext
						.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(name, data);
			}

			@Override
			public void dispatchEvent(String name, WritableArray data) {
				RNRfidBarcodeZebraModule.this.reactContext
						.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(name, data);
			}
		};
		scannerthread.start();

		Log.v("RFID", "RFIDScannerManager created");
	}

	@Override
	public String getName() {
		return "RNRfidBarcodeZebra";
	}

	@Override
	public void onHostResume() {
		if (this.scannerthread != null) {
			this.scannerthread.onHostResume();
		}
	}

	@Override
	public void onHostPause() {
		if (this.scannerthread != null) {
			this.scannerthread.onHostPause();
		}
	}

	@Override
	public void onHostDestroy() {
		if (this.scannerthread != null) {
			this.scannerthread.onHostDestroy();
		}
	}

	@Override
	public void onCatalystInstanceDestroy() {
		if (this.scannerthread != null) {
			this.scannerthread.onCatalystInstanceDestroy();
		}
	}

	@ReactMethod
	public void GetAvailableBluetoothDevices(Callback callback) {
		if (this.scannerthread != null) {
			callback.invoke(this.scannerthread.GetAvailableBluetoothDevices());
		}
	}

	@ReactMethod
	public void SaveSelectedScanner(String item) {
		if (this.scannerthread != null) {
			this.scannerthread.SaveSelectedScanner(item);
		}
	}

	@ReactMethod
	public void init() {
		if (this.scannerthread != null) {
			this.scannerthread.init(reactContext);
		}
	}

	@ReactMethod
	public void isConnected(Callback callback) {
		if (this.scannerthread != null) {
			callback.invoke(this.scannerthread.isConnected());
		}
	}

	@ReactMethod
	public void barcodeConnect() {
		if (this.scannerthread != null) {
			this.scannerthread.barcodeConnect();
		}
	}

	@ReactMethod
	public void barcodeDisconnect() {
		if (this.scannerthread != null) {
			this.scannerthread.barcodeDisconnect();
		}
	}

	@ReactMethod
	public void barcodePullTrigger() {
		if (this.scannerthread != null) {
			this.scannerthread.barcodePullTrigger();
		}
	}

	@ReactMethod
	public void barcodeReleaseTrigger() {
		if (this.scannerthread != null) {
			this.scannerthread.barcodeReleaseTrigger();
		}
	}

	@ReactMethod
	public void reconnect() {
		if (this.scannerthread != null) {
			this.scannerthread.reconnect();
		}
	}

	@ReactMethod
	public void switchDPO(boolean value, Callback callback) {
		if (this.scannerthread != null) {
			callback.invoke(this.scannerthread.switchDPO(value));
		}
	}

	@ReactMethod
	public void saveTagID(String tag) {
		if (this.scannerthread != null) {
			this.scannerthread.saveTagID(tag);
		}
	}

	@ReactMethod
	public void locateMode(boolean isLocateMode) {
		if (this.scannerthread != null) {
			this.scannerthread.locateMode(isLocateMode);
		}
	}

	@ReactMethod
	public void TagITMode(boolean isTagITMode) {
		if (this.scannerthread != null) {
			this.scannerthread.TagITMode(isTagITMode);
		}
	}

	@ReactMethod
	public void TagITReadBarcode(boolean isReadBarcode) {
		if (this.scannerthread != null) {
			this.scannerthread.TagITReadBarcode(isReadBarcode);
		}
	}

	@ReactMethod
	public void AuditMode(boolean isAuditMode) {
		if (this.scannerthread != null) {
			this.scannerthread.AuditMode(isAuditMode);
		}
	}

	@ReactMethod
	public void locateTag(String tag) {
		if (this.scannerthread != null) {
			this.scannerthread.LoopForLocateTag();
		}
	}

	@ReactMethod
	public void writeTag(String targetTag, String newTag, Callback callback) {
		if (this.scannerthread != null) {
			callback.invoke(this.scannerthread.writeTag(targetTag, newTag));
		}
	}

	@ReactMethod
	public void read(ReadableMap config) {
		if (this.scannerthread != null) {
			this.scannerthread.read(config);
		}
	}

	@ReactMethod
	public void cleanTags() {
		if (this.scannerthread != null) {
			this.scannerthread.cleanTags();
		}
	}

	@ReactMethod
	public void cancel() {
		if (this.scannerthread != null) {
			this.scannerthread.cancel();
		}
	}

	@ReactMethod
	public void shutdown() {
		if (this.scannerthread != null) {
			this.scannerthread.shutdown();
		}
	}

	@ReactMethod
	public void getConfig(Callback callback) {
		if (this.scannerthread != null) {
			callback.invoke(this.scannerthread.getConfig());
		}
	}

	@ReactMethod
	public void getAntennaConfig(Callback callback) {
		if (this.scannerthread != null) {
			callback.invoke(this.scannerthread.getAntennaConfig());
		}
	}

	@ReactMethod
	public void saveAntennaConfig(ReadableMap config, Callback callback) {
		if (this.scannerthread != null) {
			callback.invoke(this.scannerthread.saveAntennaConfig(config));
		}
	}

	@ReactMethod
	public void ChangeBeeperVolume(boolean value) {
		if (this.scannerthread != null) {
			this.scannerthread.ChangeBeeperVolume(value);
		}
	}

	@ReactMethod
	public void OpenAndroidSetting() {
		reactContext.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
	}
}