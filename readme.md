# Barcode sample application

This application shows how to use barcode API on Coppernic's devices.

## Build

```sh
./gradlew build
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

String barcode = '3.4.2'

dependencies {
    implementation "fr.coppernic.sdk.core:CpcCore:1.8.2"
    implementation "fr.coppernic.sdk.cpcutils:CpcUtilsLib:6.18.0"
    implementation "fr.coppernic.sdk.barcode:CpcBarcode:$barcode"
}

// Special case for C-One²

configurations.all {
    if( name.startsWith("conen") ) {
        resolutionStrategy.dependencySubstitution {
            substitute module("fr.coppernic.sdk.barcode:CpcBarcode:$barcode") with module('fr.coppernic.lib.barcode:CpcBarcode-conen:1.2.0')
        }
    }
}

```

If you are compiling your application only for C-One², just add this

```groovy
    implementation "fr.coppernic.lib.barcode:CpcBarcode-conen:1.2.0"
```

instead of this

```groovy
    implementation "fr.coppernic.sdk.barcode:CpcBarcode:$barcode"
```

### PowerMgmt

To use power management, please read [this](https://coppernic.github.io/coppernic/2017/10/23/Power-Management.html)

## Barcode API

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

## Configure Barcode Service

### Get a GlobalConfig instance

```java
GlobalConfig globalConfig = GlobalConfig.Builder.get(context);
```

Then you can call methods of GlobalConfig object to configure barcode behavior.

### Warning

- If you have CpcSystemServices with version > `3.3`, then you shall use CpcBarcode > `3.4.2`
- If you have CpcSystemServices with version < `3.2`, then you shall use CpcBarcode < `3.3.0`

