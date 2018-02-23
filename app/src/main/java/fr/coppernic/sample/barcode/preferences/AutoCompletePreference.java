package fr.coppernic.sample.barcode.preferences;


import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import fr.coppernic.sample.barcode.R;

/**
 * Created on 14/03/17
 *
 * @author Bastien Paul
 */

public class AutoCompletePreference extends EditTextPreference {

    @SuppressWarnings("unused")
    private static final String TAG = "AutoCompletePreference";
    private AutoCompleteTextView mEditText = null;

    @SuppressWarnings("unused")
    public AutoCompletePreference(Context context) {
        this(context, null);
    }

    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public AutoCompletePreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextPreferenceStyle);
    }

    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public AutoCompletePreference(Context context, AttributeSet attrs,
                                  int defStyle) {
        super(context, attrs, defStyle);

        mEditText = new AutoCompleteTextView(context, attrs);
        String[] array = context.getResources().getStringArray(R.array.pref_barcode_port_list);
        ArrayAdapter<String> adapter =
            new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, array);
        mEditText.setAdapter(adapter);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // find the current EditText object
        final EditText editText = (EditText) view.findViewById(android.R.id.edit);
        // copy its layout params
        ViewGroup.LayoutParams params = editText.getLayoutParams();
        ViewGroup vg = (ViewGroup) editText.getParent();
        String curVal = editText.getText().toString();
        // remove it from the existing layout hierarchy
        vg.removeView(editText);

        mEditText.setLayoutParams(params);
        mEditText.setId(android.R.id.edit);
        mEditText.setText(curVal);

        vg.addView(mEditText);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = mEditText.getText().toString();
            if (callChangeListener(value)) {
                setText(value);
            }
        }
    }

    /**
     * again we need to override methods from the base class
     */
    public EditText getEditText() {
        return mEditText;
    }

}
