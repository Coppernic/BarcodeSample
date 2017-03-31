package fr.coppernic.sample.barcode.preferences;


import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;

import fr.coppernic.sample.barcode.AppCompatActivity;
import fr.coppernic.sample.barcode.R;
import fr.coppernic.sdk.barcode.BarcodeReaderType;
import fr.coppernic.sdk.barcode.core.GlobalConfig;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatActivity {

	private static final String TAG = "SettingsActivity";
	public final static String KEY_TYPE = "key_barcode_reader";
	public final static String KEY_BAUDRATE = "key_barcode_bdt";
	public final static String KEY_PORT = "key_barcode_port";
	public final static String KEY_SOUND = "key_barcode_sound";
	public final static String KEY_DISPLAY = "key_barcode_display";
	public final static String KEY_CONTINUOUS = "key_barcode_continuous_mode";
	public final static String KEY_TIMEOUT_SOFT = "key_barcode_soft_timeout";

	public final static String TYPE_NONE = "-1";
	public final static String TYPE_OPTICON_MDI3100 = "0";
	public final static String TYPE_OPTICON_MDL1000 = "1";
	public final static String TYPE_HONEYWELL_N6603_DECODED = "2";
	public final static String TYPE_HONEYWELL_N6603_UNDECODED = "3";

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
		new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object value) {
				String stringValue = value.toString();

				if (preference instanceof ListPreference) {
					// For list preferences, look up the correct display value in
					// the preference's 'entries' list.
					ListPreference listPreference = (ListPreference) preference;
					int index = listPreference.findIndexOfValue(stringValue);

					// Set the summary to reflect the new value.
					preference.setSummary(
						index >= 0
						? listPreference.getEntries()[index]
						: null);

				} else {
					// For all other preferences, set the summary to the value's
					// simple string representation.
					preference.setSummary(stringValue);
				}

				GlobalConfig config = new GlobalConfig(preference.getContext());

				switch (preference.getKey()){
					case KEY_TIMEOUT_SOFT:
						config.setScanTimeoutSoft((Integer) value);
						break;
				}
				return true;
			}
		};

	private static OnPreferenceChangeListener checkboxPrefListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			Log.i(TAG, "onPreferenceChange, Key : " + preference.getKey() + " -> "
			           + newValue.getClass() + "|" + newValue);
			if(newValue instanceof Boolean){
				boolean enable = (boolean) newValue;
				GlobalConfig config = new GlobalConfig(preference.getContext());
				switch (preference.getKey()){
					case KEY_DISPLAY:
						config.enableDisplay(enable);
						break;
					case KEY_SOUND:
						config.enableSound(enable);
						break;
					case KEY_CONTINUOUS:
						config.enableContinuous(enable);
						break;
				}
			}
			return true;
		}
	};
	private static OnPreferenceChangeListener numberPrefListener =
		new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object value) {
				int val = (int) value;

				GlobalConfig config = new GlobalConfig(preference.getContext());

				switch (preference.getKey()){
					case KEY_TIMEOUT_SOFT:
						config.setScanTimeoutSoft(val);
						break;
				}
				return true;
			}
		};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener
			.onPreferenceChange(preference,
			                    PreferenceManager
				                    .getDefaultSharedPreferences(preference.getContext())
				                    .getString(preference.getKey(), ""));
	}

	private static void setCheckboxPrefChangeListener(Preference preference){
		preference.setOnPreferenceChangeListener(checkboxPrefListener);

		checkboxPrefListener.onPreferenceChange(preference,
		                                PreferenceManager.getDefaultSharedPreferences(
		                                	preference.getContext())
			                                .getBoolean(preference.getKey(), true));
	}

	private static void setNumberPickerChangeListener(Preference preference){
		preference.setOnPreferenceChangeListener(numberPrefListener);
		numberPrefListener.onPreferenceChange(preference,
		                                      PreferenceManager.getDefaultSharedPreferences(
			                                      preference.getContext())
			                                      .getInt(preference.getKey(), 3));
	}

	public static BarcodeReaderType barcodeSettingToBarcodeType(String setting) {
		BarcodeReaderType type;
		switch (setting) {
			case TYPE_OPTICON_MDI3100:
				type = BarcodeReaderType.OPTICON_MDI3100;
				break;
			case TYPE_OPTICON_MDL1000:
				type = BarcodeReaderType.OPTICON_MDL1000;
				break;
			case TYPE_HONEYWELL_N6603_DECODED:
				type = BarcodeReaderType.HONEYWELL_N6603_DECODED;
				break;
			case TYPE_HONEYWELL_N6603_UNDECODED:
				type = BarcodeReaderType.HONEYWELL_N6603_UNDECODED;
				break;
			default:
				type = BarcodeReaderType.NONE;
		}
		return type;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.settings_activity);
		setupActionBar();
		setupFragment();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == android.R.id.home) {
			onBackPressed();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			// Show the Up button in the action bar.
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	private void setupFragment() {
		getFragmentManager().beginTransaction()
			.replace(android.R.id.content, new SettingsFragment())
			.commit();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class SettingsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			setHasOptionsMenu(true);

			// Bind the summaries of EditText/List/Dialog preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference(KEY_TYPE));
			bindPreferenceSummaryToValue(findPreference(KEY_PORT));
			bindPreferenceSummaryToValue(findPreference(KEY_BAUDRATE));
			
			setNumberPickerChangeListener(findPreference(KEY_TIMEOUT_SOFT));

			setCheckboxPrefChangeListener(findPreference(KEY_DISPLAY));
			setCheckboxPrefChangeListener(findPreference(KEY_SOUND));
			setCheckboxPrefChangeListener(findPreference(KEY_CONTINUOUS));
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), SettingsActivity.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}
	}
}
