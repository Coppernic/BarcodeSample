package fr.coppernic.sample.barcode;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.coppernic.sample.barcode.preferences.SettingsActivity;
import fr.coppernic.sdk.barcode.BarcodeFactory;
import fr.coppernic.sdk.barcode.BarcodeReader;
import fr.coppernic.sdk.barcode.BarcodeReader.BarcodeListener;
import fr.coppernic.sdk.barcode.BarcodeReader.ScanResult;
import fr.coppernic.sdk.barcode.BarcodeReaderType;
import fr.coppernic.sdk.barcode.Symbol;
import fr.coppernic.sdk.barcode.SymbolSetting;
import fr.coppernic.sdk.barcode.SymbolSetting.SettingParam;
import fr.coppernic.sdk.barcode.SymbolSettingDiff;
import fr.coppernic.sdk.barcode.core.Parameter;
import fr.coppernic.sdk.barcode.core.Parameter.ParamType;
import fr.coppernic.sdk.power.PowerManager;
import fr.coppernic.sdk.power.api.PowerListener;
import fr.coppernic.sdk.power.api.peripheral.Peripheral;
import fr.coppernic.sdk.power.impl.cizi.CiziPeripheral;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.power.impl.idplatform.IdPlatformPeripheral;
import fr.coppernic.sdk.utils.core.CpcBytes;
import fr.coppernic.sdk.utils.core.CpcResult.RESULT;
import fr.coppernic.sdk.utils.helpers.CpcOs;
import fr.coppernic.sdk.utils.io.InstanceListener;

/**
 * A placeholder fragment containing a simple view.
 */
public class BarcodeFragment extends Fragment {

    private static final String TAG = "BarcodeFragment";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 42;
    private final Handler handler = new Handler();
    @BindView(R.id.btnOpen)
    Button btnOpen;
    @BindView(R.id.btnFirm)
    Button btnFirm;
    @BindView(R.id.btnScan)
    Button btnScan;
    @BindView(R.id.btnGetParam)
    Button btnGetParam;
    @BindView(R.id.btnSetParam)
    Button btnSetParam;
    @BindView(R.id.btnGetSym)
    Button btnGetSym;
    @BindView(R.id.btnSetSym)
    Button btnSetSym;
    @BindView(R.id.txtSetParam)
    EditText edtSetParam;
    @BindView(R.id.spinnerSetParam)
    Spinner spinnerSetParam;
    @BindView(R.id.spinnerGetParam)
    Spinner spinnerGetParam;
    @BindView(R.id.spinnerGetSym)
    Spinner spinnerGetSym;
    @BindView(R.id.spinnerSetSym)
    Spinner spinnerSetSym;
    @BindView(R.id.checkGetParam)
    CheckBox checkGetParam;
    @BindView(R.id.txtLog)
    TextView txtLog;
    @BindView(R.id.checkGetSym)
    CheckBox checkGetSym;
    @BindView(R.id.checkReloadAll)
    CheckBox checkGetAllSym;
    @Nullable @BindView(R.id.switchSymEnable)
    Switch dialogSwitch;
    @Nullable @BindView(R.id.edtPrefix)
    EditText dialogPrefix;
    @Nullable @BindView(R.id.edtSuffix)
    EditText dialogSuffix;
    @Nullable @BindView(R.id.edtMin)
    EditText dialogMin;
    @Nullable @BindView(R.id.edtMax)
    EditText dialogMax;
    private SharedPreferences sharedPreferences;
    private BarcodeReader reader;
    private Dialog dialog;
    private Peripheral peripheral = null;
    private SymSettingState mState = SymSettingState.NONE;
    private Context context;
    private final BarcodeListener barcodeListener = new BarcodeListener() {

        @Override
        public void onFirmware(RESULT res, String s) {
            Log.d(TAG, "onFirmware " + res);
            log("Firmware : " + (s == null ? "null" : s));
        }

        @Override
        public void onScan(RESULT res, ScanResult data) {
            Log.d(TAG, "onScan " + res);
            log(data == null ? "null" : data.toString() + ", " + res);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateScanButton();
                }
            }, 10);
        }

        @Override
        public void onOpened(RESULT res) {
            Toast.makeText(getContext(), res.toString(), Toast.LENGTH_SHORT).show();
            updateSpinnerParam();
            updateOpenBtn();
        }

        @Override
        public void onParameterAvailable(RESULT res, Parameter param) {
            if (res != RESULT.OK) {
                log("Get parameter : " + res);
            } else {
                log("Get parameter : " + param);
            }
        }

        @Override
        public void onSymbolSettingAvailable(RESULT res, SymbolSetting setting) {
            if (mState == SymSettingState.SET) {
                showSetSymDialog(setting);
            } else {
                log("onSymbolSettingAvailable : " + res.toString() + ", " + setting.toString());
            }
        }

        @Override
        public void onAllSymbolSettingsAvailable(RESULT res, Collection<SymbolSetting> list) {
            log("onAllSymbolSettingsAvailable : " + res.toString() + ", " + list.size());
            for (SymbolSetting s : list) {
                log(s.toString());
            }
        }

        @Override
        public void onSettingsSaved(RESULT res) {
            Toast.makeText(getContext(), "Setting saved " + res, Toast.LENGTH_SHORT).show();
        }

    };
    private final InstanceListener<BarcodeReader> instanceListener = new InstanceListener<BarcodeReader>() {

        @Override
        public void onCreated(BarcodeReader instance) {
            Log.d(TAG, "onCreated " + instance);
            reader = instance;
            if (instance == null) {
                enableView(false);
                log("No reader available");
            } else {
                power(true);
            }
        }

        @Override
        public void onDisposed(BarcodeReader instance) {
            Log.d(TAG, "onDisposed " + instance);
            reader = null;
            enableView(false);
        }

    };
    private final PowerListener powerListener = new PowerListener() {
        @Override
        public void onPowerUp(RESULT result, Peripheral peripheral) {
            if(RESULT.OK == result) {
                enableView(true);
            }
        }

        @Override
        public void onPowerDown(RESULT result, Peripheral peripheral) {
            if(RESULT.OK == result) {
                enableView(false);
            }
        }
    };

    public BarcodeFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_barcode, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        context = getContext();

        btnOpen.setEnabled(false); //will be enabled with the camera permission
        txtLog.setMovementMethod(new ScrollingMovementMethod());

        //permission
        requestCameraPermission();

        super.onViewCreated(view, savedInstanceState);
    }

    private void requestCameraPermission(){
        Context context = getContext();
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(context,
                                                                    Manifest.permission.CAMERA);
            /*
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                btnOpen.setEnabled(true);
            } else {
                btnOpen.setEnabled(false);
                */
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                               MY_PERMISSIONS_REQUEST_CAMERA);
            //}
        }
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        super.onAttach(context);
    }

    //@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btnOpen.setEnabled(true);
            } else {
                btnOpen.setEnabled(false);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        PowerManager.get().registerListener(powerListener);
        BarcodeReader.ServiceManager.stopService(context);
        updateOpenBtn();
        setUpReader();
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        close();
        power(false);
        BarcodeReader.ServiceManager.startService(context);
        PowerManager.get().releaseAndUnregister();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (dialog != null) {
            dialog.dismiss();
        }
        super.onDestroy();
    }

    @OnClick(R.id.btnOpen)
    void onOpen() {
        if (reader != null && reader.isOpened()) {
            close();
        } else {
            open();
        }
    }

    @OnClick(R.id.btnScan)
    void onScan() {
        if (reader.isScanning()) {
            abortScan();
        } else {
            scan();
        }
    }

    @OnClick(R.id.btnFirm)
    void getFirmware() {
        RESULT res = reader.getFirmware();
        showResError(res);
    }

    @OnClick(R.id.btnGetParam)
    void getParam() {
        ParamType type = (ParamType) spinnerSetParam.getSelectedItem();
        if (type != null) {
            RESULT res = reader.getParameter(type, checkGetParam.isChecked());
            showResError(res);
        }
    }

    @OnClick(R.id.btnSetParam)
    void setParam() {
        ParamType type = (ParamType) spinnerSetParam.getSelectedItem();
        String value = edtSetParam.getText().toString();
        if (type != null && !value.isEmpty()) {
            try {
                int val = Integer.parseInt(value);
                Parameter parameter = new Parameter();
                parameter.set(type, val);
                RESULT res = reader.setParameter(parameter);
                showResError(res);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Wrong value format", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "nothing to set", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btnGetSym)
    void getSym() {
        String symName = (String) spinnerGetSym.getSelectedItem();
        getSym(symName, SymSettingState.GET);
    }

    @OnClick(R.id.btnGetAllSym)
    void getAllSym() {
        RESULT res = reader.getAllSymbolSettings(checkGetAllSym.isChecked());
        showResError(res);
    }

    @OnClick(R.id.btnSetSym)
    void setSym() {
        String symName = (String) spinnerSetSym.getSelectedItem();
        getSym(symName, SymSettingState.SET);
    }

    @OnClick(R.id.btnClear)
    void clear() {
        txtLog.setText("");
    }

    private void setUpReader() {
        BarcodeFactory factory = BarcodeFactory.get().setBarcodeListener(barcodeListener);
        if (sharedPreferences.contains(SettingsActivity.KEY_BAUDRATE)) {
            String bdt = sharedPreferences.getString(SettingsActivity.KEY_BAUDRATE, "9600");
            factory.setBaudrate(Integer.parseInt(bdt));
        }
        if (sharedPreferences.contains(SettingsActivity.KEY_PORT)) {
            factory.setPort(sharedPreferences.getString(SettingsActivity.KEY_PORT, ""));
        }
        if (sharedPreferences.contains(SettingsActivity.KEY_TYPE)) {
            factory.setType(SettingsActivity.barcodeSettingToBarcodeType(
                sharedPreferences.getString(SettingsActivity.KEY_TYPE,
                                            SettingsActivity.TYPE_NONE)));
        }
        //Override previous setting if connector is checked
        if (sharedPreferences.getBoolean(SettingsActivity.KEY_USE_CONNECTOR, false)) {
            factory.setType(BarcodeReaderType.CONNECTOR);
        }
        // init power
        peripheral = getPeripheralFromReaderFactory(factory);
        if (!factory.build(getContext(), instanceListener)) {
            Toast.makeText(getContext(), "No reader available", Toast.LENGTH_SHORT).show();
            enableView(false);
        }
    }

    private void enableView(boolean enable) {
        btnOpen.setEnabled(enable);
        btnFirm.setEnabled(enable);
        btnScan.setEnabled(enable);
        btnGetParam.setEnabled(enable);
        btnSetParam.setEnabled(enable);
        btnGetSym.setEnabled(enable);
        btnSetSym.setEnabled(enable);
        edtSetParam.setEnabled(enable);
        spinnerSetParam.setEnabled(enable);
        spinnerGetParam.setEnabled(enable);
        spinnerSetSym.setEnabled(enable);
        spinnerGetSym.setEnabled(enable);
    }

    private void enableDialogView(boolean enable) {
        if(dialogPrefix != null && dialogSuffix != null && dialogMin != null && dialogMax != null) {
            dialogPrefix.setEnabled(enable);
            dialogSuffix.setEnabled(enable);
            dialogMin.setEnabled(enable);
            dialogMax.setEnabled(enable);
        }
    }

    private void open() {
        reader.open();
    }

    private void close() {
        Log.d(TAG, "close");
        if (reader != null && reader.isOpened()) {
            reader.close();
        }
        updateOpenBtn();
        updateScanButton();
    }

    private void showResError(RESULT res) {
        if (res != RESULT.OK) {
            Toast.makeText(getContext(), res.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getSym(String symName, SymSettingState state) {
        if (symName != null) {
            Symbol s = getSymbolByName(symName);
            if (s != null) {
                mState = state;
                RESULT res = reader
                    .getSymbolSetting(s, checkGetSym.isChecked() && state == SymSettingState.GET);
                showResError(res);
            } else {
                Toast.makeText(getContext(), "No symbol found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "No symbol selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void scan() {
        RESULT res = reader.scan();
        showResError(res);
        updateScanButton();
    }

    private void abortScan() {
        RESULT res = reader.abortScan();
        showResError(res);
        updateScanButton();
    }

    private void updateOpenBtn() {
        if (reader == null || !reader.isOpened()) {
            btnOpen.setText(R.string.open);
        } else {
            btnOpen.setText(R.string.close);
        }
    }

    private void updateScanButton() {
        if (reader != null && reader.isScanning()) {
            btnScan.setText(R.string.abort_scan);
        } else {
            btnScan.setText(R.string.scan);
        }
    }

    private void updateSpinnerParam() {
        List<ParamType> list = new ArrayList<>(reader.getSupportedParameters());
        ArrayAdapter<ParamType> adapter = new ArrayAdapter<>(getContext(),
                                                             android.R.layout.simple_spinner_item,
                                                             list);
        adapter.sort(new Comparator<ParamType>() {
            @Override
            public int compare(ParamType o1, ParamType o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        spinnerSetParam.setAdapter(adapter);
        spinnerGetParam.setAdapter(adapter);

        List<String> symbolNames = new ArrayList<>();
        for (Symbol s : reader.getSupportedSymbols()) {
            symbolNames.add(s.getName());
        }
        ArrayAdapter<String> symbolAdapter = new ArrayAdapter<>(getContext(),
                                                                android.R.layout.simple_spinner_item,
                                                                symbolNames);
        symbolAdapter.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        spinnerGetSym.setAdapter(symbolAdapter);
        spinnerSetSym.setAdapter(symbolAdapter);
    }

    private void log(CharSequence s) {
        Log.i(TAG, s.toString());
        txtLog.append(s + "\n");
    }

    private Peripheral getPeripheralFromReaderFactory(BarcodeFactory bf){
        if(CpcOs.isConeK()){
            if(bf.getType() == BarcodeReaderType.OPTICON_MDI3100) {
                peripheral = ConePeripheral.BARCODE_OPTICON_MDI3100_GPIO;
            } else if (bf.getType() == BarcodeReaderType.HONEYWELL_N6603_DECODED){
                peripheral = ConePeripheral.BARCODE_HONEYWELL_N6603DECODED_GPIO;
            }
        } else if (CpcOs.isIdPlatform()){
            peripheral = IdPlatformPeripheral.BARCODE;
        } else if (CpcOs.isCizi()){
            peripheral = CiziPeripheral.BARCODE_OPTICON_MDI3100_GPIO;
        }
        return peripheral;
    }

    private void power(boolean b) {
        Context context = getContext();
        if(context != null && peripheral != null) {
            PowerManager.get().power(context, peripheral, b);
        } else {
            //FIXME waiting for new version of CpcCore that provides default peripheral.
            powerListener.onPowerUp(RESULT.OK, peripheral);
        }
    }

    private Symbol getSymbolByName(String name) {
        for (Symbol s : reader.getSupportedSymbols()) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        return null;
    }

    private void showSetSymDialog(final SymbolSetting s) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getContext());
        builder.title(s.getSymbol().getName())
            .customView(R.layout.dialog, true)
            .negativeText(android.R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog materialDialog,
                                    @NonNull DialogAction dialogAction) {
                    configureSymbol(s);
                }
            })
            .positiveText(android.R.string.ok);

        MaterialDialog d = builder.build();
        onDialogViewCreated(d.getCustomView(), s);
        dialog = d;
        dialog.show();
    }

    private void onDialogViewCreated(View v, SymbolSetting setting) {
        ButterKnife.bind(this, v);
        dialogSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableDialogView(isChecked);
            }
        });
        enableDialogView(dialogSwitch.isChecked());

        if (setting != null) {
            dialogSwitch.setChecked(setting.isEnabled());
            dialogPrefix.setText(CpcBytes.byteArrayToUtf8String(setting.getPrefix()));
            dialogSuffix.setText(CpcBytes.byteArrayToUtf8String(setting.getSuffix()));
            dialogMin.setText(String.format(Locale.US, "%d", setting.getMin()));
            dialogMax.setText(String.format(Locale.US, "%d", setting.getMax()));
        }
    }

    private void configureSymbol(SymbolSetting current) {
        SymbolSettingDiff diff = new SymbolSettingDiff();
        diff.set(SettingParam.ENABLE, dialogSwitch.isChecked());
        diff.set(SettingParam.PREFIX, dialogPrefix.getText().toString().getBytes());
        diff.set(SettingParam.SUFFIX, dialogSuffix.getText().toString().getBytes());

        try {
            diff.set(SettingParam.MIN, Integer.parseInt(dialogMin.getText().toString()));
        } catch (NumberFormatException ignore) {

        }
        try {
            diff.set(SettingParam.MAX, Integer.parseInt(dialogMax.getText().toString()));
        } catch (NumberFormatException ignore) {

        }
        RESULT res = reader.setSymbolSetting(current.getSymbol(), diff);
        showResError(res);
    }

    // ********** Instance Listener ********** //

    private enum SymSettingState {
        NONE,
        SET,
        GET,
    }
}
