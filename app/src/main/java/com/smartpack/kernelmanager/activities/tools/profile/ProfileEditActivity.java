/*
 * Copyright (C) 2015-2016 Willi Ye <williye97@gmail.com>
 *
 * This file is part of Kernel Adiutor.
 *
 * Kernel Adiutor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kernel Adiutor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kernel Adiutor.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.smartpack.kernelmanager.activities.tools.profile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.smartpack.kernelmanager.R;
import com.smartpack.kernelmanager.activities.BaseActivity;
import com.smartpack.kernelmanager.database.tools.profiles.Profiles;
import com.smartpack.kernelmanager.fragments.RecyclerViewFragment;
import com.smartpack.kernelmanager.utils.Utils;
import com.smartpack.kernelmanager.utils.ViewUtils;
import com.smartpack.kernelmanager.views.dialog.Dialog;
import com.smartpack.kernelmanager.views.recyclerview.DescriptionView;
import com.smartpack.kernelmanager.views.recyclerview.RecyclerViewItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import in.sunilpaulmathew.sCommon.Utils.sExecutor;

/**
 * Created by willi on 15.08.16.
 */

public class ProfileEditActivity extends BaseActivity {

    public static final String POSITION_INTENT = "position";
    private static boolean sChanged;
    private int mPosition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sChanged = false;

        mPosition = getIntent().getIntExtra(POSITION_INTENT, 0);

        setContentView(R.layout.activity_fragments);
        initToolBar();

        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.edit));

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
                getFragment(), "fragment").commit();
    }

    private Fragment getFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("fragment");
        if (fragment == null) {
            fragment = ProfileEditFragment.newInstance(mPosition);
        }
        return fragment;
    }

    @Override
    public void finish() {
        if (sChanged) {
            setResult(Activity.RESULT_OK, new Intent());
        }
        sChanged = false;
        super.finish();
    }

    public static class ProfileEditFragment extends RecyclerViewFragment {

        public static ProfileEditFragment newInstance(int position) {
            Bundle args = new Bundle();
            args.putInt(POSITION_INTENT, position);
            ProfileEditFragment fragment = new ProfileEditFragment();
            fragment.setArguments(args);
            return fragment;
        }

        private Profiles mProfiles;
        private Profiles.ProfileItem mItem;

        private Dialog mDeleteDialog;

        @Override
        protected boolean showViewPager() {
            return false;
        }

        @Override
        protected void init() {
            super.init();

            if (mDeleteDialog != null) {
                mDeleteDialog.show();
            }

            if (mProfiles == null) {
                mProfiles = new Profiles(requireActivity());
            }
            if (mItem == null) {
                assert getArguments() != null;
                mItem = mProfiles.getAllProfiles().get(getArguments()
                        .getInt(POSITION_INTENT));
                if (mItem.getCommands().size() < 1) {
                    Utils.snackbar(requireActivity().findViewById(android.R.id.content), getString(R.string.profile_empty));
                    getHandler().postDelayed(() -> requireActivity().finish(),1000);
                }
            }
        }

        @Override
        protected void addItems(List<RecyclerViewItem> items) {
            load(items);
        }

        private void reload() {
            getHandler().postDelayed(() -> {
                clearItems();
                new UILoader(ProfileEditFragment.this).execute();
            }, 250);
        }

        private void load(List<RecyclerViewItem> items) {
            for (final Profiles.ProfileItem.CommandItem commandItem : mItem.getCommands()) {
                final DescriptionView descriptionView = new DescriptionView();
                descriptionView.setTitle(commandItem.getPath());
                descriptionView.setSummary(commandItem.getCommand());
                descriptionView.setOnItemClickListener(item -> {
                    mDeleteDialog = ViewUtils.dialogBuilder(getString(R.string.delete_question,
                            descriptionView.getTitle()), (dialog, which) -> {
                            }, (dialog, which) -> {
                                sChanged = true;
                                mItem.delete(commandItem);
                                mProfiles.commit();
                                reload();
                            }, dialog -> mDeleteDialog = null, getActivity());
                    mDeleteDialog.show();
                });

                items.add(descriptionView);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mProfiles = null;
            mItem = null;
        }

        private static class UILoader extends sExecutor {
            private List<RecyclerViewItem> items;
            private final WeakReference<ProfileEditFragment> mRefFragment;

            private UILoader(ProfileEditFragment fragment) {
                mRefFragment = new WeakReference<>(fragment);
            }

            @Override
            public void onPreExecute() {
                mRefFragment.get().showProgress();
            }

            @Override
            public void doInBackground() {
                items = new ArrayList<>();
                mRefFragment.get().load(items);
            }

            @Override
            public void onPostExecute() {
                ProfileEditFragment fragment = mRefFragment.get();
                for (RecyclerViewItem item : items) {
                    fragment.addItem(item);
                }
                fragment.hideProgress();
            }
        }
    }

}