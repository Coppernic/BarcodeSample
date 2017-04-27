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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import fr.coppernic.cpcframework.cpcpowermgmt.PowerMgmt;
import fr.coppernic.cpcframework.cpcpowermgmt.PowerMgmtFactory;
import fr.coppernic.sample.barcode.preferences.SettingsActivity;
import fr.coppernic.sdk.barcode.BarcodeFactory;
import fr.coppernic.sdk.barcode.BarcodeReader;
import fr.coppernic.sdk.barcode.BarcodeReader.ScanResult;
import fr.coppernic.sdk.barcode.BarcodeReaderType;
import fr.coppernic.sdk.barcode.Symbol;
import fr.coppernic.sdk.barcode.SymbolSetting;
import fr.coppernic.sdk.barcode.SymbolSetting.SettingParam;
import fr.coppernic.sdk.barcode.SymbolSettingDiff;
import fr.coppernic.sdk.barcode.core.Parameter;
import fr.coppernic.sdk.barcode.core.Parameter.ParamType;
import fr.coppernic.sdk.utils.core.CpcBytes;
import fr.coppernic.sdk.utils.core.CpcResult.RESULT;
import fr.coppernic.sdk.utils.debug.Log;
import fr.coppernic.sdk.utils.helpers.CpcOs;
import fr.coppernic.sdk.utils.io.InstanceListener;

/**
 * A placeholder fragment containing a simple view.
 */
public class BarcodeFragment extends Fragment implements BarcodeReader.BarcodeListener,
	InstanceListener<BarcodeReader> {

	private static final String TAG = "BarcodeFragment";
	private static final int MY_PERMISSIONS_REQUEST_CAMERA = 42;
	private final Handler handler = new Handler();
	private Button btnOpen;
	private Button btnFirm;
	private Button btnScan;
	private Button btnGetParam;
	private Button btnSetParam;
	private Button btnGetSym;
	private EditText edtSetParam;
	private Spinner spinnerSetParam;
	private Spinner spinnerGetParam;
	private Spinner spinnerGetSym;
	private Button btnSetSym;
	private Spinner spinnerSetSym;
	private CheckBox checkGetParam;
	private TextView txtLog;
	private SharedPreferences sharedPreferences;
	private BarcodeReader reader;
	private PowerMgmt power;
	private CheckBox checkGetSym;
	private CheckBox checkGetAllSym;
	private Dialog dialog;
	private Switch dialogSwitch;
	private EditText dialogPrefix;
	private EditText dialogSuffix;
	private EditText dialogMin;
	private EditText dialogMax;
	private SymSettingState mState = SymSettingState.NONE;

	public BarcodeFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_barcode, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		btnOpen = (Button) view.findViewById(R.id.btnOpen);
		btnOpen.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (reader != null && reader.isOpened()) {
					close();
				} else {
					open();
				}
			}
		});
		btnOpen.setEnabled(false); //will be enabled with the camera permission
		btnFirm = (Button) view.findViewById(R.id.btnFirm);
		btnFirm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getFirmware();
			}
		});
		btnScan = (Button) view.findViewById(R.id.btnScan);
		btnScan.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (reader.isScanning()) {
					abortScan();
				} else {
					scan();
				}
			}
		});
		btnGetParam = (Button) view.findViewById(R.id.btnGetParam);
		btnGetParam.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getParam();
			}
		});
		btnSetParam = (Button) view.findViewById(R.id.btnSetParam);
		btnSetParam.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setParam();
			}
		});
		btnGetSym = (Button) view.findViewById(R.id.btnGetSym);
		btnGetSym.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getSym();
			}
		});
		Button btnGetAllSym = (Button) view.findViewById(R.id.btnGetAllSym);
		btnGetAllSym.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getAllSym();
			}
		});
		btnSetSym = (Button) view.findViewById(R.id.btnSetSym);
		btnSetSym.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setSym();
			}
		});
		Button btnClear = (Button) view.findViewById(R.id.btnClear);
		btnClear.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clear();
			}
		});
		edtSetParam = (EditText) view.findViewById(R.id.txtSetParam);
		spinnerSetParam = (Spinner) view.findViewById(R.id.spinnerSetParam);
		spinnerGetParam = (Spinner) view.findViewById(R.id.spinnerGetParam);
		spinnerGetSym = (Spinner) view.findViewById(R.id.spinnerGetSym);
		spinnerSetSym = (Spinner) view.findViewById(R.id.spinnerSetSym);
		txtLog = (TextView) view.findViewById(R.id.txtLog);
		txtLog.setMovementMethod(new ScrollingMovementMethod());
		checkGetParam = (CheckBox) view.findViewById(R.id.checkGetParam);
		checkGetSym = (CheckBox) view.findViewById(R.id.checkGetSym);
		checkGetAllSym = (CheckBox) view.findViewById(R.id.checkReloadAll);

		//init
		power = null;

		//permission
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			int permissionCheck = ContextCompat.checkSelfPermission(getContext(),
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

		super.onViewCreated(view, savedInstanceState);
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
		if(requestCode == MY_PERMISSIONS_REQUEST_CAMERA){
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
		BarcodeReader.ServiceManager.stopService(getContext());
		updateOpenBtn();
		setUpReader();
		super.onStart();
	}

	@Override
	public void onStop() {
		Log.d(TAG, "onStop");
		close();
		power(false);
		BarcodeReader.ServiceManager.startService(getContext());
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

	private void setUpReader() {
		BarcodeFactory factory = BarcodeFactory.get().setBarcodeListener(this);
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
		power = getPowerMgmtFromReaderFactory(factory);
		if (!factory.build(getContext(), this)) {
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
		dialogPrefix.setEnabled(enable);
		dialogSuffix.setEnabled(enable);
		dialogMin.setEnabled(enable);
		dialogMax.setEnabled(enable);
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

	private void getFirmware() {
		RESULT res = reader.getFirmware();
		showResError(res);
	}

	private void getParam() {
		ParamType type = (ParamType) spinnerSetParam.getSelectedItem();
		if (type != null) {
			RESULT res = reader.getParameter(type, checkGetParam.isChecked());
			showResError(res);
		}
	}

	private void setParam() {
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

	private void getSym() {
		String symName = (String) spinnerGetSym.getSelectedItem();
		getSym(symName, SymSettingState.GET);
	}

	private void getAllSym(){
		RESULT res = reader.getAllSymbolSettings(checkGetAllSym.isChecked());
		showResError(res);
	}

	private void setSym() {
		String symName = (String) spinnerSetSym.getSelectedItem();
		getSym(symName, SymSettingState.SET);
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

	private void clear() {
		txtLog.setText("");
	}

	private void power(boolean b) {
		try {
			if (b) {
				power.powerOn();
			} else {
				power.powerOff();
			}
		} catch (InvalidParameterException ignore) {
			Log.w(TAG, ignore.toString());
		}
	}

	private PowerMgmt getPowerMgmtFromReaderFactory(BarcodeFactory bf) {
		PowerMgmtFactory factory = PowerMgmtFactory.get()
			.setContext(getContext())
			.setTimeToSleepAfterPowerOn(500);
		if (CpcOs.isCone()) {
			factory.setPeripheralTypes(
				fr.coppernic.cpcframework.cpcpowermgmt.cone.PowerMgmt.PeripheralTypesCone
					.BarcodeReader);
			factory.setInterfaces(
				fr.coppernic.cpcframework.cpcpowermgmt.cone.PowerMgmt.InterfacesCone.ScannerPort);
			switch (bf.getType()) {
				case OPTICON_MDI3100:
					factory.setManufacturers(
						fr.coppernic.cpcframework.cpcpowermgmt.cone.PowerMgmt.ManufacturersCone
							.Opticon);
					factory.setModels(
						fr.coppernic.cpcframework.cpcpowermgmt.cone.PowerMgmt.ModelsCone.Mdi3100);
					break;
				case HONEYWELL_N6603_DECODED:
					factory.setManufacturers(
						fr.coppernic.cpcframework.cpcpowermgmt.cone.PowerMgmt.ManufacturersCone
							.Honeywell);
					factory.setModels(
						fr.coppernic.cpcframework.cpcpowermgmt.cone.PowerMgmt.ModelsCone.n6603_decoded);
					break;
				case HONEYWELL_N6603_UNDECODED:
				case OPTICON_MDL1000:
				case NONE:
					break;
			}
		} else if (CpcOs.isCizi()) {
			factory.setPeripheralTypes(
				fr.coppernic.cpcframework.cpcpowermgmt.cizi.PowerMgmt.PeripheralTypesCizi
					.BarcodeReader);
			factory.setInterfaces(
				fr.coppernic.cpcframework.cpcpowermgmt.cizi.PowerMgmt.InterfacesCizi.ScannerPort);
			factory.setManufacturers(
				fr.coppernic.cpcframework.cpcpowermgmt.cizi.PowerMgmt.ManufacturersCizi
					.Opticon);
			factory.setModels(
				fr.coppernic.cpcframework.cpcpowermgmt.cizi.PowerMgmt.ModelsCizi.Mdi3100);
		}
		return factory.build();
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
		dialogSwitch = (Switch) v.findViewById(R.id.switchSymEnable);
		dialogPrefix = (EditText) v.findViewById(R.id.edtPrefix);
		dialogSuffix = (EditText) v.findViewById(R.id.edtSuffix);
		dialogMin = (EditText) v.findViewById(R.id.edtMin);
		dialogMax = (EditText) v.findViewById(R.id.edtMax);
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

	// ********** Barcode Listener ********** //

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
		for(SymbolSetting s : list){
			log(s.toString());
		}
	}

	@Override
	public void onSettingsSaved(RESULT res) {
		Toast.makeText(getContext(), "Setting saved " + res, Toast.LENGTH_SHORT).show();
	}

	// ********** Instance Listener ********** //

	@Override
	public void onCreated(BarcodeReader instance) {
		Log.d(TAG, "onCreated " + instance);
		reader = instance;
		if (instance == null) {
			enableView(false);
			log("No reader available");
		} else {
			power(true);
			enableView(true);
		}
	}

	@Override
	public void onDisposed(BarcodeReader instance) {
		Log.d(TAG, "onDisposed " + instance);
		reader = null;
		enableView(false);
	}

	private enum SymSettingState {
		NONE,
		SET,
		GET,
	}
}
