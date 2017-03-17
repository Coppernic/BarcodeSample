package fr.coppernic.sample.barcode;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import fr.coppernic.cpcframework.cpcpowermgmt.PowerMgmt;
import fr.coppernic.cpcframework.cpcpowermgmt.PowerMgmtFactory;
import fr.coppernic.sample.barcode.preferences.SettingsActivity;
import fr.coppernic.sdk.barcode.BarcodeFactory;
import fr.coppernic.sdk.barcode.BarcodeReader;
import fr.coppernic.sdk.barcode.BarcodeReader.ScanResult;
import fr.coppernic.sdk.barcode.core.Parameter;
import fr.coppernic.sdk.barcode.core.Parameter.ParamType;
import fr.coppernic.sdk.barcode.core.Symbol;
import fr.coppernic.sdk.barcode.core.SymbolSetting;
import fr.coppernic.sdk.utils.core.CpcDefinitions;
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
	private TextView txtLog;
	private SharedPreferences sharedPreferences;
	private BarcodeReader reader;
	private PowerMgmt power;

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
				scan();
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
		txtLog = (TextView) view.findViewById(R.id.txtLog);

		//init
		power = null;
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onAttach(Context context) {
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		super.onAttach(context);
	}

	@Override
	public void onStart() {
		//Close barcode service (on C-five)
		Intent intent = new Intent(CpcDefinitions.INTENT_ACTION_STOP_BARCODE_SERVICE);
		intent.setPackage(CpcOs.getSystemServicePackage(getContext()));
		getContext().startService(intent);

		updateOpenBtn();
		setUpReader();
		super.onStart();
	}

	@Override
	public void onStop() {
		close();
		super.onStop();
	}

	private void setUpReader() {
		BarcodeFactory factory = BarcodeFactory.get().setBarcodeListener(this);
		if (sharedPreferences.contains(SettingsActivity.KEY_BAUDRATE)) {
			String bdt = sharedPreferences.getString(SettingsActivity.KEY_BAUDRATE, "9600");
			factory.setBdt(Integer.parseInt(bdt));
		}
		if (sharedPreferences.contains(SettingsActivity.KEY_PORT)) {
			factory.setPort(sharedPreferences.getString(SettingsActivity.KEY_PORT, ""));
		}
		if (sharedPreferences.contains((SettingsActivity.KEY_TYPE))) {
			factory.setType(
				SettingsActivity.barcodeSettingToBarcodeType(
					sharedPreferences.getString(SettingsActivity.KEY_TYPE,
					                            SettingsActivity.TYPE_NONE)));
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
		edtSetParam.setEnabled(enable);
		spinnerSetParam.setEnabled(enable);
	}

	private void open() {
		reader.open();
	}

	private void close() {
		if (reader != null && reader.isOpened()) {
			reader.close();
		}
		updateOpenBtn();
		power(false);
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
			RESULT res = reader.getParameterValue(type, false);
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
				RESULT res = reader.setParameterValue(parameter);
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
		if (symName != null) {
			Symbol s = getSymbolByName(symName);
			if (s != null) {
				RESULT res = reader.getSymbolSetting(s, false);
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
	}

	private void updateOpenBtn() {
		if (reader == null || !reader.isOpened()) {
			btnOpen.setText(R.string.open);
		} else {
			btnOpen.setText(R.string.close);
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

	// ********** Barcode Listener ********** //

	@Override
	public void onFirmware(RESULT res, String s) {
		Log.d(TAG, "onFirmware " + res);
		log("Firmware : " + (s == null ? "null" : s));
	}

	@Override
	public void onScan(RESULT res, ScanResult data) {
		Log.d(TAG, "onScan " + res);
		log("Scan : " + (data == null ? "null" : data.dataToString()));
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
		log("onSymbolSettingAvailable : " + res.toString() + ", " + setting.toString());
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
		Log.d(TAG, "onCreated " + instance);
		reader = null;
		enableView(false);
	}
}
