package de.danoeh.antennapod.ui.preferences.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

public class IntMaterialListPreference extends MaterialListPreference {
    public IntMaterialListPreference(Context context) {
        super(context);
    }

    public IntMaterialListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean persistString(String value) {
        return persistInt(toIntValue(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        int defaultValue = toIntValue(defaultReturnValue);
        return String.valueOf(getPersistedInt(defaultValue));
    }

    private int toIntValue(@Nullable String value) {
        if (value == null) {
            return 1;
        }
        if ("true".equalsIgnoreCase(value)) {
            return 1;
        }
        if ("false".equalsIgnoreCase(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
