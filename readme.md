# Barcode sample application

This application shows how to use barcode API on Coppernic's devices.

## Build

This application normally stands in a standard gradle workspace at Coppernic. Nevertheless, it is possible to build it as a standalone project. For that, you need to specify the `ìnit.gradle` script when running `gradle`.

To build run this command :

```sh
./gradlew -I init.gradle build
```

## Quick start

### Build

**build.gradle**

```groovy
repositories {
    mavenCentral()
    jcenter()
    maven { url 'https://artifactory.coppernic.fr/artifactory/libs-release' }
}

dependencies {
    compile("fr.coppernic.sdk.barcode:CpcBarcode:3.2.0@aar") {
        transitive = true
    }
    compile("fr.coppernic.sdk.core:CpcCore:1.5.0@aar") {
      transitive = true
    }
    compile("fr.coppernic.sdk.cpcutils:CpcUtilsLib:6.13.0@aar") {
      transitive = true
    }
}
```

### PowerMgmt

> Description above is now deprecated. To use power management, please read [this](https://coppernic.github.io/coppernic/2017/10/23/Power-Management.html)

----

We need to power the barcode reader before using it

 - Get an instance of PowerMgmt

```java
PowerMgmtFactory factory = PowerMgmtFactory.get()
            .setContext(getContext())
            // 500ms is needed by the reader to initialize
            .setTimeToSleepAfterPowerOn(500);
            // Waiting for the line to be down (Capacitive effect of some hardware)
            .setTimeToSleepAfterPowerOff(200);

// In this example we are telling Powermgmt to use Barcore reader Opticon Mdi3100 that is installed on C-One
factory.setPeripheralTypes(PeripheralTypesCone.BarcodeReader);
factory.setInterfaces(InterfacesCone.ScannerPort);
factory.setManufacturers(ManufacturersCone.Opticon);
factory.setModels(ModelsCone.Mdi3100);

PowerMgmt power = factory.build();
```

 - PowerMgmt instance can be used for powering up or down peripheral

```java
//  Avoid to do that on UI thread
power.powerOn();
power.powerOff();
```

CpcBarcode API
--------------

### Get a reader

A barcode reader instance is build using the `BarcodeFactory` class. Barcode reader
instance is given asynchronously. `onCreated()` method is called with the newly created
 instance. `onDisposed()` is called if reader instance is disposed for any reason or if
  the build has failed.

```java
public class example implements BarcodeReader.BarcodeListener,
InstanceListener<BarcodeReader> {

    private BarcodeReader reader;

    public void makeReader() {
         BarcodeFactory factory = BarcodeFactory.get();
         //Mandatory
         factory.setBarcodeListener(this);

         //Optional
         factory.setBaudrate(115200);
         factory.setPort("/dev/ttyHS1");
         factory.setType(fr.coppernic.sdk.barcode.BarcodeReaderType.OPTICON_MDI3100);

         boolean ok = factory.build(context, this);
    }

    // Called with the new instance
    @Override
    public void onCreated(BarcodeReader instance) {
        Log.d(TAG, "onCreated " + instance);
        reader = instance;
        if (instance == null) {
            log("No reader available");
        } else {
            //enable power
            power(true);
        }
    }

    // Called if an error occurred.
    @Override
    public void onDisposed(BarcodeReader instance) {
        Log.d(TAG, "onDisposed " + instance);
        reader = null;
    }

    // [...]
}
```

### Reader opening

Opening is done asynchronously. `onOpened()` is called with the result of the operation.

```java
public class example implements BarcodeReader.BarcodeListener,
InstanceListener<BarcodeReader> {

    private void open() {
        reader.open();
    }

    @Override
    public void onOpened(RESULT res) {
        Toast.makeText(getContext(), res.toString(), Toast.LENGTH_SHORT).show();
        // Do some operation after open here
    }

}
```

### Reader closing

Be sure to close the reader when you are done with it. It can then free some resources.

```java
public class example implements BarcodeReader.BarcodeListener,
InstanceListener<BarcodeReader> {

    private void close() {
        Log.d(TAG, "close");
        if (reader != null && reader.isOpened()) {
            reader.close();
        }
    }

}
```

### Get firmware version

```java
public class example implements BarcodeReader.BarcodeListener,
InstanceListener<BarcodeReader> {

    private void getFirmware() {
        RESULT res = reader.getFirmware();
        Toast.makeText(getContext(), res.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFirmware(RESULT res, String s) {
        Log.d(TAG, "onFirmware " + res);
        log("Firmware : " + (s == null ? "null" : s));
    }

}
```

### Scan data

```java
public class example implements BarcodeReader.BarcodeListener,
InstanceListener<BarcodeReader> {

    private void scan() {
        RESULT res = reader.scan();
        Toast.makeText(getContext(), res.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScan(RESULT res, ScanResult data) {
        Log.d(TAG, "onScan " + res);
        log(data == null ? "null" : data.toString() + ", " + res);
    }
}
```

### Connector

Connector is a special kind of reader. It is connecting to CpcSystemServices to get an instance of reader.
Here is how to get one :


```java
public class example implements BarcodeReader.BarcodeListener,
InstanceListener<BarcodeReader> {

    private BarcodeReader reader;

    public void makeReader() {
         BarcodeFactory factory = BarcodeFactory.get();
         //Mandatory
         factory.setBarcodeListener(this);

         GlobalConfig globalConfig = GlobalConfig.Builder.get(mContext);
         globalConfig.setPort(/*port of barcode reader*/);
         globalConfig.setBarcodeType(/*type of barcode reader*/);

         //Optional
         factory.setType(fr.coppernic.sdk.barcode.BarcodeReaderType.CONNECTOR);

         boolean ok = factory.build(context, this);
    }

    // Called with the new instance
    @Override
    public void onCreated(BarcodeReader instance) {
        Log.d(TAG, "onCreated " + instance);
        reader = instance;
        if (instance == null) {
            log("No reader available");
        } else {
            //enable power
            power(true);
        }
    }

    // Called if an error occurred.
    @Override
    public void onDisposed(BarcodeReader instance) {
        Log.d(TAG, "onDisposed " + instance);
        reader = null;
    }

    // [...]
}
```

#### Timeout

Service is automatically configuring reader timeout to infinite.
As soon as the barcode service starts (when you disconnect from connector)
timeout settings will change. If you need to handle a specific timeout,
then you need to configure it each time you get the connector instance.