package com.github.hfutwifi.database;
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

import com.github.hfutwifi.utils.VayLog;

import java.util.ArrayList;
import java.util.List;

import static com.github.hfutwifi.HfutWifiApplication.app;

public class WifiSubManager {

    private static final String TAG = WifiSubManager.class.getSimpleName();
    private final DBHelper dbHelper;
    private List<WifiSubAddedListener> mWifiSubAddedListeners;

    public WifiSubManager(DBHelper helper) {
        this.dbHelper = helper;
        mWifiSubAddedListeners = new ArrayList<>(20);
    }

    public WifiSub createWifiSub(WifiSub p) {
        WifiSub wifisub;
        if (p == null) {
            wifisub = new WifiSub();
        } else {
            wifisub = p;
        }
        wifisub.id = 0;

        try {
            dbHelper.wifisubDao.createOrUpdate(wifisub);
            invokeWifiSubAdded(wifisub);
        } catch (Exception e) {
            VayLog.e(TAG, "addWifiSub", e);
            app.track(e);
        }
        return wifisub;
    }
    /*
    public boolean updateSSRSub(WifiSub ssrsub) {
        try {
            dbHelper.wifisubDao.update(ssrsub);
            return true;
        } catch (Exception e) {
            VayLog.e(TAG, "updateSSRSub", e);
            app.track(e);
            return false;
        }
    }

    public WifiSub getSSRSub(int id) {
        try {
            return dbHelper.wifisubDao.queryForId(id);
        } catch (Exception e) {
            VayLog.e(TAG, "getSSRSub", e);
            app.track(e);
            return null;
        }
    }
    */
    public boolean delWifiSub(int id) {
        try {
            dbHelper.wifisubDao.deleteById(id);
            return true;
        } catch (Exception e) {
            VayLog.e(TAG, "delWifiSub", e);
            app.track(e);
            return false;
        }
    }

    public WifiSub getFirstWifiSub() {
        try {
            List<WifiSub> result = dbHelper.wifisubDao.query(dbHelper.wifisubDao.queryBuilder().limit(1L).prepare());
            if (result != null && !result.isEmpty()) {
                return result.get(0);
            } else {
                return null;
            }
        } catch (Exception e) {
            VayLog.e(TAG, "getAllWifiSubs", e);
            app.track(e);
            return null;
        }
    }

    public List<WifiSub> getAllWifiSubs() {
        try {
            return dbHelper.wifisubDao.query(dbHelper.wifisubDao.queryBuilder().prepare());
        } catch (Exception e) {
            VayLog.e(TAG, "getAllWifiSubs", e);
            app.track(e);
            return null;
        }
    }

    public WifiSub createDefault() {
        WifiSub wifiSub = new WifiSub();
        wifiSub.url = "https://hfutwifi.tianqiraf.cn/subscription/rss.txt";
        wifiSub.url_group = "HfutWifi Subscription";
        return createWifiSub(wifiSub);
    }

    /**
     * add wifi sub added listener
     *
     * @param l callback
     */
    public void addWifiSubAddedListener(WifiSubAddedListener l) {
        if (mWifiSubAddedListeners == null) {
            return;
        }

        // adding listener
        if (!mWifiSubAddedListeners.contains(l)) {
            mWifiSubAddedListeners.add(l);
        }
    }

    /**
     * remove wifi sub added listener
     *
     * @param l callback
     */
    public void removeWifiSubAddedListener(WifiSubAddedListener l) {
        if (mWifiSubAddedListeners == null || mWifiSubAddedListeners.isEmpty()) {
            return;
        }

        // remove listener
        if (mWifiSubAddedListeners.contains(l)) {
            mWifiSubAddedListeners.remove(l);
        }
    }

    /**
     * invoke wifi sub added listener
     *
     * @param wifiSub wifi sub param
     */
    private void invokeWifiSubAdded(WifiSub wifiSub) {
        if (mWifiSubAddedListeners == null || mWifiSubAddedListeners.isEmpty()) {
            return;
        }

        // iteration invoke listener
        for (WifiSubAddedListener l : mWifiSubAddedListeners) {
            if (l != null) {
                l.onWifiSubAdded(wifiSub);
            }
        }
    }

    public interface WifiSubAddedListener {

        /**
         * wifi sub added
         *
         * @param wifiSub wifi sub object
         */
        void onWifiSubAdded(WifiSub wifiSub);
    }
}
