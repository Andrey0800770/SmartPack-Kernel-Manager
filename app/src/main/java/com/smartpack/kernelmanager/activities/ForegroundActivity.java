/*
 * Copyright (C) 2020-2021 sunilpaulmathew <sunil.kde@gmail.com>
 *
 * This file is part of SmartPack Kernel Manager, which is a heavily modified version of Kernel Adiutor,
 * originally developed by Willi Ye <williye97@gmail.com>
 *
 * Both SmartPack Kernel Manager & Kernel Adiutor are free softwares: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SmartPack Kernel Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SmartPack Kernel Manager.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.smartpack.kernelmanager.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.smartpack.kernelmanager.R;
import com.smartpack.kernelmanager.fragments.tools.DetailsFragment;
import com.smartpack.kernelmanager.utils.Common;

/**
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on December 07, 2020
 */

public class ForegroundActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foreground);

        AppCompatImageButton mBack = findViewById(R.id.back);
        MaterialTextView mTitle = findViewById(R.id.title);
        MaterialCardView mCancel = findViewById(R.id.cancel);

        if (Common.getDetailsTitle() != null) {
            mTitle.setText(Common.getDetailsTitle());
        }
        mBack.setOnClickListener(v -> onBackPressed());
        mCancel.setOnClickListener(v -> onBackPressed());

        getSupportFragmentManager().beginTransaction().replace(R.id.foreground_content, new DetailsFragment()).commit();
    }

    @Override
    public void onBackPressed() {
        Common.setDetailsTitle(null);
        Common.setDetailsTxt(null);
        super.onBackPressed();
    }

}
