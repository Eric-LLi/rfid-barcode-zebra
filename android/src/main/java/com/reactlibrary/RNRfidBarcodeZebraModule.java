
package com.reactlibrary;

import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.LifecycleEventListener;

public class RNRfidBarcodeZebraModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

	private final ReactApplicationContext reactContext;
	private RNRfidBarcodeZebraThread scannerthread = null;

	public RNRfidBarcodeZebraModule(ReactApplicationContext reactContext) {
		super(reactContext);
		this.reactContext = reactContext;
		this.reactContext.addLifecycleEventListener(this);

		if (this.scannerthread == null) {
			InitialThread();
		}
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
	public void InitialThread() {
		if (this.scannerthread != null) {
			this.scannerthread.interrupt();
		}
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

	}

	@ReactMethod
	public void GetAvailableBluetoothDevices(Promise promise) {
		if (this.scannerthread != null) {
			try {
				WritableArray array = this.scannerthread.GetAvailableBluetoothDevices();
				promise.resolve(array);
			} catch (Exception err) {
				promise.reject(err);
			}
		}
	}

	@ReactMethod
	public void SaveSelectedScanner(String item) {
		if (this.scannerthread != null) {
			this.scannerthread.SaveSelectedScanner(item);
		}
	}

	@ReactMethod
	public void init(Promise promise) {
		try {
			if (this.scannerthread != null) {
				this.scannerthread.init(reactContext);
				promise.resolve("");
			}
		} catch (Exception err) {
			promise.reject(err);
		}

	}

	@ReactMethod
	public void isConnected(Promise promise) {
		if (this.scannerthread != null) {
			promise.resolve(this.scannerthread.isConnected());
		}
	}

	@ReactMethod
	public void AttemptToReconnect(Promise promise) {
		try {
			if (this.scannerthread != null) {
				promise.resolve(this.scannerthread.AttemptToReconnect());
			}
		} catch (Exception err) {
			promise.reject(err);
		}

	}

	@ReactMethod
	public void barcodeConnect(Promise promise) {
		try {
			if (this.scannerthread != null) {
				this.scannerthread.barcodeConnect();
				promise.resolve("barcodeConnect");
			}
		} catch (Exception err) {
			promise.reject(err);
		}

	}

	@ReactMethod
	public void barcodeDisconnect() {
		if (this.scannerthread != null) {
			this.scannerthread.barcodeDisconnect();
		}
	}

	@ReactMethod
	public void SaveCurrentRoute(String value, Promise promise) {
		try {
			if (this.scannerthread != null) {
				this.scannerthread.SaveCurrentRoute(value);
			}
			promise.resolve("Done");
		} catch (Exception err) {
			promise.reject(err);
		}
	}

	@ReactMethod
	public void GetConnectedReader(Promise promise) {
		try {
			if (this.scannerthread != null) {
				promise.resolve(this.scannerthread.GetConnectedReader());
			}
		} catch (Exception err) {
			promise.reject(err);
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
	public void reconnect(Promise promise) {
		try {
			if (this.scannerthread != null) {
				this.scannerthread.reconnect();
				promise.resolve("");
			}
		} catch (Exception err) {
			promise.reject(err);
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
	public void TagITReadBarcode(boolean isReadBarcode, Promise promise) {
		if (this.scannerthread != null) {
			this.scannerthread.TagITReadBarcode(isReadBarcode);
			promise.resolve("Done");
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
	public void writeTag(String targetTag, String newTag, Promise promise) {
		try {
			if (this.scannerthread != null) {
				promise.resolve(this.scannerthread.writeTag(targetTag, newTag));
			}
		} catch (Exception err) {
			promise.reject(err);
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
	public void saveAntennaConfig(ReadableMap config, Promise promise) {
		if (this.scannerthread != null) {
			try {
				promise.resolve(this.scannerthread.saveAntennaConfig(config));
			} catch (Exception err) {
				promise.reject(err);
			}
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