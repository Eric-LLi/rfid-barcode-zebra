package com.reactlibrary;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.lang.NullPointerException;

import com.zebra.rfid.api3.*;
import com.zebra.scannercontrol.*;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

public abstract class RNRfidBarcodeZebraThread extends Thread implements Readers.RFIDReaderEventHandler,
		IDcsSdkApiDelegate {

	private ReactApplicationContext context;

	// RFID
	private Readers readers = null;
	private static RFIDReader reader;
	private static EventHandler eventHandler = null;
	private static ArrayList<ReaderDevice> deviceList = null;
	private static Boolean reading = false;
	private static ArrayList<String> scannedTags = new ArrayList<>();
	private final int MAX_POWER = 290;

	//Flag current react native page
	private static String currentRoute = null;

	// Save reader name
	private static String selectedScanner = null;

	// Locate Tag
	private static boolean isLocatingTag = false;
	private static boolean isLocateMode = false;
	private static String tagID = "";

	// Tag IT
	private static boolean isReadBarcode = false;
	private static boolean isProgrammingTag = false;

	// Instance of SDK Handler, Barcode
	private SDKHandler sdkHandler;
	private static ArrayList<DCSScannerInfo> scannerAvailableList = new ArrayList<>();
	private static boolean barcodeDeviceConnected = false;
	private static int BarcodeScannerID = 0;

	public RNRfidBarcodeZebraThread(ReactApplicationContext context) {
		this.context = context;
		eventHandler = new EventHandler();
		init();
	}

	public abstract void dispatchEvent(String name, WritableMap data);

	public abstract void dispatchEvent(String name, String data);

	public abstract void dispatchEvent(String name, WritableArray data);

	public abstract void dispatchEvent(String name, boolean data);

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
	public boolean barcodeConnect() throws Exception {
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
					scannerAvailableList.addAll(s.getAuxiliaryScanners().values());
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
					} else if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE) {
						barcodeDeviceConnected = false;
						throw new Exception("Barcode scanner connection fail");
					}
				} catch (Exception e) {
					throw e;
				}
			} else {
				throw new Exception("Barcode scanner not available");
			}
		} else {
			throw new Exception("Barcode scanner already connected");
		}
		return barcodeDeviceConnected;
	}

	// Barcode disconnect
	public void barcodeDisconnect() {
		if (barcodeDeviceConnected) {
			sdkHandler.dcssdkTerminateCommunicationSession(BarcodeScannerID);
			barcodeDeviceConnected = false;
			BarcodeScannerID = 0;
			sdkHandler = null;
			scannerAvailableList = new ArrayList<>();
		}
	}

	// Trigger barcode read.
	private void barcodePullTrigger() {
		if (BarcodeScannerID != 0) {
			StringBuilder outXML = null;
			String in_xml = "<inArgs><scannerID>" + BarcodeScannerID + "</scannerID></inArgs>";
			DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode = DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER;
			executeCommand(opcode, in_xml, outXML, BarcodeScannerID);
		}
	}

	// Release barcode trigger
	private void barcodeReleaseTrigger() {
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
	}

	@Override
	public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo activeScanner) {
		barcodeDeviceConnected = true;
	}

	@Override
	public void dcssdkEventScannerDisappeared(int scannerID) {
		// this.dispatchEvent("barcode", "dcssdkEventScannerDisappeared");
	}

	@Override
	public void dcssdkEventScannerAppeared(DCSScannerInfo availableScanner) {
		// this.dispatchEvent("barcode", "dcssdkEventScannerAppeared");
	}

	public WritableArray GetAvailableBluetoothDevices() throws Exception {
		WritableArray list = Arguments.createArray();
		if (readers == null) {
			readers = new Readers(this.context, ENUM_TRANSPORT.BLUETOOTH);
		}
		try {
			deviceList = readers.GetAvailableRFIDReaderList();
			for (ReaderDevice device : deviceList) {
				WritableMap map = Arguments.createMap();
				map.putString("name", device.getName());
				map.putString("address", device.getAddress());
				map.putString("password", device.getPassword());
				list.pushMap(map);
			}
		} catch (InvalidUsageException e) {
			throw new Exception("Init scanner error - invalid message: " + e.getMessage());
		} catch (NullPointerException ex) {
			throw new Exception("Blue tooth not support on device");
		}
		return list;
	}

	private void init() {
		readers = new Readers(context, ENUM_TRANSPORT.BLUETOOTH);

//		tempDisconnected = false;
		reading = false;
	}

	public void connect() throws Exception {
		String err = null;
		if (reader != null) {
			if (reader.isConnected())
				return;
			disconnect();
		}
		try {
			Log.v("RFID", "initScanner");

			readers.attach(this);

			if (deviceList.size() > 0) {
				for (ReaderDevice device : deviceList) {
					if (device.getName().equals(selectedScanner)) {
						reader = device.getRFIDReader();
					}
				}
				if (reader != null) {
					try {
						reader.connect();
						ConfigureReader();
					} catch (OperationFailureException e) {
						if (e.getResults() == RFIDResults.RFID_READER_REGION_NOT_CONFIGURED) {
							try {
								RegulatoryConfig regulatoryConfig =
										reader.Config.getRegulatoryConfig();
								SupportedRegions regions =
										reader.ReaderCapabilities.SupportedRegions;
								int len = regions.length();
								boolean regionSet = false;
								for (int i = 0; i < len; i++) {
									RegionInfo regionInfo = regions.getRegionInfo(i);
									if ("AUS".equals(regionInfo.getRegionCode())) {
										regulatoryConfig.setRegion(regionInfo.getRegionCode());
										reader.Config.setRegulatoryConfig(regulatoryConfig);
										Log.i("RFID", "Region set to " + regionInfo.getName());
										regionSet = true;
									}
								}
								if (!regionSet) {
									err = "Region not found";
								}
							} catch (OperationFailureException ex1) {
								err = "Error setting RFID region: " + ex1.getMessage();
							}
						} else if (e.getResults() == RFIDResults.RFID_CONNECTION_PASSWORD_ERROR) {
							// Password error
							err = "Password error";
						} else if (e.getResults() == RFIDResults.RFID_BATCHMODE_IN_PROGRESS) {
							// handle batch mode related stuff
							err = "Batch mode in progress";
						} else {
							err = e.getResults().toString();
						}
					} catch (InvalidUsageException e) {
						throw new Exception(e.getMessage());
					} catch (Exception e) {
						throw e;
					}

				} else {
					err = "Cannot get rfid reader";
				}

				if (err == null) {
					// Connect success
					Log.i("RFID", "Connected to " + reader.getHostName());
				}
			} else {
				err = "No connected device";
			}

			if (err != null) {
				throw new Exception(err);
			}

		} catch (InvalidUsageException e) {
			err = "connect: invalid usage error: " + e.getMessage();
			throw new Exception("connect: invalid usage error: " + e.getMessage());
		}
	}

	private void ConfigureReader() throws Exception {
		Log.d("ConfigureReader", "ConfigureReader " + reader.getHostName());
		if (reader.isConnected()) {
			TriggerInfo triggerInfo = new TriggerInfo();
			triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
			triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);

			// receive events from reader
			if (eventHandler == null)
				eventHandler = new EventHandler();
			reader.Events.addEventsListener(eventHandler);

			reader.Events.setReaderDisconnectEvent(true);
			reader.Events.setInventoryStartEvent(true);
			reader.Events.setInventoryStopEvent(true);
			reader.Events.setPowerEvent(true);
			reader.Events.setOperationEndSummaryEvent(true);

			//Battery event
			reader.Events.setBatteryEvent(true);
			// HH event. Control active reader
			reader.Events.setHandheldEvent(true);
			// tag event with tag data
			reader.Events.setTagReadEvent(true);
			reader.Events.setAttachTagDataWithReadEvent(false);

			//Disable batch mode
			reader.Events.setBatchModeEvent(false);
			reader.Config.setBatchMode(BATCH_MODE.DISABLE);

			//Turn Off beeper
			reader.Config.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP);

			// set trigger mode as rfid so scanner beam will not come
			reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
			// set start and stop triggers
			reader.Config.setStartTrigger(triggerInfo.StartTrigger);
			reader.Config.setStopTrigger(triggerInfo.StopTrigger);
			//set DPO enable
			reader.Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.ENABLE);
//			// power levels are index based so maximum power supported get the last one
//			MAX_POWER = reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
			// set antenna configurations
			Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
			config.setTransmitPowerIndex(MAX_POWER);
			config.setrfModeTableIndex(0);
			config.setTari(0);
			reader.Config.Antennas.setAntennaRfConfig(1, config);
			// Set the singulation control
			Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
			s1_singulationControl.setSession(SESSION.SESSION_S0);
			s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
			s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
			reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
			// delete any prefilters
			reader.Actions.PreFilters.deleteAll();

			reader.Config.getDeviceStatus(true, false, false);
		}
	}

	private void disconnect() {
		if (reader != null) {
			String err = null;
			if (!reader.isConnected()) {
				Log.i("RFID", "disconnect: already disconnected");
				// already disconnected
			} else {
				try {
					if (reading) {
						cancel();
					}
					reader.Events.removeEventsListener(eventHandler);
					reader.disconnect();

					// RFID
					readers = null;
					reader = null;
					eventHandler = null;
					deviceList = null;
					reading = false;
					scannedTags = new ArrayList<>();

					//Flag current react native page
					currentRoute = null;

					// Save reader name
					selectedScanner = null;

					// Locate Tag
					isLocatingTag = false;
					isLocateMode = false;
					tagID = null;

					// Tag IT
					isReadBarcode = false;
					isProgrammingTag = false;
				} catch (InvalidUsageException e) {
					err = e.getMessage();
				} catch (OperationFailureException e) {
					err = e.getMessage();
				} catch (Exception e) {
					err = e.getMessage();
				}
			}
			if (err != null) {
				HandleError(err, "disconnect");
			}

			// Ignore error and send feedback
			WritableMap event = Arguments.createMap();
			event.putString("RFIDStatusEvent", "closed");
			dispatchEvent("RFIDStatusEvent", event);
		} else {
			Log.w("RFID", "disconnect: no device was connected");
		}
	}

	public void shutdown() {
		if (reader != null) {
			disconnect();
			reader = null;
		}
		// Unregister receiver
		if (readers != null) {
			readers.Dispose();
			readers = null;
		}
		deviceList = null;
	}

	public void SaveCurrentRoute(String value) throws Exception {

		currentRoute = value;

		if (currentRoute != null && currentRoute.equalsIgnoreCase("tagit")) {
			enableDPO(false);
		} else if (currentRoute != null && currentRoute.equalsIgnoreCase("locatetag")) {
			enableBeeper(true);
		} else {
			//
		}
	}

	public void SaveSelectedScanner(String scanner) {
		selectedScanner = scanner;
	}

	public String GetConnectedReader() {
		if (reader != null && reader.isConnected())
			return reader.getHostName();
		else {
			return null;
		}
	}

	public void read() throws Exception {
		if (reading) {
			Log.e("RFID", "already reading");
			throw new Exception("Already reading");
		}
		String err = null;
		if (reader != null) {
			if (!reader.isConnected()) {
				err = "read: device not connected";
			} else {
				try {
					// Perform inventory
					reader.Actions.Inventory.perform();
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
			throw new Exception(err);
		}
	}

	public void cancel() throws Exception {
		String err = null;
		if (reader != null) {
			if (!reader.isConnected()) {
				err = "cancel: device not connected";
			} else {
				if (reading) {
					try {
						// Stop inventory
						reader.Actions.Inventory.stop();
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
			throw new Exception(err);
		}
	}

	// Check RFID scanner is connected or not.
	public boolean isConnected() {
		if (reader != null) {
			return reader.isConnected();
		} else {
			return false;
		}
	}

	public boolean AttemptToReconnect() throws Exception {
		if (selectedScanner != null) {
			connect();
			barcodeConnect();
			return true;
		}
		return false;
	}

	// Turn on/off dynamic power management.
	private void enableDPO(boolean value) throws Exception {
		String err = null;
		if (reader != null && reader.isConnected()) {
			try {
				if (value) {
					reader.Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.ENABLE);
				} else {
					reader.Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.DISABLE);
				}
			} catch (InvalidUsageException e) {
				err = e.getInfo();
			} catch (OperationFailureException e) {
				err = e.getVendorMessage();
			}
		}
		if (err != null) {
			throw new Exception(err);
		}
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
		if (reader != null && reader.isConnected()) {
			if (reader.isCapabilitiesReceived()) {
				if (!isLocatingTag) {
					if (tagID != null) {

						new AsyncTask<Void, Void, Boolean>() {
							private InvalidUsageException invalidUsageException;
							private OperationFailureException operationFailureException;

							@Override
							protected Boolean doInBackground(Void... voids) {
								try {
									reader.Actions.TagLocationing.Perform(tagID, null, null);
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
								reader.Actions.TagLocationing.Stop();
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
	public void writeTag(final String targetTag, String newTag) throws Exception {
		if (!isProgrammingTag) {
			if (reader != null && reader.isConnected()) {
				if (reading) {
					cancel();
				}
				if (reader.isCapabilitiesReceived()) {
					try {
						if (targetTag != null && newTag != null) {
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
								private Exception err;

								@Override
								protected Boolean doInBackground(Void... voids) {
									try {
										reader.Actions.TagAccess.writeWait(targetTag,
												writeAccessParams, null, null, true, false);
										bResult = true;
									} catch (InvalidUsageException e) {
										invalidUsageException = e;
									} catch (OperationFailureException e) {
										operationFailureException = e;
									} catch (Exception e) {
										err = e;
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
										} else if (err != null) {
											event.putString("error", err.getMessage());
											dispatchEvent("writeTag", event);
										}
									} else {
										dispatchEvent("writeTag", "success");
									}
								}
							}.execute();
						} else {
							throw new Exception("Tag# cannot be empty.");
						}
					} catch (Exception e) {
						throw e;
					}
				} else {
					throw new Exception("Reader capabilities not updated");
				}
			} else {
				throw new Exception("No Active Connection with Reader");
			}
		}
	}

	// Save tag id from react native.
	public void saveTagID(String tag) {
		if (reader != null && reader.isConnected()) {
			tagID = tag;
		}
	}

	// Flag as locate mode.
	public void locateMode(boolean value) {
		if (reader != null && reader.isConnected()) {
			isLocateMode = value;
			if (!isLocateMode) {
				isLocatingTag = true;
				this.LoopForLocateTag();
			}
		}
	}

	private void enableBeeper(boolean value) throws Exception {
		if (reader != null && reader.isConnected()) {
			reader.Config.setBeeperVolume(value ? BEEPER_VOLUME.HIGH_BEEP : BEEPER_VOLUME.QUIET_BEEP);
		} else {
			throw new Exception("Reader is not connected");
		}
	}

	// Flag as use trigger to read barcode
	public void ReadBarcode(boolean value) {
		if (reader != null && reader.isConnected()) {
			isReadBarcode = value;
		}
	}

	// Clean tags info that is stored in the RFID scanner.
	public void cleanTags() {
		if (reader != null && reader.isConnected()) {
			reader.Actions.purgeTags();
			scannedTags = new ArrayList<>();
		}
	}

	public boolean setAntennaConfig(ReadableMap config) throws Exception {
		if (reader != null && reader.isConnected()) {
			if (config == null) {
				throw new Exception("Config cannot be empty");
			}
			if (reading) {
				cancel();
			}

			if (config.hasKey("antennaLevel")) {
				try {
					Antennas.AntennaRfConfig antennaRfConfig = reader.Config.Antennas
							.getAntennaRfConfig(1);
					int index = Integer.parseInt(config.getString("antennaLevel"));
					antennaRfConfig.setTransmitPowerIndex(index * 10);
					reader.Config.Antennas.setAntennaRfConfig(1, antennaRfConfig);
				} catch (InvalidUsageException e) {
					throw e;
				} catch (OperationFailureException e) {
					throw e;
				} catch (NumberFormatException e) {
					throw e;
				}
			}
			if (config.hasKey("singulationControl")) {
				int index = Integer.parseInt(config.getString("singulationControl"));
				try {
					Antennas.SingulationControl singulationControl = reader.Config.Antennas.getSingulationControl(1);
					singulationControl.setTagPopulation((short) 600);
					reader.Config.Antennas.setSingulationControl(1, singulationControl);
				} catch (InvalidUsageException e) {
					throw e;
				} catch (OperationFailureException e) {
					throw e;
				}
			}
			return true;
		} else {
			throw new Exception("No Active Connection with Reader");
		}
	}

	@Override
	public void RFIDReaderAppeared(ReaderDevice readerDevice) {
		Log.e("RFIDReaderAppeared", readerDevice.getName() + " is available.");
	}

	@Override
	public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
		Log.e("RFIDReaderAppeared", readerDevice.getName() + " is unavailable.");
	}

	private class EventHandler implements RfidEventsListener {
		//Read tag handler
		@Override
		public void eventReadNotify(RfidReadEvents rfidReadEvents) {
			// reader not active
			if (reader == null)
				return;

			final TagData[] myTags = reader.Actions.getReadTags(100);
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
						String EPC = myTags[index].getTagID();
						int rssi = myTags[index].getPeakRSSI();
						if (!isLocateMode) {
							if (currentRoute != null && currentRoute.equals("tagit")) {
								if (rssi > -40) {
									boolean result = addTagToList(EPC);
									if (result && scannedTags.size() == 1) {
										dispatchEvent("TagEvent", EPC);
										return;
									}
								}
							} else {
								boolean result = addTagToList(EPC);
								if (result) {
									dispatchEvent("TagEvent", EPC);
								}
							}
						}
					}
				}
			}
		}

		//Status handler
		@Override
		public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
			Log.d("eventStatusNotify", "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());
			WritableMap event = Arguments.createMap();

			STATUS_EVENT_TYPE statusEventType = rfidStatusEvents.StatusEventData.getStatusEventType();
			if (statusEventType == STATUS_EVENT_TYPE.INVENTORY_START_EVENT) {
				event.putString("RFIDStatusEvent", "inventoryStart");
			} else if (statusEventType == STATUS_EVENT_TYPE.INVENTORY_STOP_EVENT) {
				event.putString("RFIDStatusEvent", "inventoryStop");
			} else if (statusEventType == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
				event.putString("RFIDStatusEvent", "disconnect");
				disconnect();
			} else if (statusEventType == STATUS_EVENT_TYPE.BATCH_MODE_EVENT) {
				event.putString("RFIDStatusEvent", "batchMode");
				Log.i("RFID", "batch mode event: " + rfidStatusEvents.StatusEventData.BatchModeEventData.toString());
			} else if (statusEventType == STATUS_EVENT_TYPE.BATTERY_EVENT) {
				int level = rfidStatusEvents.StatusEventData.BatteryData.getLevel();
				WritableMap event2 = Arguments.createMap();
				event2.putBoolean("ConnectionState", true);
				event2.putString("BatteryLevel", String.valueOf(level));
				dispatchEvent("RFIDStatusEvent", event2);
			} else if (statusEventType == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
				HANDHELD_TRIGGER_EVENT_TYPE eventData = rfidStatusEvents.StatusEventData.HandheldTriggerEventData
						.getHandheldEvent();
				if (eventData == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
					try {
						if (isLocateMode) {
							LoopForLocateTag();
						} else if (isReadBarcode) {
							if (barcodeDeviceConnected) {
								barcodePullTrigger();
							} else {
								dispatchEvent("BarcodeTrigger", true);
							}
						} else if (currentRoute != null) {
							read();
						}
					} catch (Exception e) {
						HandleError(e.getMessage(), "TriggerPressed");
					}
				} else if (eventData == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
					try {
						if (isLocateMode) {
							isLocatingTag = true;
							LoopForLocateTag();
						} else if (isReadBarcode) {
							if (barcodeDeviceConnected) {
								barcodeReleaseTrigger();
							} else {
								dispatchEvent("BarcodeTrigger", false);
							}
						} else {
							cancel();
							reader.Actions.purgeTags();
							if (currentRoute != null && currentRoute.equals("tagit")) {
								scannedTags = new ArrayList<>();
							}
						}
					} catch (Exception e) {
						HandleError(e.getMessage(), "TriggerReleased");
					}
				}
			}
			if (event.hasKey("RFIDStatusEvent")) {
				dispatchEvent("RFIDStatusEvent", event);
			}
		}
	}

	private static boolean addTagToList(String strEPC) {
		if (strEPC != null) {
			if (!checkIsExisted(strEPC)) {
				scannedTags.add(strEPC);
				return true;
			}
		}
		return false;
	}

	private static boolean checkIsExisted(String strEPC) {
		for (int i = 0; i < scannedTags.size(); i++) {
			String tag = scannedTags.get(i);
			if (strEPC != null && strEPC.equals(tag)) {
				return true;
			}
		}
		return false;
	}

	private void HandleError(String error, String code) {
		Log.e(code, error);
		WritableMap map = Arguments.createMap();
		map.putString("code", code);
		map.putString("message", error);
		dispatchEvent("HandleError", map);
	}

}
