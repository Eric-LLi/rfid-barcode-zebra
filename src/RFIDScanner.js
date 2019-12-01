import { NativeModules, DeviceEventEmitter } from 'react-native';
import _ from 'lodash';
import { RFIDScannerEvent } from './RFIDScannerEvent';

const rfidScannerManager = NativeModules.RNRfidBarcodeZebra;

let instance = null;

export class RFIDScanner {
	constructor() {
		if (!instance) {
			instance = this;
			this.opened = false;
			this.oncallbacks = [];
		}
	}

	handlerLocateTagEvent(event) {
		if (!_.isEmpty(event) && !_.isEmpty(event.error)) {
			if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.LOCATE_TAG)) {
				this.oncallbacks[RFIDScannerEvent.LOCATE_TAG].forEach(callback => {
					callback({ error: event.error });
				});
			}
		} else if (!_.isEmpty(event)) {
			if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.LOCATE_TAG)) {
				this.oncallbacks[RFIDScannerEvent.LOCATE_TAG].forEach(callback => {
					callback(event);
				});
			}
		}
	}

	handlerBarcodeEvent(event) {
		console.log(`Barcode event: ${event}`);
		if (!_.isEmpty(event) && !_.isEmpty(event.error)) {
			if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.BARCODE)) {
				this.oncallbacks[RFIDScannerEvent.BARCODE].forEach(callback => {
					callback({ error: event.error });
				});
			}
		} else if (!_.isEmpty(event)) {
			if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.BARCODE)) {
				this.oncallbacks[RFIDScannerEvent.BARCODE].forEach(callback => {
					callback(event);
				});
			}
		}
	}

	handleWriteTagEvent(event) {
		console.log(`RFID write event: ${event}`);
		if (!_.isEmpty(event) && !_.isEmpty(event.error)) {
			if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.WRITETAG)) {
				this.oncallbacks[RFIDScannerEvent.WRITETAG].forEach(callback => {
					callback(event.error);
				});
			}
		} else if (!_.isEmpty(event)) {
			if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.WRITETAG)) {
				this.oncallbacks[RFIDScannerEvent.WRITETAG].forEach(callback => {
					callback(event);
				});
			}
		}
	}

	handleStatusEvent(event) {
		console.log(`RFID status event ${event.RFIDStatusEvent}`);
		if (event.hasOwnProperty('ConnectionState')) {
			if (this.oncallbacks.hasOwnProperty('RFIDStatusEvent')) {
				this.oncallbacks.RFIDStatusEvent.forEach(callback => {
					callback(event);
				});
			}
		} else if (
			event.RFIDStatusEvent === 'inventoryStart' ||
			event.RFIDStatusEvent === 'inventoryStop'
		) {
			if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.triggerAction)) {
				this.oncallbacks[RFIDScannerEvent.triggerAction].forEach(callback => {
					callback(event);
				});
			}
		}
	}

	handleTagEvent(tag) {
		if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.TAG)) {
			this.oncallbacks[RFIDScannerEvent.TAG].forEach(callback => {
				callback(tag);
			});
		}
	}

	handleTagsEvent(tags) {
		if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.TAGS)) {
			this.oncallbacks[RFIDScannerEvent.TAGS].forEach(callback => {
				callback(tags);
			});
		}
	}

	RemoveAllListener = () => {
		if (!_.isEmpty(this.tagEvent)) {
			this.tagEvent.remove();
			this.tagEvent = null;
		}
		if (!_.isEmpty(this.rfidStatusEvent)) {
			this.rfidStatusEvent.remove();
			this.rfidStatusEvent = null;
		}
		if (!_.isEmpty(this.writeTagEvent)) {
			this.writeTagEvent.remove();
			this.writeTagEvent = null;
		}
		if (!_.isEmpty(this.barcodeEvent)) {
			this.barcodeEvent.remove();
			this.barcodeEvent = null;
		}
		if (!_.isEmpty(this.locateTagEvent)) {
			this.locateTagEvent.remove();
			this.locateTagEvent = null;
		}
	};

	ActiveAllListener = () => {
		if (_.isEmpty(this.tagEvent))
			this.tagEvent = DeviceEventEmitter.addListener(
				RFIDScannerEvent.TAG,
				this.handleTagEvent.bind(this)
			);
		if (_.isEmpty(this.rfidStatusEvent))
			this.rfidStatusEvent = DeviceEventEmitter.addListener(
				RFIDScannerEvent.RFID_Status,
				this.handleStatusEvent.bind(this)
			);
		if (_.isEmpty(this.writeTagEvent))
			this.writeTagEvent = DeviceEventEmitter.addListener(
				RFIDScannerEvent.WRITETAG,
				this.handleWriteTagEvent.bind(this)
			);
		if (_.isEmpty(this.barcodeEvent))
			this.barcodeEvent = DeviceEventEmitter.addListener(
				RFIDScannerEvent.BARCODE,
				this.handlerBarcodeEvent.bind(this)
			);
		if (_.isEmpty(this.locateTagEvent))
			this.locateTagEvent = DeviceEventEmitter.addListener(
				RFIDScannerEvent.LOCATE_TAG,
				this.handlerLocateTagEvent.bind(this)
			);
	};

	InitThread = () => {
		rfidScannerManager.InitialThread();
	};

	SetAntennaConfig(config) {
		return rfidScannerManager.setAntennaConfig(config);
	}

	saveTagID(value) {
		rfidScannerManager.saveTagID(value);
	}

	locateMode(value) {
		rfidScannerManager.locateMode(value);
	}

	locateTag(tag) {
		rfidScannerManager.locateTag(tag);
	}

	SaveCurrentRoute = value => {
		return rfidScannerManager.SaveCurrentRoute(value);
	};

	ReadBarcode(value) {
		return rfidScannerManager.ReadBarcode(value);
	}

	barcodeConnect = () => {
		return rfidScannerManager.barcodeConnect();
	};

	cleanTags() {
		rfidScannerManager.cleanTags();
	}

	GetAvailableBluetoothDevices = () => {
		return rfidScannerManager.GetAvailableBluetoothDevices();
	};

	InitialThread = () => {
		rfidScannerManager.InitialThread();
	};

	init = () => {
		// this.oncallbacks = [];
		return rfidScannerManager.init();
	};

	connect = () => {
		return rfidScannerManager.connect();
	}
	
	SaveSelectedScanner = item => {
		rfidScannerManager.SaveSelectedScanner(item);
	};

	GetConnectedReader = () => {
		return rfidScannerManager.GetConnectedReader();
	};

	AttemptToReconnect = () => {
		return rfidScannerManager.AttemptToReconnect();
	};

	writeTag(targetTag, newTag) {
		return rfidScannerManager.writeTag(targetTag, newTag);
	}

	isConnected = () => {
		return rfidScannerManager.isConnected();
	};

	barcodeDisconnect() {
		rfidScannerManager.barcodeDisconnect();
	}

	shutdown() {
		rfidScannerManager.shutdown();
	}

	OpenAndroidSetting = () => {
		rfidScannerManager.OpenAndroidSetting();
	};

	on(event, callback) {
		this.oncallbacks[event] = [];
		this.oncallbacks[event].push(callback);
	}

	removeon(event, callback) {
		if (this.oncallbacks.hasOwnProperty(event)) {
			this.oncallbacks[event].forEach((funct, index) => {
				// if (callback === undefined || callback === null) {
				// this.oncallbacks[event] = [];
				// } else
				if (funct.toString() === callback.toString()) {
					this.oncallbacks[event].splice(index, 1);
				}
			});
		}
	}
}

export default new RFIDScanner();
