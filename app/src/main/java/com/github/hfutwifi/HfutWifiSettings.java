package com.github.hfutwifi;
/*
 * HfutWifi - A hfutwifi client for Android
 * Copyright (C) 2014 <max.c.lv@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                            ___====-_  _-====___
 *                      _--^^^#####//      \\#####^^^--_
 *                   _-^##########// (    ) \\##########^-_
 *                  -############//  |\^^/|  \\############-
 *                _/############//   (@::@)   \\############\_
 *               /#############((     \\//     ))#############\
 *              -###############\\    (oo)    //###############-
 *             -#################\\  / VV \  //#################-
 *            -###################\\/      \//###################-
 *           _#/|##########/\######(   /\   )######/\##########|\#_
 *           |/ |#/\#/\#/\/  \#/\##\  |  |  /##/\#/  \/\#/\#/\#| \|
 *           `  |/  V  V  `   V  \#\| |  | |/#/  V   '  V  V  \|  '
 *              `   `  `      `   / | |  | | \   '      '  '   '
 *                               (  | |  | |  )
 *                              __\ | |  | | /__
 *                             (vvv(VVV)(VVV)vvv)
 *
 *                              HERE BE DRAGONS
 *
 */

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import com.github.hfutwifi.database.Profile;
import com.github.hfutwifi.network.request.RequestCallback;
import com.github.hfutwifi.network.request.RequestHelper;
import com.github.hfutwifi.preferences.DropDownPreference;
import com.github.hfutwifi.preferences.NumberPickerPreference;
import com.github.hfutwifi.preferences.PasswordEditTextPreference;
import com.github.hfutwifi.preferences.SummaryEditTextPreference;
import com.github.hfutwifi.utils.Constants;
import com.github.hfutwifi.utils.ToastUtils;
import com.github.hfutwifi.utils.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static com.github.hfutwifi.HfutWifiApplication.app;

public class HfutWifiSettings extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = HfutWifiSettings.class.getSimpleName();

    private static final String[] PROXY_PREFS = {
            Constants.Key.name, Constants.Key.host,
            Constants.Key.remotePort, Constants.Key.localPort, Constants.Key.password,
            Constants.Key.method, Constants.Key.protocol, Constants.Key.obfs,
            Constants.Key.obfs_param, Constants.Key.dns, Constants.Key.protocol_param};

    private static final String[] FEATURE_PREFS = {
            //Constants.Key.route, // Constants.Key.proxyApps,
            Constants.Key.udpdns, Constants.Key.ipv6};

    private HfutWifi activity;
    //public SwitchPreference natSwitch;

    //private SwitchPreference isProxyApps;

    /**
     * Helper functions
     */
    public void updateDropDownPreference(Preference pref, String value) {
        ((DropDownPreference) pref).setValue(value);
    }

    public void updatePasswordEditTextPreference(Preference pref, String value) {
        pref.setSummary(value);
        ((PasswordEditTextPreference) pref).setText(value);
    }

    public void updateNumberPickerPreference(Preference pref, int value) {
        ((NumberPickerPreference) pref).setValue(value);
    }

    public void updateSummaryEditTextPreference(Preference pref, String value) {
        pref.setSummary(value);
        ((SummaryEditTextPreference) pref).setText(value);
    }

    public void updateSwitchPreference(Preference pref, boolean value) {
        ((SwitchPreference) pref).setChecked(value);
    }

    public void updatePreference(Preference pref, String name, Profile profile) {
        if (Constants.Key.group_name.equals(name)) {
            updateSummaryEditTextPreference(pref, profile.url_group);
        } else if (Constants.Key.name.equals(name)) {
            updateSummaryEditTextPreference(pref, profile.name);
        } else if (Constants.Key.remotePort.equals(name)) {
            updateNumberPickerPreference(pref, profile.remotePort);
        } else if (Constants.Key.localPort.equals(name)) {
            updateNumberPickerPreference(pref, profile.localPort);
        } else if (Constants.Key.password.equals(name)) {
            updatePasswordEditTextPreference(pref, profile.password);
        } else if (Constants.Key.method.equals(name)) {
            updateDropDownPreference(pref, profile.method);
        } else if (Constants.Key.protocol.equals(name)) {
            updateDropDownPreference(pref, profile.protocol);
        } else if (Constants.Key.protocol_param.equals(name)) {
            updateSummaryEditTextPreference(pref, profile.protocol_param);
        } else if (Constants.Key.obfs.equals(name)) {
            updateDropDownPreference(pref, profile.obfs);
        } else if (Constants.Key.obfs_param.equals(name)) {
            updateSummaryEditTextPreference(pref, profile.obfs_param);
        } /*else if (Constants.Key.route.equals(name)) {
            updateDropDownPreference(pref, profile.route);
        } /*else if (Constants.Key.proxyApps.equals(name)) {
            updateSwitchPreference(pref, profile.proxyApps);
        } */ else if (Constants.Key.udpdns.equals(name)) {
            updateSwitchPreference(pref, profile.udpdns);
        } else if (Constants.Key.dns.equals(name)) {
            updateSummaryEditTextPreference(pref, profile.dns);
        } else if (Constants.Key.ipv6.equals(name)) {
            updateSwitchPreference(pref, profile.ipv6);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_all);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        activity = (HfutWifi) getActivity();
        //natSwitch = (SwitchPreference) findPreference(Constants.Key.isNAT);

        findPreference(Constants.Key.name).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                profile.name = (String) value;
                return app.profileManager.updateProfile(profile);
            }
        });
        findPreference(Constants.Key.host).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final EditText HostEditText = new EditText(activity);
                HostEditText.setText(profile.host);
                new AlertDialog.Builder(activity)
                        .setTitle(getString(R.string.proxy))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                profile.host = HostEditText.getText().toString();
                                app.profileManager.updateProfile(profile);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setProfile(profile);
                            }
                        }).setView(HostEditText).create().show();
                return true;
            }
        });
        findPreference(Constants.Key.remotePort).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                profile.remotePort = (int) value;
                return app.profileManager.updateProfile(profile);
            }
        });
        findPreference(Constants.Key.localPort).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                profile.localPort = (int) value;
                return app.profileManager.updateProfile(profile);
            }
        });
        findPreference(Constants.Key.password).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                profile.password = (String) value;
                return app.profileManager.updateProfile(profile);
            }
        });
        findPreference(Constants.Key.method).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                profile.method = (String) value;
                return app.profileManager.updateProfile(profile);
            }
        });
        findPreference(Constants.Key.protocol).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                profile.protocol = (String) value;
                return app.profileManager.updateProfile(profile);
            }
        });
        findPreference(Constants.Key.protocol_param).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                profile.protocol_param = (String) value;
                return app.profileManager.updateProfile(profile);
            }
        });
        findPreference(Constants.Key.obfs).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                profile.obfs = (String) value;
                return app.profileManager.updateProfile(profile);
            }
        });
        findPreference(Constants.Key.obfs_param).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                profile.obfs_param = (String) value;
                return app.profileManager.updateProfile(profile);
            }
        });

        /*findPreference(Constants.Key.route).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, final Object value) {
                app.profileManager.updateAllProfileByString(Constants.Key.route, (String) value);
                return true;
            }
        });

         */
        /*
        isProxyApps = (SwitchPreference) findPreference(Constants.Key.proxyApps);
        isProxyApps.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(activity, AppManager.class));
                isProxyApps.setChecked(true);
                return false;
            }
        });

        isProxyApps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                return app.profileManager.updateAllProfileByBoolean("proxyApps", (boolean) value);
            }
        });

         */

        findPreference(Constants.Key.udpdns).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                return app.profileManager.updateAllProfileByBoolean("udpdns", (boolean) value);
            }
        });

        findPreference(Constants.Key.dns).

                setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object value) {
                        return app.profileManager.updateAllProfileByString(Constants.Key.dns, (String) value);
                    }
                });

        findPreference(Constants.Key.ipv6).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                return app.profileManager.updateAllProfileByBoolean("ipv6", (boolean) value);
            }
        });

        SwitchPreference switchPre = (SwitchPreference) findPreference(Constants.Key.isAutoConnect);
        switchPre.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                BootReceiver.setEnabled(activity, (boolean) value);
                return true;
            }
        });

        if (getPreferenceManager().getSharedPreferences().getBoolean(Constants.Key.isAutoConnect, false)) {
            BootReceiver.setEnabled(activity, true);
            getPreferenceManager().getSharedPreferences().edit().remove(Constants.Key.isAutoConnect).apply();
        }

        switchPre.setChecked(BootReceiver.getEnabled(activity));


        findPreference("recovery").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                app.track(TAG, "reset");
                activity.recovery();
                return true;
            }
        });

        findPreference("ignore_battery_optimization").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                app.track(TAG, "ignore_battery_optimization");
                activity.ignoreBatteryOptimization();
                return true;
            }
        });


        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                app.track(TAG, "about");
                WebView web = new WebView(activity);
                //web.loadUrl("file:///android_asset/pages/about.html");
                web.loadUrl("https://hfutwifi.tianqiraf.cn/about.html");
                web.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        } catch (Exception e) {
                            //Ignore;
                        }
                        return true;
                    }
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description,
                                                String failingUrl) {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                        web.loadUrl("file:///android_asset/pages/about.html");
                    }
                });
                new AlertDialog.Builder(activity)
                        .setTitle(String.format(Locale.ENGLISH, getString(R.string.about_title), BuildConfig.VERSION_NAME))
                        .setNegativeButton(getString(android.R.string.ok), null)
                        .setView(web)
                        .create()
                        .show();
                return true;
            }
        });

        findPreference("check_update").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                app.track(TAG, "check_update");
                activity.check_update();
                return true;
            }
        });

        findPreference("logcat").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                app.track(TAG, "logcat");
                EditText et_logcat = new EditText(activity);
                try {
                    Process logcat = Runtime.getRuntime().exec("logcat -d");
                    BufferedReader br = new BufferedReader(new InputStreamReader(logcat.getInputStream()));
                    String line = "";
                    line = br.readLine();
                    while (line != null) {
                        et_logcat.append(line);
                        et_logcat.append("\n");
                        line = br.readLine();
                    }
                    br.close();
                } catch (Exception e) {
                    // unknown failures, probably shouldn't retry
                    e.printStackTrace();
                }

                new AlertDialog.Builder(activity)
                        .setTitle("Logcat")
                        .setNegativeButton(getString(android.R.string.ok), null)
                        .setView(et_logcat)
                        .create()
                        .show();
                return true;
            }
        });
        showProfileTipDialog();
    }

    /**
     * show profile tips dialog
     */
    private void showProfileTipDialog(){
        if (app.settings.getBoolean(Constants.Key.profileTip, true)) {
            app.editor.putBoolean(Constants.Key.profileTip, false).apply();
            new AlertDialog.Builder(getActivity(), R.style.Theme_Material_Dialog_Alert)
                    .setTitle(R.string.tips_dialog)
                    .setMessage(R.string.tips_dialog_content)
                    .setPositiveButton(R.string.gotcha, null)
                    .create().show();

            //sent add_user_count to hfutwifi.tianqiraf.cn
            final OkHttpClient okHttpClient = new OkHttpClient();
            final Request request = new Request.Builder()
                    .url("https://hfutwifi.tianqiraf.cn/php/add_user_count.php")
                    .build();
            // request
            RequestHelper.instance().request(request, new RequestCallback() {
                @Override
                public void onFinished() {
                }
            });
        }
    }


    public void refreshProfile() {
        Profile profile = app.currentProfile();
        if (profile != null) {
            this.profile = profile;
        } else {
            Profile first = app.profileManager.getFirstProfile();
            if (first != null) {
                app.profileId(first.id);
                this.profile = first;
            } else {
                Profile defaultProfile = app.profileManager.createDefault();
                app.profileId(defaultProfile.id);
                this.profile = defaultProfile;
            }
        }

        //isProxyApps.setChecked(this.profile.proxyApps);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        app.settings.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    }

    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        List<String> list = new ArrayList<>();
        //list.add(Constants.Key.isNAT);
        list.addAll(Arrays.asList(PROXY_PREFS));
        list.addAll(Arrays.asList(FEATURE_PREFS));

        for (String name : list) {
            Preference pref = findPreference(name);
            if (pref != null) {
                //pref.setEnabled(enabled && (!Constants.Key.proxyApps.equals(name) || Utils.isLollipopOrAbove()));
                pref.setEnabled(enabled && (Utils.isLollipopOrAbove()));
            }
        }
    }

    public Profile profile;

    public void setProfile(Profile profile) {
        this.profile = profile;
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(PROXY_PREFS));
        list.addAll(Arrays.asList(FEATURE_PREFS));

        for (String name : list) {
            updatePreference(findPreference(name), name, profile);
        }
    }
}
