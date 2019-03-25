
# react-native-rfid-barcode-zebra

## Getting started

`$ npm install react-native-rfid-barcode-zebra --save`

### Mostly automatic installation

`$ react-native link react-native-rfid-barcode-zebra`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-rfid-barcode-zebra` and add `RNRfidBarcodeZebra.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNRfidBarcodeZebra.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNRfidBarcodeZebraPackage;` to the imports at the top of the file
  - Add `new RNRfidBarcodeZebraPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-rfid-barcode-zebra'
  	project(':react-native-rfid-barcode-zebra').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-rfid-barcode-zebra/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-rfid-barcode-zebra')
  	```

#### Windows
[Read it! :D](https://github.com/ReactWindows/react-native)

1. In Visual Studio add the `RNRfidBarcodeZebra.sln` in `node_modules/react-native-rfid-barcode-zebra/windows/RNRfidBarcodeZebra.sln` folder to their solution, reference from their app.
2. Open up your `MainPage.cs` app
  - Add `using Rfid.Barcode.Zebra.RNRfidBarcodeZebra;` to the usings at the top of the file
  - Add `new RNRfidBarcodeZebraPackage()` to the `List<IReactPackage>` returned by the `Packages` method


## Usage
```javascript
import RNRfidBarcodeZebra from 'react-native-rfid-barcode-zebra';

// TODO: What to do with the module?
RNRfidBarcodeZebra;
```
  