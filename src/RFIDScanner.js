import { NativeModules, DeviceEventEmitter } from 'react-native';
import { RFIDScannerEvent } from './RFIDScannerEvent';
import _ from 'lodash';

const rfidScannerManager = NativeModules.RNRfidBarcodeZebra;

let instance = null;

export class RFIDScanner {
	constructor() {
		if (!instance) {
			instance = this;
			this.opened = false;
			this.deferReading = false;
			this.oncallbacks = [];
			this.config = {};

			DeviceEventEmitter.addListener('TagEvent', this.handleTagEvent.bind(this));
			DeviceEventEmitter.addListener('TagsEvent', this.handleTagsEvent.bind(this));
			DeviceEventEmitter.addListener('RFIDStatusEvent', this.handleStatusEvent.bind(this));
			DeviceEventEmitter.addListener('writeTag', this.handleWriteTagEvent.bind(this));
			DeviceEventEmitter.addListener('barcode', this.handlerBarcodeEvent.bind(this));
			DeviceEventEmitter.addListener(
				RFIDScannerEvent.LOCATE_TAG,
				this.handlerLocateTagEvent.bind(this)
			);
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
		console.log('Barcode event: ' + event);
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
		console.log('RFID write event: ' + event);
		if (!_.isEmpty(event) && !_.isEmpty(event.error)) {
			if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.WRITETAG)) {
				this.oncallbacks[RFIDScannerEvent.WRITETAG].forEach(callback => {
					callback(event.error);
				});
			}
		} else {
			if (!_.isEmpty(event)) {
				if (this.oncallbacks.hasOwnProperty(RFIDScannerEvent.WRITETAG)) {
					this.oncallbacks[RFIDScannerEvent.WRITETAG].forEach(callback => {
						callback(event);
					});
				}
			}
		}
	}

	handleStatusEvent(event) {
		console.log('RFID status event ' + event.RFIDStatusEvent);
		if (event.RFIDStatusEvent == 'opened') {
			this.opened = true;
			if (this.deferReading) {
				rfidScannerManager.read(this.config);
				this.deferReading = false;
			}
		} else if (event.RFIDStatusEvent == 'closed') {
			this.opened = false;
		} else if (event.RFIDStatusEvent.split(' ')[0] == 'battery') {
			if (this.oncallbacks.hasOwnProperty('RFIDStatus')) {
				this.oncallbacks['RFIDStatus'].forEach(callback => {
					callback(event.RFIDStatusEvent + '%');
				});
			}
		} else if (
			event.RFIDStatusEvent == 'inventoryStart' ||
			event.RFIDStatusEvent == 'inventoryStop'
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
	getAntennaConfig(callback) {
		rfidScannerManager.getAntennaConfig(callback);
	}
	getConfig(callback){
		rfidScannerManager.getConfig(callback);
	}
	saveAntennaConfig(config, callback) {
		rfidScannerManager.saveAntennaConfig(config, callback);
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
	barcodeConnect(callback) {
		rfidScannerManager.barcodeConnect(callback);
	}

	switchDPO(value, callback) {
		rfidScannerManager.switchDPO(value, callback);
	}

	cleanTags() {
		rfidScannerManager.cleanTags();
	}

	init() {
		// this.oncallbacks = [];
		rfidScannerManager.init();
	}

	read(config = {}) {
		this.config = config;

		if (this.opened) {
			rfidScannerManager.read(this.config);
		} else {
			this.deferReading = true;
		}
	}

	reconnect() {
		rfidScannerManager.reconnect();
	}

	cancel() {
		rfidScannerManager.cancel();
	}

	writeTag(sourceTag, targetTag, callback) {
		rfidScannerManager.writeTag(sourceTag, targetTag, callback);
	}

	isConnected(callback) {
		rfidScannerManager.isConnected(callback);
	}
	barcodeDisconnect(){
		rfidScannerManager.barcodeDisconnect();
	}
	shutdown() {
		rfidScannerManager.shutdown();
	}

	on(event, callback) {
		// if (!this.oncallbacks[event]) {
		// 	this.oncallbacks[event] = [];
		// }
		this.oncallbacks[event] = [];
		this.oncallbacks[event].push(callback);
	}

	removeon(event, callback) {
		if (this.oncallbacks.hasOwnProperty(event)) {
			this.oncallbacks[event].forEach((funct, index) => {
				if (funct.toString() === callback.toString()) {
					this.oncallbacks[event].splice(index, 1);
				}
			});
		}
	}

	hason(event, callback) {
		let result = false;
		if (this.oncallbacks.hasOwnProperty(event)) {
			this.oncallbacks[event].forEach((funct, index) => {
				if (funct.toString() === callback.toString()) {
					result = true;
				}
			});
		}
		return result;
	}
}

export default new RFIDScanner();
