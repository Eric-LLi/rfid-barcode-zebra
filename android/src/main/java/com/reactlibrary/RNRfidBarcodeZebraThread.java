package com.reactlibrary;

import android.os.AsyncTask;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.lang.NullPointerException;
import java.util.HashMap;

import com.zebra.rfid.api3.*;
import com.zebra.scannercontrol.*;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

public abstract class RNRfidBarcodeZebraThread extends Thread implements RfidEventsListener, IDcsSdkApiDelegate {

	private ReactApplicationContext context;

	// RFID
	private Readers readers = null;
	private ArrayList<ReaderDevice> deviceList = null;
	private ReaderDevice rfidReaderDevice = null;
	boolean tempDisconnected = false;
	private Boolean reading = false;
	private ReadableMap config = null;

	// Save scanner name
	private String selectedScanner = null;

	// Locate Tag
	private boolean isLocatingTag = false;
	private boolean isLocateMode = false;
	private String tagID = "";

	// Tag IT
	private boolean isTagITMode = false;
	private boolean isReadBarcode = false;
	private boolean isProgrammingTag = false;

	// Audit
	private boolean isAuditMode = false;

	// Instance of SDK Handler, Barcode
	public static SDKHandler sdkHandler;
	ArrayList<DCSScannerInfo> scannerAvailableList = new ArrayList<>();
	boolean barcodeDeviceConnected = false;
	private int BarcodeScannerID = 0;

	public RNRfidBarcodeZebraThread(ReactApplicationContext context) {
		this.context = context;
		// //Declare barcode library.
		// sdkHandler = new SDKHandler(context);
		// //In order to receive event.
		// sdkHandler.dcssdkSetDelegate(this);
		// //Setup operation mode.
		// sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
		// //Enable auto detect available devices.
		// sdkHandler.dcssdkEnableAvailableScannersDetection(true);
		//
		// //Barcode library register listener.
		// int notifications_mask = 0;
		// notifications_mask |=
		// (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
		// DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
		// notifications_mask |=
		// (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
		// DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
		// notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
		// sdkHandler.dcssdkSubsribeForEvents(notifications_mask);
		//
		// //Get available devices list.
		// ArrayList<DCSScannerInfo> scannerTreeList = new ArrayList<>();
		// sdkHandler.dcssdkGetAvailableScannersList(scannerTreeList);
		// sdkHandler.dcssdkGetActiveScannersList(scannerTreeList);
		//
		// for (DCSScannerInfo s :
		// scannerTreeList) {
		// scannerAvailableList.add(s);
		// if (s.getAuxiliaryScanners() != null) {
		// for (DCSScannerInfo aux :
		// s.getAuxiliaryScanners().values()) {
		// scannerAvailableList.add(aux);
		// }
		// }
		// }
	}

	public void run() {

	}

	public abstract void dispatchEvent(String name, WritableMap data);

	public abstract void dispatchEvent(String name, String data);

	public abstract void dispatchEvent(String name, WritableArray data);

	public void onHostResume() {
		// if (readers != null) {
		// this.connect();
		// } else {
		// Log.e("RFID", "Can't resume - reader is null");
		// }
	}

	public void onHostPause() {
		// if (this.reading) {
		// this.cancel();
		// }
		// this.disconnect();
	}

	public void onHostDestroy() {
		if (this.reading) {
			this.cancel();
		}
		shutdown();
		barcodeDisconnect();
	}

	public void onCatalystInstanceDestroy() {
		if (this.reading) {
			this.cancel();
		}
		shutdown();
		barcodeDisconnect();
	}

	// Barcode SDK method.
	private boolean executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE opCode, String inXML, StringBuilder outXML,
			int scannerID) {
		if (sdkHandler != null) {
			if (outXML == null) {
				outXML = new StringBuilder();
			}
			DCSSDKDefs.DCSSDK_RESULT result = sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(opCode, inXML,
					outXML, scannerID);
			if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS)
				return true;
			else if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE)
				return false;
		}
		return false;
	}

	// Using barcode library to connect RFID scanner.
	public void barcodeConnect() {
		if (barcodeDeviceConnected) {
			barcodeDisconnect();
		}

		WritableMap event = Arguments.createMap();
		DCSSDKDefs.DCSSDK_RESULT result = DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
		if (sdkHandler == null) {
			// Declare barcode library.
			sdkHandler = new SDKHandler(context);
			// In order to receive event.
			sdkHandler.dcssdkSetDelegate(this);
			// Setup operation mode.
			sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
			// Enable auto detect available devices.
			sdkHandler.dcssdkEnableAvailableScannersDetection(true);

			// Barcode library register listener.
			int notifications_mask = 0;
			notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value
					| DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
			notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value
					| DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
			notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
			sdkHandler.dcssdkSubsribeForEvents(notifications_mask);

			// Get available devices list.
			ArrayList<DCSScannerInfo> scannerTreeList = new ArrayList<>();
			sdkHandler.dcssdkGetAvailableScannersList(scannerTreeList);
			sdkHandler.dcssdkGetActiveScannersList(scannerTreeList);

			for (DCSScannerInfo s : scannerTreeList) {
				scannerAvailableList.add(s);
				if (s.getAuxiliaryScanners() != null) {
					for (DCSScannerInfo aux : s.getAuxiliaryScanners().values()) {
						scannerAvailableList.add(aux);
					}
				}
			}
		}
		if (!barcodeDeviceConnected) {
			if (scannerAvailableList.size() > 0) {
				try {
					int index = -1;
					for (int i = 0; i < scannerAvailableList.size(); i++) {
						String name = scannerAvailableList.get(i).getScannerName();
						if (name.equals(selectedScanner)) {
							index = i;
							break;
						}
					}
					BarcodeScannerID = scannerAvailableList.get(index).getScannerID();
					result = sdkHandler.dcssdkEstablishCommunicationSession(BarcodeScannerID);
					if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS) {
						barcodeDeviceConnected = true;
						// return true;
					} else if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE) {
						barcodeDeviceConnected = false;
						// return false;
					}
				} catch (Exception e) {
					event.putString("error", e.getMessage());
					dispatchEvent("barcodeError", event);
				}
			} else {
				event.putString("error", "DCSSDK_RESULT_SCANNER_NOT_AVAILABLE");
				dispatchEvent("barcodeError", event);
			}
		} else {
			event.putString("error", "DCSSDK_RESULT_SCANNER_ALREADY_ACTIVE");
			dispatchEvent("barcodeError", event);
		}
		// else {
		// event.putString("error", " DCSSDK_RESULT_FAILURE");
		// dispatchEvent("barcodeError", event);
		// }
		// return false;
	}

	// Trigger barcode read.
	public void barcodePullTrigger() {
		if (BarcodeScannerID != 0) {
			StringBuilder outXML = null;
			String in_xml = "<inArgs><scannerID>" + BarcodeScannerID + "</scannerID></inArgs>";
			DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode = DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER;
			executeCommand(opcode, in_xml, outXML, BarcodeScannerID);
		}
	}

	// Release barcode trigger
	public void barcodeReleaseTrigger() {
		if (BarcodeScannerID != 0) {
			StringBuilder outXML = null;
			String in_xml = "<inArgs><scannerID>" + BarcodeScannerID + "</scannerID></inArgs>";
			DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode = DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_RELEASE_TRIGGER;
			executeCommand(opcode, in_xml, outXML, BarcodeScannerID);
		}
	}

	/* ###################################################################### */
	/* ########## IDcsSdkApiDelegate Protocol implementation ################ */
	/* ###################################################################### */
	// Must to declare following functions since the class implement
	// IDcsSdkApiDelegate.
	@Override
	public void dcssdkEventBarcode(byte[] barcodeData, int barcodeType, int fromScannerID) {
		// Receive barcode info, then send to RN.
		this.dispatchEvent("barcode", new String(barcodeData));
	}

	@Override
	public void dcssdkEventAuxScannerAppeared(DCSScannerInfo newTopology, DCSScannerInfo auxScanner) {
		// this.dispatchEvent("barcode", "dcssdkEventAuxScannerAppeared");
	}

	@Override
	public void dcssdkEventFirmwareUpdate(FirmwareUpdateEvent firmwareUpdateEvent) {
		// this.dispatchEvent("barcode", "dcssdkEventFirmwareUpdate");
	}

	@Override
	public void dcssdkEventBinaryData(byte[] binaryData, int fromScannerID) {
		// this.dispatchEvent("barcode", "dcssdkEventBinaryData");
	}

	@Override
	public void dcssdkEventVideo(byte[] videoFrame, int fromScannerID) {
		// this.dispatchEvent("barcode", "dcssdkEventVideo");
	}

	@Override
	public void dcssdkEventImage(byte[] imageData, int fromScannerID) {
		// this.dispatchEvent("barcode", "dcssdkEventImage");
	}

	@Override
	public void dcssdkEventCommunicationSessionTerminated(int scannerID) {
		barcodeDeviceConnected = false;
		// this.dispatchEvent("barcode", "dcssdkEventCommunicationSessionTerminated");
	}

	@Override
	public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo activeScanner) {
		barcodeDeviceConnected = true;
		CheckBarcodeRFIDConnection();
	}

	@Override
	public void dcssdkEventScannerDisappeared(int scannerID) {
		// this.dispatchEvent("barcode", "dcssdkEventScannerDisappeared");
	}

	@Override
	public void dcssdkEventScannerAppeared(DCSScannerInfo availableScanner) {
		// this.dispatchEvent("barcode", "dcssdkEventScannerAppeared");
	}

	public WritableArray GetAvailableBluetoothDevices() {
		WritableArray availableRFIDReaderList = Arguments.createArray();
		if (this.rfidReaderDevice == null) {
			readers = new Readers(this.context, ENUM_TRANSPORT.BLUETOOTH);
			try {
				ArrayList<ReaderDevice> list = readers.GetAvailableRFIDReaderList();
				for (int i = 0; i < list.size(); i++) {
					ReaderDevice reader = list.get(i);
					WritableMap map = Arguments.createMap();
					map.putString("name", reader.getName());
					map.putString("address", reader.getAddress());
					map.putString("password", reader.getPassword());
					availableRFIDReaderList.pushMap(map);
				}
			} catch (InvalidUsageException e) {
				Log.e("RFID", "Init scanner error - invalid message: " + e.getMessage());
			} catch (NullPointerException ex) {
				Log.e("RFID", "Blue tooth not support on device");
			}
		}
		return availableRFIDReaderList;
	}

	private void connect() {
		String err = null;
		if (this.rfidReaderDevice != null) {
			if (rfidReaderDevice.getRFIDReader().isConnected())
				return;
			disconnect();
		}
		try {

			Log.v("RFID", "initScanner");

			ArrayList<ReaderDevice> availableRFIDReaderList = null;
			try {
				availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
				Log.v("RFID", "Available number of reader : " + availableRFIDReaderList.size());
				deviceList = availableRFIDReaderList;

			} catch (InvalidUsageException e) {
				Log.e("RFID", "Init scanner error - invalid message: " + e.getMessage());
			} catch (NullPointerException ex) {
				Log.e("RFID", "Blue tooth not support on device");
			}

			int listSize = (availableRFIDReaderList == null) ? 0 : availableRFIDReaderList.size();
			Log.v("RFID", "Available number of reader : " + listSize);

			if (listSize > 0) {
				int index = -1;
				for (int i = 0; i < availableRFIDReaderList.size(); i++) {
					String name = availableRFIDReaderList.get(i).getName();
					if (name.equals(selectedScanner)) {
						index = i;
						break;
					}
				}

				ReaderDevice readerDevice = availableRFIDReaderList.get(index);
				RFIDReader rfidReader = readerDevice.getRFIDReader();
				// Connect to RFID reader

				if (rfidReader != null) {
					while (true) {
						try {
							rfidReader.connect();
							rfidReader.Config.getDeviceStatus(true, false, false);
							rfidReader.Events.addEventsListener(this);
							// Subscribe required status notification
							rfidReader.Events.setInventoryStartEvent(true);
							rfidReader.Events.setInventoryStopEvent(true);
							// enables tag read notification
							rfidReader.Events.setTagReadEvent(true);
							rfidReader.Events.setReaderDisconnectEvent(true);
							rfidReader.Events.setBatteryEvent(true);
							rfidReader.Events.setBatchModeEvent(true);
							rfidReader.Events.setHandheldEvent(true);
							rfidReader.Events.setPowerEvent(true);
							rfidReader.Events.setOperationEndSummaryEvent(true);

							// Set trigger mode
							setTriggerImmediate(rfidReader);
							break;
						} catch (OperationFailureException ex) {
							if (ex.getResults() == RFIDResults.RFID_READER_REGION_NOT_CONFIGURED) {
								// Get and Set regulatory configuration settings
								try {
									RegulatoryConfig regulatoryConfig = rfidReader.Config.getRegulatoryConfig();
									SupportedRegions regions = rfidReader.ReaderCapabilities.SupportedRegions;
									int len = regions.length();
									boolean regionSet = false;
									for (int i = 0; i < len; i++) {
										RegionInfo regionInfo = regions.getRegionInfo(i);
										if ("AUS".equals(regionInfo.getRegionCode())) {
											regulatoryConfig.setRegion(regionInfo.getRegionCode());
											rfidReader.Config.setRegulatoryConfig(regulatoryConfig);
											Log.i("RFID", "Region set to " + regionInfo.getName());
											regionSet = true;
											break;
										}
									}
									if (!regionSet) {
										err = "Region not found";
										break;
									}
								} catch (OperationFailureException ex1) {
									err = "Error setting RFID region: " + ex1.getMessage();
									break;
								}
							} else if (ex.getResults() == RFIDResults.RFID_CONNECTION_PASSWORD_ERROR) {
								// Password error
								err = "Password error";
								break;
							} else if (ex.getResults() == RFIDResults.RFID_BATCHMODE_IN_PROGRESS) {
								// handle batch mode related stuff
								err = "Batch mode in progress";
								break;
							} else {
								err = ex.getResults().toString();
								break;
							}
						} catch (InvalidUsageException e1) {
							Log.e("RFID", "InvalidUsageException: " + e1.getMessage() + " " + e1.getInfo());
							err = "Invalid usage " + e1.getMessage();
							break;
						}
					}
				} else {
					err = "Cannot get rfid reader";
				}
				if (err == null) {
					// Connect success
					rfidReaderDevice = readerDevice;
					tempDisconnected = false;
					WritableMap event = Arguments.createMap();
					event.putString("RFIDStatusEvent", "opened");
					this.dispatchEvent("RFIDStatusEvent", event);
					Log.i("RFID", "Connected to " + rfidReaderDevice.getName());
					return;
				}
			} else {
				err = "No connected device";
			}
		} catch (InvalidUsageException e) {
			err = "connect: invalid usage error: " + e.getMessage();
		}
		if (err != null) {
			Log.e("RFID", err);
		}
	}

	private void CheckBarcodeRFIDConnection() {
		if (rfidReaderDevice.getRFIDReader().isConnected() && barcodeDeviceConnected) {
			WritableMap event = Arguments.createMap();
			event.putString("RFIDStatusEvent", "connected");
			this.dispatchEvent("RFIDStatusEvent", event);
			try {
				switchDPO(true);
				WritableMap map = Arguments.createMap();
				map.putString("singulationControl", "600");
				saveAntennaConfig(map);
				rfidReaderDevice.getRFIDReader().Config.setBatchMode(BATCH_MODE.DISABLE);
				// rfidReaderDevice.getRFIDReader().Config.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP);
			} catch (InvalidUsageException e) {
				Log.i("Connection", e.getInfo());
			} catch (OperationFailureException e) {
				Log.i("Connection", e.getVendorMessage());
			}
		}
	}

	/**
	 * Set trigger mode
	 */
	private void setTriggerImmediate(RFIDReader reader) throws InvalidUsageException, OperationFailureException {
		TriggerInfo triggerInfo = new TriggerInfo();
		// Start trigger: set to immediate mode
		triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
		// Stop trigger: set to immediate mode
		triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
		reader.Config.setStartTrigger(triggerInfo.StartTrigger);
		reader.Config.setStopTrigger(triggerInfo.StopTrigger);
	}

	// Barcode disconnect
	public void barcodeDisconnect() {
		if (barcodeDeviceConnected) {
			try {
				sdkHandler.dcssdkTerminateCommunicationSession(BarcodeScannerID);
				barcodeDeviceConnected = false;
				BarcodeScannerID = 0;
				sdkHandler = null;
				scannerAvailableList = new ArrayList<>();

			} catch (Exception e) {
				WritableMap event = Arguments.createMap();
				event.putString("error", e.getMessage());
				dispatchEvent("barcodeError", event);
			}

		}
	}

	// RFID disconnect
	private void disconnect() {

		if (this.rfidReaderDevice != null) {
			RFIDReader rfidReader = rfidReaderDevice.getRFIDReader();
			String err = null;
			if (!rfidReader.isConnected()) {
				Log.i("RFID", "disconnect: already disconnected");
				// already disconnected
			} else {
				try {
					rfidReader.disconnect();
				} catch (InvalidUsageException e) {
					err = "disconnect: invalid usage error: " + e.getMessage();
				} catch (OperationFailureException ex) {
					err = "disconnect: " + ex.getResults().toString();
				}
			}
			try {
				if (rfidReader.Events != null) {
					rfidReader.Events.removeEventsListener(this);
				}
			} catch (InvalidUsageException e) {
				err = "disconnect: invalid usage error when removing events: " + e.getMessage();
			} catch (OperationFailureException ex) {
				err = "disconnect: error removing events: " + ex.getResults().toString();
			}
			if (err != null) {
				Log.e("RFID", err);
			}
			// Ignore error and send feedback
			WritableMap event = Arguments.createMap();
			event.putString("RFIDStatusEvent", "closed");
			this.dispatchEvent("RFIDStatusEvent", event);
			rfidReaderDevice = null;
			tempDisconnected = false;
		} else {
			Log.w("RFID", "disconnect: no device was connected");
		}
	}

	public void reconnect() {
		if (this.rfidReaderDevice != null) {
			if (tempDisconnected) {
				RFIDReader rfidReader = this.rfidReaderDevice.getRFIDReader();
				if (!rfidReader.isConnected()) {
					String err = null;
					try {
						// Stop inventory
						rfidReader.reconnect();
						barcodeConnect();
					} catch (InvalidUsageException e) {
						err = "reconnect: invalid usage error: " + e.getMessage();
					} catch (OperationFailureException ex) {
						err = "reconnect error: " + ex.getResults().toString();
					}
					if (err != null) {
						Log.e("RFID", err);
					} else {
						tempDisconnected = false;
						WritableMap event = Arguments.createMap();
						event.putString("RFIDStatusEvent", "opened");
						this.dispatchEvent("RFIDStatusEvent", event);
						Log.i("RFID", "Reconnected to " + rfidReaderDevice.getName());
					}
				} else {
					Log.i("RFID", rfidReaderDevice.getName() + " is already connected");
				}
			} else {
				Log.i("RFID", "reconnect: not temp disconnected");
			}
		} else {
			Log.i("RFID", "reconnect: device is null");
			init(this.context);
			barcodeConnect();
		}
	}

	public void SaveSelectedScanner(String scanner) {
		selectedScanner = scanner;
	}

	public void init(Context context) {
		// Register receiver
		Log.v("RFID", "init");
		readers = new Readers(context, ENUM_TRANSPORT.BLUETOOTH);
		try {
			ArrayList<ReaderDevice> availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
			Log.v("RFID", "Available number of reader : " + availableRFIDReaderList.size());
			deviceList = availableRFIDReaderList;

			Log.v("RFID", "Scanner thread initialized");
		} catch (InvalidUsageException e) {
			Log.e("RFID", "Init scanner error - invalid message: " + e.getMessage());
		} catch (NullPointerException ex) {
			Log.e("RFID", "Blue tooth not support on device");
		}
		tempDisconnected = false;
		reading = false;
		this.connect();
	}

	public void shutdown() {
		if (this.rfidReaderDevice != null) {
			disconnect();
		}
		// Unregister receiver
		if (readers != null) {
			readers.Dispose();
			readers = null;
		}
		deviceList = null;
	}

	public void read(ReadableMap config) {
		if (this.reading) {
			Log.e("RFID", "already reading");
			return;
		}
		String err = null;
		if (this.rfidReaderDevice != null) {
			if (!rfidReaderDevice.getRFIDReader().isConnected()) {
				err = "read: device not connected";
			} else {
				RFIDReader rfidReader = rfidReaderDevice.getRFIDReader();
				try {
					// Perform inventory
					rfidReader.Actions.Inventory.perform();
					reading = true;
				} catch (InvalidUsageException e) {
					err = "read: invalid usage error on scanner read: " + e.getMessage();
				} catch (OperationFailureException ex) {
					err = "read: error setting up scanner read: " + ex.getResults().toString();
				}
			}
		} else {
			err = "read: device not initialised";
		}
		if (err != null) {
			Log.e("RFID", err);
		}
	}

	public void cancel() {
		String err = null;
		if (this.rfidReaderDevice != null) {
			if (!this.rfidReaderDevice.getRFIDReader().isConnected()) {
				err = "cancel: device not connected";
			} else {
				if (reading) {
					RFIDReader rfidReader = rfidReaderDevice.getRFIDReader();
					try {
						// Stop inventory
						rfidReader.Actions.Inventory.stop();
					} catch (InvalidUsageException e) {
						err = "cancel: invalid usage error on scanner read: " + e.getMessage();
					} catch (OperationFailureException ex) {
						err = "cancel: error setting up scanner read: " + ex.getResults().toString();
					}
					reading = false;
				}
			}
		} else {
			err = "cancel: device not initialised";
		}
		if (err != null) {
			Log.e("RFID", err);
		}
	}

	// Check RFID scanner is connected or not.
	public boolean isConnected() {
		if (this.rfidReaderDevice != null) {
			return rfidReaderDevice.getRFIDReader().isConnected();
		} else {
			return false;
		}
	}

	// Turn on/off dynamic power management.
	public String switchDPO(boolean value) {
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			try {
				if (value) {
					this.rfidReaderDevice.getRFIDReader().Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.ENABLE);
					return "DPO switch on";
				} else {
					this.rfidReaderDevice.getRFIDReader().Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.DISABLE);
					return "DPO switch off";
				}
			} catch (InvalidUsageException e) {
				return e.getInfo();
			} catch (OperationFailureException e) {
				return e.getVendorMessage();
			}
		}
		return null;
	}

	// Keep locating tag
	public void LoopForLocateTag() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				LocateTag();
			}
		});
	}

	// Locate tag
	public void LocateTag() {
		WritableMap event = Arguments.createMap();
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			if (this.rfidReaderDevice.getRFIDReader().isCapabilitiesReceived()) {
				if (!isLocatingTag) {
					if (!tagID.isEmpty()) {

						new AsyncTask<Void, Void, Boolean>() {
							private InvalidUsageException invalidUsageException;
							private OperationFailureException operationFailureException;

							@Override
							protected Boolean doInBackground(Void... voids) {
								try {
									rfidReaderDevice.getRFIDReader().Actions.TagLocationing.Perform(tagID, null, null);
								} catch (InvalidUsageException e) {
									invalidUsageException = e;
								} catch (OperationFailureException e) {
									operationFailureException = e;
								}
								return null;
							}

							@Override
							protected void onPostExecute(Boolean result) {
								isLocatingTag = true;
								WritableMap event = Arguments.createMap();
								if (invalidUsageException != null) {
									event.putString("error", invalidUsageException.getInfo());
								} else if (operationFailureException != null) {
									event.putString("error", operationFailureException.getVendorMessage());
								}
								dispatchEvent("locateTag", event);
							}
						}.execute();
					}
				} else {
					new AsyncTask<Void, Void, Boolean>() {
						private InvalidUsageException invalidUsageException;
						private OperationFailureException operationFailureException;

						@Override
						protected Boolean doInBackground(Void... voids) {
							isLocatingTag = false;
							try {
								rfidReaderDevice.getRFIDReader().Actions.TagLocationing.Stop();
							} catch (InvalidUsageException e) {
								invalidUsageException = e;
							} catch (OperationFailureException e) {
								operationFailureException = e;
							}
							return null;
						}

						@Override
						protected void onPostExecute(Boolean result) {

							WritableMap event = Arguments.createMap();
							if (invalidUsageException != null) {
								event.putString("error", invalidUsageException.getInfo());
							} else if (operationFailureException != null) {
								event.putString("error", operationFailureException.getVendorMessage());
							}
							dispatchEvent("locateTag", event);
						}
					}.execute();
				}
			} else {
				event.putString("error", "Reader capabilities not updated");
				dispatchEvent("locateTag", event);
			}
		} else {
			event.putString("error", "No Active Connection with Reader");
			dispatchEvent("locateTag", event);
		}
	}

	// Program tag
	public String writeTag(final String targetTag, String newTag) {
		if (!isProgrammingTag) {
			if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
				if (this.rfidReaderDevice.getRFIDReader().isCapabilitiesReceived()) {
					try {
						if (!targetTag.isEmpty() && !newTag.isEmpty()) {
							isProgrammingTag = true;
							TagAccess tagAccess = new TagAccess();
							final TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();

							writeAccessParams.setAccessPassword(Long.decode("0X" + "0"));
							writeAccessParams.setWriteDataLength(newTag.length() / 4);
							writeAccessParams.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
							writeAccessParams.setOffset(2);
							writeAccessParams.setWriteData(newTag);

							new AsyncTask<Void, Void, Boolean>() {
								private Boolean bResult = false;
								private WritableMap event = Arguments.createMap();
								private InvalidUsageException invalidUsageException;
								private OperationFailureException operationFailureException;

								@Override
								protected Boolean doInBackground(Void... voids) {

									try {
										rfidReaderDevice.getRFIDReader().Actions.TagAccess.writeWait(targetTag,
												writeAccessParams, null, null);
										bResult = true;
									} catch (InvalidUsageException e) {
										invalidUsageException = e;
									} catch (OperationFailureException e) {
										operationFailureException = e;
									}
									return bResult;
								}

								@Override
								protected void onPostExecute(Boolean result) {
									isProgrammingTag = false;
									if (!result) {
										if (invalidUsageException != null) {
											event.putString("error", "" + invalidUsageException.getInfo());
											dispatchEvent("writeTag", event);
										} else if (operationFailureException != null) {
											event.putString("error", "" + operationFailureException.getVendorMessage());
											dispatchEvent("writeTag", event);
										}
									} else {
										dispatchEvent("writeTag", "success");
									}

								}
							}.execute();
						} else {
							return "Both RFID Tag# cannot be empty.";
						}
					} catch (Exception e) {
						return "Error: " + e.getMessage();
					}
				} else {
					return "Reader capabilities not updated";
				}
			} else {
				return "No Active Connection with Reader";
			}
		}
		return null;
	}

	// Save tag id from react native.
	public void saveTagID(String tag) {
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			tagID = tag;
		}
	}

	// Flag as locate mode.
	public void locateMode(boolean value) {
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			isLocateMode = value;
			if (!isLocateMode) {
				isLocatingTag = true;
				this.LoopForLocateTag();
				if (isAuditMode)
					ChangeBeeperVolume(0);
			} else {
				if (isAuditMode)
					ChangeBeeperVolume(1);

			}
		}
	}

	// Flag as Tag IT mode.
	public void TagITMode(boolean value) {
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			isTagITMode = value;
		}
	}

	// Flag as Audit mode.
	public void AuditMode(boolean value) {
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			isAuditMode = value;

			if (isAuditMode) {
				ChangeBeeperVolume(0);
			} else {
				ChangeBeeperVolume(1);
			}
		}
	}

	private void ChangeBeeperVolume(int vol) {
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			try {
				rfidReaderDevice.getRFIDReader().Config
						.setBeeperVolume(vol == 1 ? BEEPER_VOLUME.HIGH_BEEP : BEEPER_VOLUME.QUIET_BEEP);
			} catch (InvalidUsageException e) {
				Log.i("ChangeBeeperVolume", e.getInfo());
			} catch (OperationFailureException e) {
				Log.i("ChangeBeeperVolume", e.getVendorMessage());
			}
		}
	}

	// Flag as use trigger to read barcode
	public void TagITReadBarcode(boolean value) {
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			isReadBarcode = value;
		}
	}

	// Clean tags info that is stored in the RFID scanner.
	public void cleanTags() {
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			rfidReaderDevice.getRFIDReader().Actions.purgeTags();
		}
	}

	public WritableMap getAntennaConfig() {
		WritableMap event = Arguments.createMap();
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			try {
				Antennas.AntennaRfConfig antennaRfConfig = this.rfidReaderDevice.getRFIDReader().Config.Antennas
						.getAntennaRfConfig(1);
				event.putString("tari", antennaRfConfig.getTari() + "");
				event.putString("powerLevel", antennaRfConfig.getTransmitPowerIndex() + "");
				return event;
			} catch (InvalidUsageException e) {
				event.putString("error", e.getInfo());
				return event;
			} catch (OperationFailureException e) {
				event.putString("error", e.getVendorMessage());
				return event;
			}
		}
		return null;
	}

	public WritableMap getConfig() {
		WritableMap event = Arguments.createMap();
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			try {
				Antennas.AntennaRfConfig antennaRfConfig = this.rfidReaderDevice.getRFIDReader().Config.Antennas
						.getAntennaRfConfig(1);
				DYNAMIC_POWER_OPTIMIZATION dynamicPowerSettings = this.rfidReaderDevice.getRFIDReader().Config
						.getDPOState();
				event.putString("tari", antennaRfConfig.getTari() + "");
				event.putString("powerLevel", antennaRfConfig.getTransmitPowerIndex() + "");
				event.putInt("DPO_status", dynamicPowerSettings.getValue());
				return event;
			} catch (InvalidUsageException e) {
				event.putString("error", e.getInfo());
				return event;
			} catch (OperationFailureException e) {
				event.putString("error", e.getVendorMessage());
				return event;
			}
		}
		event.putString("error", "No Active Connection with Reader");
		return event;
	}

	public String saveAntennaConfig(ReadableMap config) {
		if (this.rfidReaderDevice != null && this.rfidReaderDevice.getRFIDReader().isConnected()) {
			if (config == null) {
				return "Config cannot be empty";
			}

			if (config.hasKey("antennaLevel")) {
				try {
					Antennas.AntennaRfConfig antennaRfConfig = this.rfidReaderDevice.getRFIDReader().Config.Antennas
							.getAntennaRfConfig(1);
					int index = Integer.parseInt(config.getString("antennaLevel"));
					antennaRfConfig.setTransmitPowerIndex(index);
					this.rfidReaderDevice.getRFIDReader().Config.Antennas.setAntennaRfConfig(1, antennaRfConfig);
					return "Antenna config changed to " + index;
				} catch (InvalidUsageException e) {
					return e.getInfo();
				} catch (OperationFailureException e) {
					return e.getVendorMessage();
				} catch (NumberFormatException e) {
					return e.getMessage();
				}
			}
			if (config.hasKey("singulationControl")) {
				int index = Integer.parseInt(config.getString("singulationControl"));
				try {
					Antennas.SingulationControl singulationControl = this.rfidReaderDevice
							.getRFIDReader().Config.Antennas.getSingulationControl(1);
					singulationControl.setTagPopulation((short) 600);
					rfidReaderDevice.getRFIDReader().Config.Antennas.setSingulationControl(1, singulationControl);
				} catch (InvalidUsageException e) {
					Log.i("SingulationControl", e.getMessage());
				} catch (OperationFailureException e) {
					Log.i("SingulationControl", e.getMessage());
				}
			}
		}
		return "No Active Connection with Reader";
	}

	@Override
	public void eventReadNotify(RfidReadEvents rfidReadEvents) {
		// reader not active
		if (rfidReaderDevice == null)
			return;
		RFIDReader rfidReader = rfidReaderDevice.getRFIDReader();
		// final TagData[] tagArray = rfidReader.Actions.getReadTags(100);
		// TagDataArray tagArray = rfidReader.Actions.getReadTagsEx(100);
		// if (tagArray != null) {
		//// WritableArray rfidTags = Arguments.createArray();
		// for (int i = 0; i < tagArray.getLength(); i++) {
		// TagData tag = tagArray.getTags()[i];
		// if (tag.isContainsLocationInfo()) {
		// WritableMap event = Arguments.createMap();
		// short tagProximityPercent = tag.LocationInfo.getRelativeDistance();
		// event.putInt("distance", tagProximityPercent);
		// dispatchEvent("locateTag", event);
		// }
		// Log.i("RFID", "Tag ID = " + tag.getTagID());
		// if (tag.getOpCode() == null) {
		// Log.w("RFID", "null opcode");
		// } else {
		// Log.w("RFID", "opcode " + tag.getOpCode().toString());
		// }
		// if (!isLocateMode) {
		// if (isTagITMode) {
		// this.cancel();
		// }
		// this.dispatchEvent("TagEvent", tag.getTagID());
		//// rfidTags.pushString(tag.getTagID());
		//// this.dispatchEvent("TagsEvent", rfidTags);
		// }
		// }
		//
		// }
		final TagData[] myTags = rfidReader.Actions.getReadTags(100);
		if (myTags != null) {
			for (int index = 0; index < myTags.length; index++) {
				if (myTags[index].getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ
						&& myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
					Log.i("RFID", "Tag ID = " + myTags[index]);
				}

				Log.i("RFID", "Tag ID = " + myTags[index].getTagID());

				if (myTags[index].isContainsLocationInfo()) {
					final int tag = index;
					short tagProximityPercent = myTags[tag].LocationInfo.getRelativeDistance();
					WritableMap event = Arguments.createMap();
					event.putInt("distance", tagProximityPercent);
					dispatchEvent("locateTag", event);
				}
				if (myTags[index] != null && (myTags[index].getOpStatus() == null
						|| myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS)) {
					if (!isLocateMode) {
						if (isTagITMode) {
							this.cancel();
							this.dispatchEvent("TagEvent", myTags[index].getTagID());
							break;
						} else {
							this.dispatchEvent("TagEvent", myTags[index].getTagID());
						}
					}
				}
			}
		}
	}

	@Override
	public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
		WritableMap event = Arguments.createMap();

		STATUS_EVENT_TYPE statusEventType = rfidStatusEvents.StatusEventData.getStatusEventType();
		if (statusEventType == STATUS_EVENT_TYPE.INVENTORY_START_EVENT) {
			event.putString("RFIDStatusEvent", "inventoryStart");
			reading = true;
		} else if (statusEventType == STATUS_EVENT_TYPE.INVENTORY_STOP_EVENT) {
			event.putString("RFIDStatusEvent", "inventoryStop");
			reading = false;
		} else if (statusEventType == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
			event.putString("RFIDStatusEvent", "disconnect");
			reading = false;
			tempDisconnected = true;
		} else if (statusEventType == STATUS_EVENT_TYPE.BATCH_MODE_EVENT) {
			event.putString("RFIDStatusEvent", "batchMode");
			Log.i("RFID", "batch mode event: " + rfidStatusEvents.StatusEventData.BatchModeEventData.toString());
		} else if (statusEventType == STATUS_EVENT_TYPE.BATTERY_EVENT) {
			int level = rfidStatusEvents.StatusEventData.BatteryData.getLevel();
			event.putString("RFIDStatusEvent", "battery " + level);
			Log.i("RFID", "battery level " + level);
		} else if (statusEventType == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
			HANDHELD_TRIGGER_EVENT_TYPE eventData = rfidStatusEvents.StatusEventData.HandheldTriggerEventData
					.getHandheldEvent();
			if (eventData == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
				if (isLocateMode) {
					this.LoopForLocateTag();
				} else if (isReadBarcode) {
					barcodePullTrigger();
				} else {
					this.read(this.config);
				}

			} else if (eventData == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
				if (isLocateMode) {
					this.isLocatingTag = true;
					this.LoopForLocateTag();
				} else if (isReadBarcode) {
					barcodeReleaseTrigger();
				} else {
					this.cancel();
					if (isTagITMode || isAuditMode) {
						cleanTags();
					}
				}
			}
		}
		if (event.hasKey("RFIDStatusEvent")) {
			this.dispatchEvent("RFIDStatusEvent", event);
		}
	}
}
