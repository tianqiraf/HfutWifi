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

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.Switch;

import com.github.hfutwifi.database.Profile;
import com.github.hfutwifi.utils.TaskerSettings;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.github.hfutwifi.HfutWifiApplication.app;

public class TaskerActivity extends AppCompatActivity {

    private TaskerSettings taskerOption;
    private Switch mSwitch;
    private ProfilesAdapter profilesAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_tasker);

        profilesAdapter = new ProfilesAdapter();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setNavigationIcon(R.drawable.ic_navigation_close);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        taskerOption = TaskerSettings.fromIntent(getIntent());
        mSwitch = (Switch) findViewById(R.id.serviceSwitch);
        mSwitch.setChecked(taskerOption.switchOn);
        RecyclerView profilesList = (RecyclerView) findViewById(R.id.profilesList);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        profilesList.setLayoutManager(lm);
        profilesList.setItemAnimator(new DefaultItemAnimator());
        profilesList.setAdapter(profilesAdapter);

        if (taskerOption.profileId >= 0) {
            int position = 0;
            List<Profile> profiles = profilesAdapter.profiles;
            for (int i = 0; i < profiles.size(); i++) {
                Profile profile = profiles.get(i);
                if (profile.id == taskerOption.profileId) {
                    position = i + 1;
                    break;
                }
            }
            lm.scrollToPosition(position);
        }
    }

    private class ProfileViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private Profile item;
        private CheckedTextView text;

        public ProfileViewHolder(View view) {
            super(view);
            TypedArray typedArray = obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
            view.setBackgroundResource(typedArray.getResourceId(0, 0));
            typedArray.recycle();

            text = (CheckedTextView) itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(this);
        }

        public void bindDefault() {
            item = null;
            text.setText(R.string.profile_default);
            text.setChecked(taskerOption.profileId < 0);
        }

        public void bind(Profile item) {
            this.item = item;
            text.setText(item.name);
            text.setChecked(taskerOption.profileId == item.id);
        }

        @Override
        public void onClick(View v) {
            taskerOption.switchOn = mSwitch.isChecked();
            taskerOption.profileId = item == null ? -1 : item.id;
            setResult(RESULT_OK, taskerOption.toIntent(TaskerActivity.this));
            finish();
        }
    }

    private class ProfilesAdapter extends RecyclerView.Adapter<ProfileViewHolder> {

        private List<Profile> profiles;
        private String name;

        public ProfilesAdapter() {
            profiles = app.profileManager.getAllProfiles();
            if (profiles == null) {
                profiles = new ArrayList<>();
            }

            String version = Build.VERSION.SDK_INT >= 21 ? "material" : "holo";
            name = "select_dialog_singlechoice_" + version;
        }

        public List<Profile> profiles() {
            List<Profile> allProfiles = app.profileManager.getAllProfiles();
            if (allProfiles == null) {
                return new ArrayList<>();
            } else {
                return allProfiles;
            }
        }

        @Override
        public int getItemCount() {
            return 1 + profiles().size();
        }

        @Override
        public void onBindViewHolder(ProfileViewHolder vh, int i) {
            if (i == 0) {
                vh.bindDefault();
            } else {
                vh.bind(profiles().get(i - 1));
            }
        }

        @Override
        public ProfileViewHolder onCreateViewHolder(ViewGroup vg, int i) {
            View view = LayoutInflater.from(vg.getContext())
                    .inflate(Resources.getSystem().getIdentifier(name, "layout", "android"), vg, false);
            return new ProfileViewHolder(view);
        }
    }
}
