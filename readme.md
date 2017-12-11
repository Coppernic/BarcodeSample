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
    compile("fr.coppernic.sdk.barcode:CpcBarcode:3.0.2@aar") {
        transitive = true
    }
    compile("fr.coppernic.sdk.core:CpcCore:1.0.1@aar") {
      transitive = true
    }
    compile("fr.coppernic.sdk.cpcutils:CpcUtilsLib:6.9.0@aar") {
      transitive = true
    }
}
```

### PowerMgmt

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

### Barcode Reader

 - First get an instance of BarcodeReader

 ```java
BarcodeListener barcodeListener = new BarcodeListener(){
...
};


BarcodeFactory factory = BarcodeFactory.get()
                                       .setBarcodeListener(barcodeListener)
                                       // C-One are shipped with reader working at 115200
                                       .setBaudrate(115200)
                                       //Port should be good by default
                                       //Type should be good by default also
                                       .setType(BarcodeReaderType.OPTICON_MDI3100);

factory.build(context, new InstanceListener<BarcodeReader>{

    @Override
    public void onCreated(BarcodeReader instance) {
        //Store instance here
    }

    @Override
    public void onDisposed(BarcodeReader instance) {
        // Should never be called
    }

}
)
```
 
 - Open BarcodeReader
  
```java
reader.open();

BarcodeListener barcodeListener = new BarcodeListener(){

    @Override
    public void onOpened(RESULT res) {
        //Then this method is called whith the result
    }

...
};
```

 - Trig a scan
 
```java
reader.scan();

BarcodeListener barcodeListener = new BarcodeListener(){

    @Override
    public void onScan(RESULT res, ScanResult data) {
        //Then this method is called
        
        Log.d(TAG, "onScan " + res);
        Log.d(TAG, data == null ? "null" : data.toString() + ", " + res);
    }
...
};

```

 - Don't forget to close the reader to release resources
  
```
reader.close();
```

 - You may power off when you are done with the scanner
 
```
power.powerOff();
```