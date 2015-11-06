/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tv.settings.system;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v17.preference.LeanbackPreferenceFragment;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.app.LocalePicker;
import com.android.tv.settings.R;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LanguageFragment extends LeanbackPreferenceFragment {
    private static final String TAG = "LanguageFragment";

    private static final String LANGUAGE_RADIO_GROUP = "language";

    private final Map<String, LocalePicker.LocaleInfo> mLocaleInfoMap = new ArrayMap<>();

    // Adjust this value to keep things relatively responsive without janking animations
    private static final int LANGUAGE_SET_DELAY_MS = 500;
    private final Handler mDelayHandler = new Handler();
    private Locale mNewLocale;
    private final Runnable mSetLanguageRunnable = new Runnable() {
        @Override
        public void run() {
            LocalePicker.updateLocale(mNewLocale);
        }
    };

    public static LanguageFragment newInstance() {
        return new LanguageFragment();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final Context themedContext = getPreferenceManager().getContext();
        final PreferenceScreen screen =
                getPreferenceManager().createPreferenceScreen(themedContext);
        screen.setTitle(R.string.system_language);

        Locale currentLocale = null;
        try {
            currentLocale = ActivityManagerNative.getDefault().getConfiguration()
                    .getLocales().getPrimary();
        } catch (RemoteException e) {
            Log.e(TAG, "Could not retrieve locale", e);
        }

        final boolean isInDeveloperMode = Settings.Global.getInt(themedContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;

        final List<LocalePicker.LocaleInfo> localeInfoList =
                LocalePicker.getAllAssetLocales(themedContext, isInDeveloperMode);

        for (final LocalePicker.LocaleInfo localeInfo : localeInfoList) {
            final String languageTag = localeInfo.getLocale().toLanguageTag();
            mLocaleInfoMap.put(languageTag, localeInfo);

            final RadioPreference radioPreference = new RadioPreference(themedContext);
            radioPreference.setKey(languageTag);
            radioPreference.setPersistent(false);
            radioPreference.setTitle(localeInfo.getLabel());
            radioPreference.setRadioGroup(LANGUAGE_RADIO_GROUP);

            if (localeInfo.getLocale().equals(currentLocale)) {
                radioPreference.setChecked(true);
            }

            screen.addPreference(radioPreference);
        }

        setPreferenceScreen(screen);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RadioPreference) {
            final RadioPreference radioPreference = (RadioPreference) preference;
            radioPreference.clearOtherRadioPreferences(getPreferenceScreen());
            mNewLocale = mLocaleInfoMap.get(radioPreference.getKey()).getLocale();
            mDelayHandler.removeCallbacks(mSetLanguageRunnable);
            mDelayHandler.postDelayed(mSetLanguageRunnable, LANGUAGE_SET_DELAY_MS);
        }
        return super.onPreferenceTreeClick(preference);
    }

    private static class RadioPreference extends CheckBoxPreference {
        private String mRadioGroup;

        public RadioPreference(Context context) {
            super(context);
            setWidgetLayoutResource(R.layout.radio_preference_widget);
        }

        public String getRadioGroup() {
            return mRadioGroup;
        }

        public void setRadioGroup(String radioGroup) {
            mRadioGroup = radioGroup;
        }

        public void clearOtherRadioPreferences(PreferenceGroup preferenceGroup) {
            final int count = preferenceGroup.getPreferenceCount();
            for (int i = 0; i < count; i++) {
                final Preference p = preferenceGroup.getPreference(i);
                if (!(p instanceof RadioPreference)) {
                    continue;
                }
                final RadioPreference radioPreference = (RadioPreference) p;
                if (!TextUtils.equals(getRadioGroup(), radioPreference.getRadioGroup())) {
                    continue;
                }
                if (TextUtils.equals(getKey(), radioPreference.getKey())) {
                    continue;
                }
                radioPreference.setChecked(false);
            }
        }
    }
}