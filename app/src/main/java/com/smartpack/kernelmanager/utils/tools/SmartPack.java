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

package com.smartpack.kernelmanager.utils.tools;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.smartpack.kernelmanager.activities.FlashingActivity;
import com.smartpack.kernelmanager.utils.Utils;
import com.smartpack.kernelmanager.utils.root.RootUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on November 29, 2018
 */

public class SmartPack {

    public static String mZipName;
    public static List<String> mFlashingOutput = null;
    public static StringBuilder mFlashingResult = null;
    public static boolean mFlashing = false, mMagiskModule = false, mWritableRoot = true;

    public static String getLogFolderPath(Context context) {
        return Utils.getInternalDataStorage(context) + "/logs";
    }

    public static void prepareFolder(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
        file.mkdirs();
    }

    public static long fileSize(File file) {
        return file.length();
    }

    public static void flashingTask(File file, Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                SmartPack.mFlashing = true;
                SmartPack.mZipName = file.getName();
                SmartPack.mFlashingResult = new StringBuilder();
                SmartPack.mFlashingOutput = new ArrayList<>();
                Intent flashingIntent = new Intent(context, FlashingActivity.class);
                context.startActivity(flashingIntent);
            }
            @Override
            protected Void doInBackground(Void... voids) {
                SmartPack.mFlashingResult.append("** Preparing to flash ").append(file.getName()).append("...\n\n");
                SmartPack.mFlashingResult.append("** Path: '").append(file.toString()).append("'\n\n");
                Utils.delete(context.getCacheDir() + "/flash.zip");
                SmartPack.mFlashingResult.append("** Copying '").append(file.getName()).append("' into temporary folder: ");
                SmartPack.mFlashingResult.append(RootUtils.runAndGetError("cp '" + file.toString() + "' " + context.getCacheDir() + "/flash.zip"));
                SmartPack.mFlashingResult.append(Utils.existFile(context.getCacheDir() + "/flash.zip") ? "Done *\n\n" : "\n\n");
                SmartPack.manualFlash(context);
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                SmartPack.mFlashing = false;
            }
        }.execute();
    }

    private static void manualFlash(Context context) {
        /*
         * Flashing recovery zip without rebooting to custom recovery (Credits to osm0sis @ xda-developers.com)
         * Also include code from https://github.com/topjohnwu/Magisk/
         * Ref: https://github.com/topjohnwu/Magisk/blob/a848f10bba4f840248ecf314f7c9d55511d05a0f/app/src/main/java/com/topjohnwu/magisk/core/tasks/FlashZip.kt#L47
         */
        String mScriptPath = Utils.getInternalDataStorage(context) + "/flash/META-INF/com/google/android/update-binary",
                FLASH_FOLDER = Utils.getInternalDataStorage(context) + "/flash",
                CLEANING_COMMAND = "rm -r '" + FLASH_FOLDER + "'",
                mZipPath = context.getCacheDir() + "/flash.zip";
        String flashingCommand = "BOOTMODE=true sh " + mScriptPath + " dummy 1 " + mZipPath + " 2>/dev/null && echo success";
        if (Utils.existFile(FLASH_FOLDER)) {
            RootUtils.runCommand(CLEANING_COMMAND);
        } else {
            prepareFolder(FLASH_FOLDER);
        }
        mFlashingResult.append("** Extracting ").append(mZipName).append(" into working folder: ");
        RootUtils.runAndGetError((Utils.isMagiskBinaryExist("unzip") ? Utils.magiskBusyBox() + " unzip " : "unzip ") + mZipPath + " -d '" + FLASH_FOLDER + "'");
        if (Utils.existFile(mScriptPath)) {
            mFlashingResult.append(" Done *\n\n");
            mFlashingResult.append("** Checking recovery zip file: ");
            if (Utils.readFile(mScriptPath.replace("update-binary","updater-script")).equals("#MAGISK")) {
                mFlashingResult.append(" Magisk Module *\n\n");
                mMagiskModule = true;
            } else if (Utils.existFile(Utils.getInternalDataStorage(context) + "/flash/anykernel.sh")) {
                mFlashingResult.append(" AnyKernel *\n\n");
            } else {
                mFlashingResult.append(" Unknown *\n\n");
            }
            mFlashingResult.append("** Preparing a recovery-like environment for flashing...\n\n");
            RootUtils.runCommand("cd '" + FLASH_FOLDER + "'");
            if (!mMagiskModule) {
                mFlashingResult.append("** Mounting Root filesystem: ");
                if (!RootUtils.isWritableRoot()) {
                    mWritableRoot = false;
                    mFlashingResult.append("Failed *\nPlease Note: Flashing may not work properly on this device!\n\n");
                } else {
                    mFlashingResult.append("Done *\n\n");
                    mFlashingResult.append(RootUtils.runAndGetError(Utils.isMagiskBinaryExist("mkdir") ? Utils.magiskBusyBox() + " mkdir /tmp" : "mkdir /tmp")).append(" \n");
                    mFlashingResult.append(RootUtils.runAndGetError(Utils.isMagiskBinaryExist("mke2fs") ? Utils.magiskBusyBox() + " mke2fs -F tmp.ext4 500000" : "mke2fs -F tmp.ext4 500000")).append(" \n");
                    mFlashingResult.append(RootUtils.runAndGetError(Utils.isMagiskBinaryExist("mount") ? Utils.magiskBusyBox() + " mount -o loop tmp.ext4 /tmp/" : "mount -o loop tmp.ext4 /tmp/")).append(" \n\n");
                }
            }
            mFlashingResult.append("** Flashing ").append(mZipName).append(" ...\n\n");
            RootUtils.runAndGetLiveOutput(flashingCommand, mFlashingOutput);
            mFlashingResult.append(Utils.getOutput(mFlashingOutput).endsWith("\nsuccess") ? Utils.getOutput(mFlashingOutput).replace("\nsuccess","") :
                    "Unfortunately, flashing " + mZipName + " is failed!");
        } else {
            mFlashingResult.append(" Failed *\n\n");
            mFlashingResult.append("** Flashing Failed *");
        }
        RootUtils.runCommand(CLEANING_COMMAND);
        Utils.delete(context.getCacheDir() + "/flash.zip");
        if (!mMagiskModule && mWritableRoot) {
            mFlashingResult.append("\n\n** Unmount Root filesystem: ");
            RootUtils.mount("ro", "/");
            mFlashingResult.append(" Done *");
        }
        if (mMagiskModule) {
            mMagiskModule = false;
        }
    }

}