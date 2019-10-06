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

import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hfutwifi.utils.ToastUtils;
import com.github.jorgecastilloprz.FABProgressCircle;
import com.github.hfutwifi.aidl.IShadowsocksServiceCallback;
import com.github.hfutwifi.database.Profile;
import com.github.hfutwifi.database.WifiSub;
import com.github.hfutwifi.job.WifiSubUpdateJob;
import com.github.hfutwifi.network.request.RequestCallback;
import com.github.hfutwifi.network.request.RequestHelper;
import com.github.hfutwifi.utils.Constants;
import com.github.hfutwifi.utils.TrafficMonitor;
import com.github.hfutwifi.utils.Typefaces;
import com.github.hfutwifi.utils.Utils;
import com.github.hfutwifi.utils.VayLog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.lang.System;
import java.lang.reflect.Field;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import okhttp3.Request;
import okhttp3.OkHttpClient;

import static com.github.hfutwifi.HfutWifiApplication.app;

public class HfutWifi extends AppCompatActivity {

    private static final String TAG = HfutWifi.class.getSimpleName();
    private static final int REQUEST_CONNECT = 1;

    private boolean serviceStarted;
    private FloatingActionButton fab;
    private FABProgressCircle fabProgressCircle;
    private ProgressDialog progressDialog;
    private int state = Constants.State.STOPPED;

    private ServiceBoundContext mServiceBoundContext;

    private boolean isDestroyed;
    private boolean isTestConnect;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);

        mServiceBoundContext = new ServiceBoundContext(newBase) {
            @Override
            protected void onServiceConnected() {
                // Update the UI
                if (fab != null) {
                    fab.setEnabled(true);
                }

                updateState();
                /*
                if (Build.VERSION.SDK_INT >= 21 && app.isNatEnabled()) {
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.nat_deprecated, Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.switch_to_vpn, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            preferences.natSwitch.setChecked(false);
                        }
                    });
                    snackbar.show();
                }

                 */
            }

            @Override
            protected void onServiceDisconnected() {
                if (fab != null) {
                    fab.setEnabled(false);
                }
            }

            @Override
            public void binderDied() {
                detachService();
                app.crashRecovery();
                attachService();
            }
        };
    }

    /**
     * Services
     */
    private IShadowsocksServiceCallback.Stub callback = new IShadowsocksServiceCallback.Stub() {
        @Override
        public void stateChanged(final int s, String profileName, final String m) throws RemoteException {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    switch (s) {
                        case Constants.State.CONNECTING:
                            fab.setBackgroundTintList(greyTint);
                            fab.setImageResource(R.drawable.ic_start_busy);
                            fab.setEnabled(false);
                            fabProgressCircle.show();
                            preferences.setEnabled(false);
                            stat.setVisibility(View.GONE);
                            break;
                        case Constants.State.CONNECTED:
                            fab.setBackgroundTintList(greenTint);
                            if (state == Constants.State.CONNECTING) {
                                fabProgressCircle.beginFinalAnimation();
                            } else {
                                fabProgressCircle.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideCircle();
                                    }
                                }, 1000);
                            }
                            fab.setEnabled(true);
                            changeSwitch(true);
                            preferences.setEnabled(false);
                            stat.setVisibility(View.VISIBLE);
                            if (app.isNatEnabled()) {
                                connectionTestText.setVisibility(View.GONE);
                            } else {
                                connectionTestText.setVisibility(View.VISIBLE);
                                connectionTestText.setText(getString(R.string.connection_test_pending));
                            }
                            break;
                        case Constants.State.STOPPED:
                            fab.setBackgroundTintList(greyTint);
                            fabProgressCircle.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    hideCircle();
                                }
                            }, 1000);
                            fab.setEnabled(true);
                            changeSwitch(false);

                            if (!TextUtils.isEmpty(m)) {
                                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                                        String.format(Locale.ENGLISH, getString(R.string.vpn_error), m), Snackbar.LENGTH_LONG);
                                /*
                                if (getString(R.string.nat_no_root).equals(m)) {
                                    snackbar.setAction(R.string.switch_to_vpn, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            preferences.natSwitch.setChecked(false);
                                        }
                                    });
                                }

                                 */
                                snackbar.show();
                                VayLog.e(TAG, "Error to start VPN service: " + m);
                            }
                            preferences.setEnabled(true);
                            //preferences.setEnabled(false);
                            stat.setVisibility(View.GONE);
                            break;
                        case Constants.State.STOPPING:
                            fab.setBackgroundTintList(greyTint);
                            fab.setImageResource(R.drawable.ic_start_busy);
                            fab.setEnabled(false);
                            if (state == Constants.State.CONNECTED) {
                                // ignore for stopped
                                fabProgressCircle.show();
                            }
                            preferences.setEnabled(false);
                            stat.setVisibility(View.GONE);
                            break;
                        default:
                            break;
                    }
                    state = s;
                }
            });
        }

        @Override
        public void trafficUpdated(final long txRate, final long rxRate, final long txTotal, final long rxTotal) throws RemoteException {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    updateTraffic(txRate, rxRate, txTotal, rxTotal);
                }
            });
        }
    };

    public void updateTraffic(long txRate, long rxRate, long txTotal, long rxTotal) {
        txText.setText(TrafficMonitor.formatTraffic(txTotal));
        rxText.setText(TrafficMonitor.formatTraffic(rxTotal));
        txRateText.setText(TrafficMonitor.formatTraffic(txRate) + "/s");
        rxRateText.setText(TrafficMonitor.formatTraffic(rxRate) + "/s");
    }

    public void attachService() {
        mServiceBoundContext.attachService(callback);
    }

    public void detachService() {
        mServiceBoundContext.detachService();
    }

    private View stat;
    private TextView connectionTestText;
    private TextView txText;
    private TextView rxText;
    private TextView txRateText;
    private TextView rxRateText;

    private ColorStateList greyTint;
    private ColorStateList greenTint;
    private HfutWifiSettings preferences;

    public Handler handler = new Handler();

    private void changeSwitch(boolean checked) {
        serviceStarted = checked;
        int resId = checked ? R.drawable.ic_start_connected : R.drawable.ic_start_idle;
        fab.setImageResource(resId);
        if (fab.isEnabled()) {
            fab.setEnabled(false);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fab.setEnabled(true);
                }
            }, 1000);
        }
    }

    private Handler showProgress(int msg) {
        clearDialog();
        progressDialog = ProgressDialog.show(this, "", getString(msg), true, false);
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                clearDialog();
            }
        };
    }

    private void cancelStart() {
        clearDialog();
        changeSwitch(false);
    }

    private void prepareStartService() {
        if (app.isNatEnabled()) {
            serviceLoad();
        } else {
            Intent intent = VpnService.prepare(mServiceBoundContext);
            if (intent != null) {
                startActivityForResult(intent, REQUEST_CONNECT);
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onActivityResult(REQUEST_CONNECT, RESULT_OK, null);
                    }
                });
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.layout_main);
        // Initialize Toolbar
        initToolbar();

        greyTint = ContextCompat.getColorStateList(this, R.color.material_blue_grey_700);
        greenTint = ContextCompat.getColorStateList(this, R.color.material_green_700);
        preferences = (HfutWifiSettings) getFragmentManager().findFragmentById(android.R.id.content);

        stat = findViewById(R.id.stat);
        connectionTestText = (TextView) findViewById(R.id.connection_test);
        txText = (TextView) findViewById(R.id.tx);
        txRateText = (TextView) findViewById(R.id.txRate);
        rxText = (TextView) findViewById(R.id.rx);
        rxRateText = (TextView) findViewById(R.id.rxRate);
        stat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnect();
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabProgressCircle = (FABProgressCircle) findViewById(R.id.fabProgressCircle);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceStarted) {
                    serviceStop();
                } else if (mServiceBoundContext.bgService != null) {
                    prepareStartService();
                } else {
                    changeSwitch(false);
                }
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int strId = serviceStarted ? R.string.stop : R.string.connect;
                Utils.positionToast(
                        Toast.makeText(HfutWifi.this, strId, Toast.LENGTH_SHORT),
                        fab,
                        getWindow(),
                        0,
                        Utils.dpToPx(HfutWifi.this, 8))
                        .show();
                return true;
            }
        });

        updateTraffic(0, 0, 0, 0);

        // wifi sub handler
        WifiSub first = app.wifisubManager.getFirstWifiSub();
        if (first == null) {
            app.wifisubManager.createDefault();
        }
        WifiSubUpdateJob.schedule();

        // attach service
        attachService();
    }

    /**
     * init toolbar
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // non-translatable logo
        toolbar.setTitle("HfutWifi");
        toolbar.setTitleTextAppearance(toolbar.getContext(), R.style.Toolbar_Logo);
        try {
            Field field = Toolbar.class.getDeclaredField("mTitleTextView");
            field.setAccessible(true);
            TextView title = (TextView) field.get(toolbar);
            title.setFocusable(true);
            title.setGravity(0x10);
            title.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HfutWifi.this, ProfileManagerActivity.class));
                }
            });
            TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.selectableItemBackgroundBorderless});
            title.setBackgroundResource(typedArray.getResourceId(0, 0));
            typedArray.recycle();
            Typeface tf = Typefaces.get(this, "fonts/Iceland.ttf");
            if (tf != null) {
                title.setTypeface(tf);
            }
            title.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * test connect
     */
    private void testConnect() {
        // reject repeat invoke
        if (isTestConnect) {
            return;
        }

        // change flag
        isTestConnect = true;

        // display state
        connectionTestText.setText(R.string.connection_test_testing);

        // start test connect
        final long start = System.currentTimeMillis();
        RequestHelper.instance().get("https://www.hfut.edu.cn", new RequestCallback() {
            @Override
            public boolean isRequestOk(int code) {
                return code == 200 || code == 204;
            }

            @Override
            public void onSuccess(int code, String response) {
                long elapsed = System.currentTimeMillis() - start;
                String result = getString(R.string.connection_test_available, elapsed);

                if (app.isVpnEnabled()) {
                    connectionTestText.setText(result);
                }
            }

            @Override
            public void onFailed(int code, String msg) {
                String exceptionMsg = getString(R.string.connection_test_error_status_code, code);
                String result = getString(R.string.connection_test_error, exceptionMsg);

                if (app.isVpnEnabled()) {
                    connectionTestText.setText(R.string.connection_test_fail);
                    Snackbar.make(findViewById(android.R.id.content), result, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFinished() {
                isTestConnect = false;
            }
        });
    }

    private void hideCircle() {
        if (fabProgressCircle == null) {
            return;
        }

        fabProgressCircle.hide();
    }

    private void updateState() {
        updateState(true);
    }

    private void updateState(boolean resetConnectionTest) {
        if (mServiceBoundContext.bgService != null) {
            try {
                int state = mServiceBoundContext.bgService.getState();
                switch (state) {
                    case Constants.State.CONNECTING:
                        fab.setBackgroundTintList(greyTint);
                        serviceStarted = false;
                        fab.setImageResource(R.drawable.ic_start_busy);
                        preferences.setEnabled(false);
                        fabProgressCircle.show();
                        stat.setVisibility(View.GONE);
                        break;
                    case Constants.State.CONNECTED:
                        fab.setBackgroundTintList(greenTint);
                        serviceStarted = true;
                        fab.setImageResource(R.drawable.ic_start_connected);
                        preferences.setEnabled(false);
                        fabProgressCircle.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                hideCircle();
                            }
                        }, 100);
                        stat.setVisibility(View.VISIBLE);
                        if (resetConnectionTest) {
                            if (app.isNatEnabled()) {
                                connectionTestText.setVisibility(View.GONE);
                            } else {
                                connectionTestText.setVisibility(View.VISIBLE);
                                connectionTestText.setText(getString(R.string.connection_test_pending));
                            }
                        }
                        break;
                    case Constants.State.STOPPING:
                        fab.setBackgroundTintList(greyTint);
                        serviceStarted = false;
                        fab.setImageResource(R.drawable.ic_start_busy);
                        preferences.setEnabled(true);
                        fabProgressCircle.show();
                        stat.setVisibility(View.GONE);
                        break;
                    default:
                        fab.setBackgroundTintList(greyTint);
                        serviceStarted = false;
                        fab.setImageResource(R.drawable.ic_start_idle);
                        preferences.setEnabled(true);
                        fabProgressCircle.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                hideCircle();
                            }
                        }, 100);
                        stat.setVisibility(View.GONE);
                        break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean updateCurrentProfile() {
        // Check if current profile changed
        if (preferences.profile == null || app.profileId() != preferences.profile.id) {
            // updated
            Profile profile = app.currentProfile();
            if (profile == null) {
                // removed
                Profile first = app.profileManager.getFirstProfile();
                int id;
                if (first != null) {
                    id = first.id;
                } else {
                    id = app.profileManager.createDefault().id;
                }

                profile = app.switchProfile(id);
            }

            updatePreferenceScreen(profile);

            if (serviceStarted) {
                serviceLoad();
            }
            return true;
        } else {
            preferences.refreshProfile();
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.refreshContainerHolder();
        updateState(updateCurrentProfile());
    }

    private void updatePreferenceScreen(Profile profile) {
        preferences.setProfile(profile);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mServiceBoundContext.registerCallback();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mServiceBoundContext.unregisterCallback();
        clearDialog();
    }

    @Override
    public boolean isDestroyed() {
        if (Build.VERSION.SDK_INT >= 17) {
            return super.isDestroyed();
        } else {
            return isDestroyed;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        mServiceBoundContext.detachService();
        new BackupManager(this).dataChanged();
        handler.removeCallbacksAndMessages(null);
    }

    public void recovery() {
        if (serviceStarted) {
            serviceStop();
        }
        Handler h = showProgress(R.string.recovering);
        app.copyAssets();
        h.sendEmptyMessage(0);
    }

    public void check_update() {
        final ProgressDialog checkupdateProgressDialog = ProgressDialog.show(HfutWifi.this, getString(R.string.check_update), getString(R.string.check_update), false, true);
        final OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url("https://hfutwifi.tianqiraf.cn/api/update")
                .build();
        // request
        RequestHelper.instance().request(request, new RequestCallback() {
            @Override
            public boolean isRequestOk(int code) {
                return code == 204 || code == 200 || code == 304;
            }

            @Override
            public void onSuccess(int code, String response) {
                int version_code = Integer.parseInt(response);
                if(version_code > BuildConfig.VERSION_CODE)
                {
                    ToastUtils.showLong(getString(R.string.new_version));
                    WebView web = new WebView(HfutWifi.this);
                    web.loadUrl("https://hfutwifi.tianqiraf.cn/updateinfo.html");
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
                    });

                    new AlertDialog.Builder(HfutWifi.this)
                            .setTitle(String.format(Locale.ENGLISH, getString(R.string.about_title), BuildConfig.VERSION_NAME))
                            .setNegativeButton(getString(android.R.string.ok), null)
                            .setView(web)
                            .create()
                            .show();
                }
                else
                {
                    ToastUtils.showLong(getString(R.string.is_new_version));
                }
            }
            @Override
            public void onFailed(int code, String msg) {
                ToastUtils.showLong(getString(R.string.update_failed)+ msg);
            }

            @Override
            public void onFinished() {
                if(checkupdateProgressDialog!=null)
                    checkupdateProgressDialog.dismiss();
            }
        });
    }

    public boolean ignoreBatteryOptimization() {
        // TODO do . ignore_battery_optimization ......................................
        // http://blog.csdn.net/laxian2009/article/details/52474214

        boolean exception;
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                throw new Throwable("power manager get is null");
            }
            String packageName = getPackageName();
            boolean hasIgnored = true;
            if (Build.VERSION.SDK_INT >= 23) {
                hasIgnored = powerManager.isIgnoringBatteryOptimizations(packageName);
            }
            if (!hasIgnored) {
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(android.net.Uri.parse("package:" + packageName));
                startActivity(intent);
            }
            exception = false;
        } catch (Throwable e) {
            exception = true;
        }
        if (exception) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);

                ComponentName cn = new ComponentName(
                        "com.android.settings",
                        "com.android.com.settings.Settings@HighPowerApplicationsActivity"
                );

                intent.setComponent(cn);
                startActivity(intent);
                exception = false;
            } catch (Throwable e) {
                exception = true;
            }
        }
        return exception;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            serviceLoad();
        } else {
            cancelStart();
            VayLog.e(TAG, "Failed to start VpnService");
        }
    }

    private void serviceStop() {
        if (mServiceBoundContext.bgService != null) {
            try {
                mServiceBoundContext.bgService.use(-1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Called when connect button is clicked.
     */
    private void serviceLoad() {
        try {
            mServiceBoundContext.bgService.use(app.profileId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (app.isVpnEnabled()) {
            changeSwitch(false);
        }
    }

    private void clearDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            if (!isDestroyed()) {
                progressDialog.dismiss();
            }
            progressDialog = null;
        }
    }
}