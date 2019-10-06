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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hfutwifi.aidl.IShadowsocksServiceCallback;
import com.github.hfutwifi.database.Profile;
import com.github.hfutwifi.database.ProfileManager;
import com.github.hfutwifi.database.WifiSub;
import com.github.hfutwifi.database.WifiSubManager;
import com.github.hfutwifi.network.ping.PingCallback;
import com.github.hfutwifi.network.ping.PingHelper;
import com.github.hfutwifi.network.wifisub.SubUpdateCallback;
import com.github.hfutwifi.network.wifisub.SubUpdateHelper;
import com.github.hfutwifi.utils.Constants;
import com.github.hfutwifi.utils.Parser;
import com.github.hfutwifi.utils.ToastUtils;
import com.github.hfutwifi.utils.TrafficMonitor;
import com.github.hfutwifi.utils.Utils;
import com.github.hfutwifi.widget.UndoSnackbarManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.TaskStackBuilder;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import static com.github.hfutwifi.HfutWifiApplication.app;

public class ProfileManagerActivity extends AppCompatActivity implements View.OnClickListener, Toolbar.OnMenuItemClickListener, NfcAdapter.CreateNdefMessageCallback, ProfileManager.ProfileAddedListener, WifiSubManager.WifiSubAddedListener {

    private static final String TAG = ProfileManagerActivity.class.getSimpleName();
    private static final int MSG_FULL_TEST_FINISH = 1;

    private ProfileViewHolder selectedItem;
    private Handler handler = new Handler();

    private ProfilesAdapter profilesAdapter;
    private WifiSubAdapter wifisubAdapter;
    private UndoSnackbarManager<Profile> undoManager;

    private ClipboardManager clipboard;

    private NfcAdapter nfcAdapter;
    private byte[] nfcShareItem;
    private boolean isNfcAvailable;
    private boolean isNfcEnabled;
    private boolean isNfcBeamEnabled;

    private ProgressDialog testProgressDialog;
    private boolean isTesting;
    private GuardedProcess ssTestProcess;

    private int REQUEST_QRCODE = 1;
    private boolean is_sort = false;

    private ServiceBoundContext mServiceBoundContext;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        mServiceBoundContext = new ServiceBoundContext(newBase) {

        };
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profilesAdapter = new ProfilesAdapter();
        wifisubAdapter = new WifiSubAdapter();

        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        String action = getIntent().getAction();

        if (action != null && action.equals(Constants.Action.SORT)) {
            is_sort = true;
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.layout_profiles);

        initToolbar();

        app.profileManager.addProfileAddedListener(this);

        final RecyclerView profilesList = (RecyclerView) findViewById(R.id.profilesList);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        profilesList.setLayoutManager(layoutManager);
        profilesList.setItemAnimator(new DefaultItemAnimator());
        profilesList.setAdapter(profilesAdapter);
        profilesList.postDelayed(new Runnable() {
            @Override
            public void run() {
                // scroll to position
                int position = getCurrentProfilePosition();
                profilesList.scrollToPosition(position);
            }
        }, 100);

        undoManager = new UndoSnackbarManager<>(profilesList, new UndoSnackbarManager.OnUndoListener<Profile>() {
            @Override
            public void onUndo(SparseArray<Profile> undo) {
                profilesAdapter.undo(undo);
            }
        }, new UndoSnackbarManager.OnCommitListener<Profile>() {
            @Override
            public void onCommit(SparseArray<Profile> commit) {
                profilesAdapter.commit(commit);
            }
        });
        if(app.profileManager.getAllProfiles().size()>1) {
            if (!is_sort) {
                new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                        ItemTouchHelper.START | ItemTouchHelper.END) {
                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        int index = viewHolder.getAdapterPosition();
                        profilesAdapter.remove(index);
                        undoManager.remove(index, ((ProfileViewHolder) viewHolder).item);
                    }

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        profilesAdapter.move(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                        return true;
                    }

                }).attachToRecyclerView(profilesList);
            }
        }


        mServiceBoundContext.attachService(new IShadowsocksServiceCallback.Stub() {

            @Override
            public void stateChanged(int state, String profileName, String msg) throws RemoteException {
                // Ignored
            }

            @Override
            public void trafficUpdated(long txRate, long rxRate, long txTotal, long rxTotal) throws RemoteException {
                if (selectedItem != null) {
                    selectedItem.updateText(txTotal, rxTotal);
                }
            }
        });

        //showProfileTipDialog();

        Intent intent = getIntent();
        if (intent != null) {
            handleShareIntent(intent);
        }
    }

    /**
     * init toolbar
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.profiles);
        toolbar.setNavigationIcon(R.drawable.ic_navigation_close);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getParentActivityIntent();
                if (shouldUpRecreateTask(intent) || isTaskRoot()) {
                    TaskStackBuilder.create(ProfileManagerActivity.this).addNextIntentWithParentStack(intent).startActivities();
                } else {
                    finish();
                }
            }
        });
        toolbar.inflateMenu(R.menu.profile_manager_menu);
        toolbar.setOnMenuItemClickListener(this);
    }

    private int getCurrentProfilePosition() {
        int position = -1;
        List<Profile> profiles = profilesAdapter.profiles;
        for (int i = 0; i < profiles.size(); i++) {
            Profile profile = profiles.get(i);
            if (profile.id == app.profileId()) {
                position = i;
            }
        }
        return position;
    }

    /**
     * show profile tips dialog

    private void showProfileTipDialog() {
        if (app.settings.getBoolean(Constants.Key.profileTip, true)) {
            app.editor.putBoolean(Constants.Key.profileTip, false).apply();
            new AlertDialog.Builder(this, R.style.Theme_Material_Dialog_Alert)
                    .setTitle(R.string.profile_manager_dialog)
                    .setMessage(R.string.profile_manager_dialog_content)
                    .setPositiveButton(R.string.gotcha, null)
                    .create().show();
        }
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        //updateNfcState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleShareIntent(intent);
    }

    @Override
    public void onClick(View v) {
    }


    public void wifisubDialog() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        View view = View.inflate(this, R.layout.layout_wifi_sub, null);
        Switch subAutoUpdateEnable = (Switch) view.findViewById(R.id.sw_wifi_sub_autoupdate_enable);

        // adding listener
        app.wifisubManager.addWifiSubAddedListener(this);

        RecyclerView ssusubsList = (RecyclerView) view.findViewById(R.id.wifisubList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        ssusubsList.setLayoutManager(layoutManager);
        ssusubsList.setItemAnimator(new DefaultItemAnimator());
        ssusubsList.setAdapter(wifisubAdapter);
        /*
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                delSubDialog(viewHolder);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return true;
            }
        }).attachToRecyclerView(ssusubsList);

         */

        if (prefs.getInt(Constants.Key.wifisub_autoupdate, 0) == 1) {
            subAutoUpdateEnable.setChecked(true);
        }

        subAutoUpdateEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                if (isChecked) {
                    editor.putInt(Constants.Key.wifisub_autoupdate, 1);
                } else {
                    editor.putInt(Constants.Key.wifisub_autoupdate, 0);
                }
                editor.apply();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_profile_methods_wifi_sub))
                .setPositiveButton(R.string.wifisub_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        confirmWithUpdateSub();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                /*
                .setNeutralButton(R.string.wifisub_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showAddWifiSubAddrDialog();
                    }
                })
                 */
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // remove listener
                        app.wifisubManager.removeWifiSubAddedListener(ProfileManagerActivity.this);
                    }
                })
                .setView(view)
                .create()
                .show();
    }

    /**
     * del sub confirm dialog
     *
    private void delSubDialog(final RecyclerView.ViewHolder viewHolder) {
        final int index = viewHolder.getAdapterPosition();
        new AlertDialog.Builder(ProfileManagerActivity.this)
                .setTitle(getString(R.string.wifisub_remove_tip_title))
                .setPositiveButton(R.string.wifisub_remove_tip_direct, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        wifisubAdapter.remove(index);
                        app.wifisubManager.delWifiSub(((WifiSubViewHolder) viewHolder).item.id);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        wifisubAdapter.notifyDataSetChanged();
                    }
                })
                .setNeutralButton(R.string.wifisub_remove_tip_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String group = ((WifiSubViewHolder) viewHolder).item.url_group;
                        List<Profile> deleteProfiles = app.profileManager.getAllProfilesByGroup(group);

                        for (Profile profile : deleteProfiles) {
                            if (profile.id != app.profileId()) {
                                app.profileManager.delProfile(profile.id);
                            }
                        }

                        int index = viewHolder.getAdapterPosition();
                        wifisubAdapter.remove(index);
                        app.wifisubManager.delWifiSub(((WifiSubViewHolder) viewHolder).item.id);

                        finish();
                        startActivity(new Intent(getIntent()));
                    }
                })
                .setMessage(getString(R.string.wifisub_remove_tip))
                .setCancelable(false)
                .create()
                .show();
    }
    */
    /**
     * config with update
     */
    private void confirmWithUpdateSub() {
        testProgressDialog = ProgressDialog.show(ProfileManagerActivity.this,
                getString(R.string.wifisub_progres),
                getString(R.string.wifisub_progres_text),
                false,
                true);
        // start update sub
        List<WifiSub> subs = app.wifisubManager.getAllWifiSubs();
        SubUpdateHelper.instance().updateSub(subs, 0, new SubUpdateCallback() {
            @Override
            public void onFailed() {
                if (testProgressDialog != null) {
                    testProgressDialog.dismiss();
                }
                ToastUtils.showShort(R.string.wifisub_error);
            }

            @Override
            public void onFinished() {
                if (testProgressDialog != null) {
                    testProgressDialog.dismiss();
                }

                finish();
                startActivity(new Intent(getIntent()));
            }
        });
    }

    /*
    private void showAddWifiSubAddrDialog() {
        final EditText urlAddEdit = new EditText(ProfileManagerActivity.this);
        new AlertDialog.Builder(ProfileManagerActivity.this)
                .setTitle(getString(R.string.wifisub_add))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // add wifi sub by url
                        String subUrl = urlAddEdit.getText().toString();
                        addWifiSubByUrl(subUrl);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        wifisubDialog();
                    }
                })
                .setView(urlAddEdit)
                .create()
                .show();
    }
    */

    /**
     * add wifi sub by url
     *
     * //@param subUrl sub url
     *
    private void addWifiSubByUrl(final String subUrl) {
        if (!TextUtils.isEmpty(subUrl)) {
            // show progress dialog
            testProgressDialog = ProgressDialog.show(ProfileManagerActivity.this,
                    getString(R.string.wifisub_progres),
                    getString(R.string.wifisub_progres_text),
                    false,
                    true);

            // request sub content
            RequestHelper.instance().get(subUrl, new RequestCallback() {
                @Override
                public void onSuccess(int code, String response) {
                    WifiSub wifisub = SubUpdateHelper.parseWifiSub(subUrl, response);
                    app.wifisubManager.createWifiSub(wifisub);
                }

                @Override
                public void onFailed(int code, String msg) {
                    ToastUtils.showShort(getString(R.string.wifisub_error));
                }

                @Override
                public void onFinished() {
                    testProgressDialog.dismiss();
                    wifisubDialog();
                }
            });
        } else {
            wifisubDialog();
        }
    }
    */

    /*
    private void updateNfcState() {
        isNfcAvailable = false;
        isNfcEnabled = false;
        isNfcBeamEnabled = false;
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            isNfcAvailable = true;
            if (nfcAdapter.isEnabled()) {
                isNfcEnabled = true;
                if (nfcAdapter.isNdefPushEnabled()) {
                    isNfcBeamEnabled = true;
                    nfcAdapter.setNdefPushMessageCallback(null, ProfileManagerActivity.this);
                }
            }
        }
    }

     */

    private void handleShareIntent(Intent intent) {
        String sharedStr = null;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            sharedStr = intent.getData().toString();
        } else if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null && rawMsgs.length > 0) {
                sharedStr = new String(((NdefMessage) rawMsgs[0]).getRecords()[0].getPayload());
            }
        }

        if (TextUtils.isEmpty(sharedStr)) {
            return;
        }

        final List<Profile> profiles = Utils.mergeList(Parser.findAll(sharedStr), Parser.findAll_wifi(sharedStr));

        if (profiles.isEmpty()) {
            finish();
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.add_profile_dialog)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (Profile profile : profiles) {
                            app.profileManager.createProfile(profile);
                        }
                    }
                })
                .setNeutralButton(R.string.dr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (Profile profile : profiles) {
                            app.profileManager.createProfileDr(profile);
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setMessage(makeString(profiles, "\n"))
                .create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_QRCODE) {
            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                if (TextUtils.isEmpty(contents)) {
                    return;
                }
                final List<Profile> profiles = Utils.mergeList(Parser.findAll(contents), Parser.findAll_wifi(contents));
                if (profiles.isEmpty()) {
                    finish();
                    return;
                }
                AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Material_Dialog_Alert)
                        .setTitle(R.string.add_profile_dialog)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for (Profile profile : profiles) {
                                    app.profileManager.createProfile(profile);
                                }
                            }
                        })
                        .setNeutralButton(R.string.dr, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for (Profile profile : profiles) {
                                    app.profileManager.createProfileDr(profile);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setMessage(makeString(profiles, "\n"))
                        .create();
                dialog.show();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //handle cancel
            }
        }
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
    }

    @Override
    public void onProfileAdded(Profile profile) {
        profilesAdapter.add(profile);
    }

    @Override
    public void onWifiSubAdded(WifiSub wifiSub) {
        wifisubAdapter.add(wifiSub);
    }

    @Override
    protected void onDestroy() {
        mServiceBoundContext.detachService();

        if (ssTestProcess != null) {
            ssTestProcess.destroy();
            ssTestProcess = null;
        }

        undoManager.flush();
        app.profileManager.removeProfileAddedListener(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        return new NdefMessage(new NdefRecord[]{new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, nfcShareItem, new byte[]{}, nfcShareItem)});
    }

    /**
     * progress handler
     *
    private Handler mProgressHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FULL_TEST_FINISH:
                    if (testProgressDialog != null) {
                        testProgressDialog.dismiss();
                        testProgressDialog = null;
                    }

                    finish();
                    startActivity(new Intent(getIntent()));
                    break;
                default:
                    break;
            }
        }
    };
    */

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            /*
            case R.id.action_export:
                List<Profile> allProfiles = app.profileManager.getAllProfiles();
                if (allProfiles != null && !allProfiles.isEmpty()) {
                    clipboard.setPrimaryClip(ClipData.newPlainText(null, makeString(allProfiles, "\n")));
                    ToastUtils.showShort(R.string.action_rss);
                } else {
                    ToastUtils.showShort(R.string.action_rss_err);
                }
                return true;

             */
            /*
            case R.id.action_full_test:
                pingAll();
                return true;
             */
            case R.id.action_rss:
                wifisubDialog();
                return true;
            default:
                break;
        }
        return false;
    }
    /*
    private void pingAll() {
        // reject repeat operation
        if (isTesting) {
            return;
        }

        isTesting = true;
        testProgressDialog = ProgressDialog.show(this,
                getString(R.string.tips_testing),
                getString(R.string.tips_testing),
                false,
                true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        // Do something...
                        if (testProgressDialog != null) {
                            testProgressDialog = null;
                        }

                        isTesting = false;

                        finish();
                        startActivity(new Intent(getIntent()));
                    }
                });

        // get profile list
        List<Profile> profiles = app.profileManager.getAllProfiles();
        // start test
        PingHelper.instance().pingAll(this, profiles, new PingCallback() {

            @Override
            public void onSuccess(Profile profile, long elapsed) {
                profile.elapsed = elapsed;
                app.profileManager.updateProfile(profile);

                // set progress message
                setProgressMessage(profile.name + " " + getResultMsg());
            }

            @Override
            public void onFailed(Profile profile) {
                profile.elapsed = -1;
                app.profileManager.updateProfile(profile);

                // set progress message
                setProgressMessage(getResultMsg());
            }

            private void setProgressMessage(String message) {
                if (testProgressDialog != null) {
                    testProgressDialog.setMessage(message);
                }
            }

            @Override
            public void onFinished(Profile profile) {
                mProgressHandler.sendEmptyMessageDelayed(MSG_FULL_TEST_FINISH, 2000);
                PingHelper.instance().releaseTempActivity();
            }
        });
    }
    */

    /**
     * use string divider list value
     *
     * @param list    list
     * @param divider divider string
     * @return list is empty, return null.
     */
    public static String makeString(List<Profile> list, String divider) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Profile item = list.get(i);
            if (i > 0) {
                sb.append(divider);
            }
            sb.append(item);
        }
        return sb.toString();
    }

    private class ProfileViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnKeyListener {

        private Profile item;
        private CheckedTextView text;

        public ProfileViewHolder(View view) {
            super(view);
            text = (CheckedTextView) itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(this);
            itemView.setOnKeyListener(this);

            initPingBtn();
        }

        /**
         * init ping btn
         */
        private void initPingBtn() {
            final ImageView pingBtn = (ImageView) itemView.findViewById(R.id.ping_single);
            pingBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ProgressDialog singleTestProgressDialog = ProgressDialog.show(ProfileManagerActivity.this, getString(R.string.tips_testing), getString(R.string.tips_testing), false, true);
                    PingHelper.instance().ping(ProfileManagerActivity.this, item, new PingCallback() {
                        @Override
                        public void onSuccess(Profile profile, long elapsed) {
                            profile.elapsed = elapsed;
                            app.profileManager.updateProfile(profile);
                            updateText(0, 0, elapsed);
                        }

                        @Override
                        public void onFailed(Profile profile) {
                        }

                        @Override
                        public void onFinished(Profile profile) {
                            Snackbar.make(findViewById(android.R.id.content), getResultMsg(), Snackbar.LENGTH_LONG).show();
                            singleTestProgressDialog.dismiss();
                            PingHelper.instance().releaseTempActivity();
                        }
                    });
                }
            });

            pingBtn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Utils.positionToast(Toast.makeText(ProfileManagerActivity.this, R.string.ping, Toast.LENGTH_SHORT),
                            pingBtn,
                            getWindow(),
                            0,
                            Utils.dpToPx(ProfileManagerActivity.this, 8))
                            .show();
                    return true;
                }
            });
        }

        public void updateText() {
            updateText(0, 0);
        }

        public void updateText(long txTotal, long rxTotal) {
            updateText(txTotal, rxTotal, -1);
        }

        public void updateText(long txTotal, long rxTotal, long elapsedInput) {
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            long tx = item.tx + txTotal;
            long rx = item.rx + rxTotal;
            long elapsed = item.elapsed;
            if (elapsedInput != -1) {
                elapsed = elapsedInput;
            }
            builder.append(item.name);
            if (tx != 0 || rx != 0 || elapsed != 0 || item.url_group != "") {
                int start = builder.length();
                builder.append(getString(R.string.stat_profiles,
                        TrafficMonitor.formatTraffic(tx), TrafficMonitor.formatTraffic(rx), String.valueOf(elapsed), item.url_group));
                builder.setSpan(new TextAppearanceSpan(ProfileManagerActivity.this, android.R.style.TextAppearance_Small),
                        start + 1, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    text.setText(builder);
                }
            });
        }

        public void bind(Profile item) {
            this.item = item;
            updateText();
            if (item.id == app.profileId()) {
                text.setChecked(true);
                selectedItem = this;
            } else {
                text.setChecked(false);
                if (this.equals(selectedItem)) {
                    selectedItem = null;
                }
            }
        }

        @Override
        public void onClick(View v) {
            app.switchProfile(item.id);
            finish();
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        int index = getAdapterPosition();
                        if (index >= 0) {
                            profilesAdapter.remove(index);
                            undoManager.remove(index, item);
                            return true;
                        } else {
                            return false;
                        }
                    default:
                        return false;
                }
            } else {
                return false;
            }
        }
    }

    private class ProfilesAdapter extends RecyclerView.Adapter<ProfileViewHolder> {

        private List<Profile> profiles;

        public ProfilesAdapter() {
            if (is_sort) {
                List<Profile> list = app.profileManager.getAllProfilesByElapsed();
                if (list != null && !list.isEmpty()) {
                    profiles = list;
                }
            } else {
                List<Profile> list = app.profileManager.getAllProfiles();
                if (list != null && !list.isEmpty()) {
                    profiles = list;
                }
            }

            if (profiles == null) {
                profiles = new ArrayList<>();
            }
        }

        @Override
        public int getItemCount() {
            return profiles == null ? 0 : profiles.size();
        }

        @Override
        public void onBindViewHolder(ProfileViewHolder vh, int i) {
            vh.bind(profiles.get(i));
        }

        @Override
        public ProfileViewHolder onCreateViewHolder(ViewGroup vg, int viewType) {
            View view = LayoutInflater.from(vg.getContext()).inflate(R.layout.layout_profiles_item, vg, false);
            return new ProfileViewHolder(view);
        }

        public void add(Profile item) {
            undoManager.flush();
            int pos = getItemCount();
            profiles.add(item);
            notifyItemInserted(pos);
        }

        public void move(int from, int to) {
            undoManager.flush();
            int step = from < to ? 1 : -1;
            Profile first = profiles.get(from);
            long previousOrder = profiles.get(from).userOrder;
            for (int i = from; i < to; i += step) {
                Profile next = profiles.get(i + step);
                long order = next.userOrder;
                next.userOrder = previousOrder;
                previousOrder = order;
                profiles.set(i, next);
                app.profileManager.updateProfile(next);
            }
            first.userOrder = previousOrder;
            profiles.set(to, first);
            app.profileManager.updateProfile(first);
            notifyItemMoved(from, to);
        }

        public void remove(int pos) {
            Profile remove = profiles.remove(pos);
            app.profileManager.delProfile(remove.id);
            notifyItemRemoved(pos);

        }

        public void undo(SparseArray<Profile> actions) {
            for (int index = 0; index < actions.size(); index++) {
                Profile item = actions.get(index);
                if (item != null) {
                    profiles.add(index, item);
                    notifyItemInserted(index);
                }
            }
        }

        public void commit(SparseArray<Profile> actions) {
            for (int index = 0; index < actions.size(); index++) {
                Profile item = actions.get(index);
                if (item != null) {
                    app.profileManager.delProfile(item.id);
                    if (item.id == app.profileId()) {
                        app.profileId(-1);
                    }
                }
            }
        }
    }

    private class WifiSubViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnKeyListener {

        private WifiSub item;
        private TextView text;

        public WifiSubViewHolder(View view) {
            super(view);
            text = (TextView) itemView.findViewById(android.R.id.text2);
            itemView.setOnClickListener(this);
        }

        public void updateText() {
            updateText(false);
        }

        public void updateText(boolean isShowUrl) {
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(this.item.url_group).append("\n");
            if (isShowUrl) {
                int start = builder.length();
                builder.append(this.item.url);
                builder.setSpan(new TextAppearanceSpan(ProfileManagerActivity.this, android.R.style.TextAppearance_Small),
                        start,
                        builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    text.setText(builder);
                }
            });
        }

        public void bind(WifiSub item) {
            this.item = item;
            updateText();
        }

        @Override
        public void onClick(View v) {
            updateText(true);
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return true;
        }
    }

    private class WifiSubAdapter extends RecyclerView.Adapter<WifiSubViewHolder> {

        private List<WifiSub> profiles;

        public WifiSubAdapter() {
            List<WifiSub> all = app.wifisubManager.getAllWifiSubs();
            if (all != null && !all.isEmpty()) {
                profiles = all;
            } else {
                profiles = new ArrayList<>();
            }
        }

        @Override
        public int getItemCount() {
            return profiles == null ? 0 : profiles.size();
        }

        @Override
        public void onBindViewHolder(WifiSubViewHolder vh, int i) {
            vh.bind(profiles.get(i));
        }

        @Override
        public WifiSubViewHolder onCreateViewHolder(ViewGroup vg, int viewType) {
            View view = LayoutInflater.from(vg.getContext()).inflate(R.layout.layout_wifi_sub_item, vg, false);
            return new WifiSubViewHolder(view);
        }

        public void add(WifiSub item) {
            undoManager.flush();
            int pos = getItemCount();
            profiles.add(item);
            notifyItemInserted(pos);
        }

        public void remove(int pos) {
            profiles.remove(pos);
            notifyItemRemoved(pos);
        }
    }
}
