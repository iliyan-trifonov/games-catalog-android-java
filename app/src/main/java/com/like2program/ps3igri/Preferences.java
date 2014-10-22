package com.like2program.ps3igri;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        //TODO: hide the smooth gradient from <=1.6 devices
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        //Log.i("Preferences", "onSharedPreferenceChanged() called, key = " + key);
        if (key.equals("cacheSizePref")) {
            String valueString = sharedPreferences.getString(key, "");
            if (valueString.equals("")) valueString = "0";
            long value = Long.parseLong(valueString);
            EditTextPreference p = (EditTextPreference) findPreference(key);
            if (value < 5) {
                p.setText("5");
                Toast.makeText(getBaseContext(),
                        "Минималната стойност може да бъде 5MB!",
                        Toast.LENGTH_SHORT).show();
            } else if (value > 100) {
                p.setText("100");
                Toast.makeText(getBaseContext(),
                        "Максималната стойност може да бъде 100MB!",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (key.equals("dbSizePref")) {
            String valueString = sharedPreferences.getString(key, "");
            if (valueString.equals("")) valueString = "0";
            long value = Long.parseLong(valueString);
            EditTextPreference p = (EditTextPreference) findPreference(key);
            if (value < 100) {
                p.setText("100");
                Toast.makeText(getBaseContext(),
                        "Минималната стойност може да бъде 100!",
                        Toast.LENGTH_SHORT).show();
            } else if (value > 1000) {
                p.setText("1000");
                Toast.makeText(getBaseContext(),
                        "Максималната стойност може да бъде 1000!",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (key.equals("smoothGradientPref")) {
            restartApp();
        }
    }

    private void restartApp() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    Intent i;
                    i = getBaseContext()
                            .getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                    if (i != null) {
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    } else {
                        //TODO: show a message the program cannot be restarted automatically
                    }
                    startActivity(i);
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setTitle("Рестартиране")
                .setMessage("Приложението трябва да бъде рестартирано.\nЖелаете ли да го направите сега?\n\n" +
                        "Ако има проблем при стартирането, настройките ще бъдат нулирани.")
                .setPositiveButton("Да", dialogClickListener)
                .setNegativeButton("Не", dialogClickListener);
        try {
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}