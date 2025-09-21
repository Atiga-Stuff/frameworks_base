/*
 * Copyright (C) 2018 The LineageOS Project
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
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.tuner.TunerService;

import lineageos.providers.LineageSettings;

public class ClockController implements TunerService.Tunable {

    private static final String TAG = "ClockController";

    private static final String CLOCK_POSITION =
            "lineagesystem:" + LineageSettings.System.STATUS_BAR_CLOCK;

    private static final String STATUSBAR_CLOCK_CHIP =
            "system" + Settings.System.STATUSBAR_CLOCK_CHIP;

    private static final int CLOCK_POSITION_RIGHT = 0;
    private static final int CLOCK_POSITION_CENTER = 1;
    private static final int CLOCK_POSITION_LEFT = 2;

    private Context mContext;
    private Clock mActiveClock, mCenterClock, mLeftClock, mRightClock;

    private int mClockPosition = CLOCK_POSITION_LEFT;
    private boolean mBlackListed = false;
    private boolean showClockBg;

    public ClockController(Context context, View statusBar) {
        mContext = context;

        mCenterClock = statusBar.findViewById(R.id.clock_center);
        mLeftClock = statusBar.findViewById(R.id.clock);
        mRightClock = statusBar.findViewById(R.id.clock_right);

        mActiveClock = mLeftClock;

        Dependency.get(TunerService.class).addTunable(this,
                StatusBarIconController.ICON_HIDE_LIST, CLOCK_POSITION);
        Dependency.get(TunerService.class).addTunable(this, STATUSBAR_CLOCK_CHIP);
    }

    public Clock getClock() {
        switch (mClockPosition) {
            case CLOCK_POSITION_RIGHT:
                return mRightClock;
            case CLOCK_POSITION_CENTER:
                return mCenterClock;
            case CLOCK_POSITION_LEFT:
            default:
                return mLeftClock;
        }
    }

    private void updateActiveClock() {
        mActiveClock.setClockVisibleByUser(false);
        removeDarkReceiver();
        mActiveClock = getClock();
        mActiveClock.setClockVisibleByUser(true);
        addDarkReceiver();

        // Override any previous setting
        mActiveClock.setClockVisibleByUser(!mBlackListed);
        // Add background chip
        addBackgroundChip(mActiveClock);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        Log.d(TAG, "onTuningChanged key=" + key + " value=" + newValue);

        if (CLOCK_POSITION.equals(key)) {
            mClockPosition = TunerService.parseInteger(newValue, CLOCK_POSITION_LEFT);
        } else {
            mBlackListed = StatusBarIconController.getIconHideList(
                    mContext, newValue).contains("clock");
        }
        if (STATUSBAR_CLOCK_CHIP.equals(key)) {
            showClockBg = TunerService.parseIntegerSwitch(newValue, false);
        }
        updateActiveClock();
    }

    public void addDarkReceiver() {
        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(mActiveClock);
    }

    public void removeDarkReceiver() {
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(mActiveClock);
    }

    private void addBackgroundChip(View vClock) {
        if (showClockBg) {
            vClock.setBackgroundResource(R.drawable.sb_date_bg);
            vClock.setPadding(10,2,10,2);
        } else {
            int clockPaddingStart = mContext.getResources().getDimensionPixelSize(
                    R.dimen.status_bar_clock_starting_padding);
            int clockPaddingEnd = mContext.getResources().getDimensionPixelSize(
                    R.dimen.status_bar_clock_end_padding);
            int leftClockPaddingStart = mContext.getResources().getDimensionPixelSize(
                    R.dimen.status_bar_left_clock_starting_padding);
            int leftClockPaddingEnd = mContext.getResources().getDimensionPixelSize(
                    R.dimen.status_bar_left_clock_end_padding);
            if (vClock.getId() == R.id.clock) {
                vClock.setBackgroundResource(0);
                vClock.setPaddingRelative(leftClockPaddingStart, 0, leftClockPaddingEnd, 0);
            } else if (vClock.getId() == R.id.clock_center) {
                vClock.setBackgroundResource(0);
                vClock.setPaddingRelative(0,0,0,0);
            } else if (vClock.getId() == R.id.clock_right) {
                vClock.setBackgroundResource(0);
                vClock.setPaddingRelative(clockPaddingStart, 0, clockPaddingEnd, 0);
            }
        }
    }
}
