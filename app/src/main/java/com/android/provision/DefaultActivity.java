/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.provision;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.Log;

import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED;
import static com.android.provision.Utils.DEFAULT_SETTINGS_PROVISION_DO_MODE;
import static com.android.provision.Utils.SETTINGS_PROVISION_DO_MODE;
import static com.android.provision.Utils.TAG;
import static com.android.provision.Utils.getSettings;

/**
 * Application that sets the provisioned bit, like {@code SetupWizard} does.
 *
 * <p>By default, it silently provisions the device, but it can also be used to provision
 * {@code DeviceOwner}. For example, to set the {@code TestDPC} app, run the steps below:
 * <pre><code>
    adb root
    adb install PATH_TO_TESTDPC_APK
    adb shell settings put secure tmp_provision_set_do 1
    adb shell settings put secure tmp_provision_package com.afwsamples.testdpc
    adb shell settings put secure tmp_provision_receiver com.afwsamples.testdpc.DeviceAdminReceiver
    adb shell settings put secure tmp_provision_trigger 2
    adb shell rm /data/system/device_policies.xml
    adb shell settings put global device_provisioned 0
    adb shell settings put secure user_setup_complete 0
    adb shell pm enable com.android.provision
    adb shell pm enable com.android.provision/.DefaultActivity
    adb shell stop && adb shell start

    // You might also need to run:
    adb shell am start com.android.provision/.DefaultActivity

 * </code></pre>
 */
public class DefaultActivity extends Activity {

    // TODO(b/170333009): copied from ManagedProvisioning app, as they're hidden;
    private static final String PROVISION_FINALIZATION_INSIDE_SUW =
            "android.app.action.PROVISION_FINALIZATION_INSIDE_SUW";
    private static final int RESULT_CODE_PROFILE_OWNER_SET = 122;
    private static final int RESULT_CODE_DEVICE_OWNER_SET = 123;

    private static final int REQUEST_CODE_STEP1 = 42;
    private static final int REQUEST_CODE_STEP2_PO = 43;
    private static final int REQUEST_CODE_STEP2_DO = 44;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        boolean provisionDeviceOwner = getSettings(getContentResolver(), SETTINGS_PROVISION_DO_MODE,
                DEFAULT_SETTINGS_PROVISION_DO_MODE) == 1;

        if (provisionDeviceOwner) {
            provisionDeviceOwner();
            return;
        }
        finishSetup();
    }

    private void finishSetup() {
        setProvisioningState();
        disableSelfAndFinish();
    }

    private void setProvisioningState() {
        Log.i(TAG, "Setting provisioning state");
        // Add a persistent setting to allow other apps to know the device has been provisioned.
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(getContentResolver(), Const.Settings_Secure_USER_SETUP_COMPLETE, 1);
    }

    private void disableSelfAndFinish() {
        // remove this activity from the package manager.
        PackageManager pm = getPackageManager();
        ComponentName name = new ComponentName(this, DefaultActivity.class);
        Log.i(TAG, "Disabling itself (" + name + ")");
        pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        // terminate the activity.
        finish();
    }

    private void provisionDeviceOwner() {
        if (!getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
            Log.e(TAG, "Cannot set up device owner because device does not have the "
                    + PackageManager.FEATURE_DEVICE_ADMIN + " feature");
            finishSetup();
            return;
        }
        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        if (!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE)) {
            Log.e(TAG, "DeviceOwner provisioning is not allowed, most like device is already "
                    + "provisioned");
            finishSetup();
            return;
        }

        DpcInfo dpcInfo = new DpcInfo(getContentResolver());
        Intent intent = new Intent(Const.DevicePolicyManager_ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE);
        intent.putExtra(Const.DevicePolicyManager_EXTRA_PROVISIONING_TRIGGER, dpcInfo.trigger);
        intent.putExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                dpcInfo.getReceiverComponentName());
        if (dpcInfo.checkSum != null) {
            intent.putExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM, dpcInfo.checkSum);
        }
        if (dpcInfo.downloadUrl != null) {
            intent.putExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION,
                    dpcInfo.downloadUrl);
        }

        // Parameters used by Headwind MDM 
        intent.putExtra(EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, true);

	PersistableBundle adminExtras = new PersistableBundle();
	adminExtras.putString("com.hmdm.BASE_URL", Const.SERVER_URL);
	adminExtras.putString("com.hmdm.SERVER_PROJECT", Const.SERVER_PATH);
        if (Const.DEVICE_ID_USE != null) {
	    adminExtras.putString("com.hmdm.DEVICE_ID_USE", Const.DEVICE_ID_USE);
	}
	if (Const.ASSIGN_CONFIG != null) {
	    adminExtras.putString("com.hmdm.CONFIG", Const.ASSIGN_CONFIG);
	}
	if (Const.ASSIGN_GROUPS != null) {
	    adminExtras.putString("com.hmdm.GROUP", Const.ASSIGN_GROUPS);
	}
	if (Const.OPEN_WIFI_SETTINGS) {
	    adminExtras.putString("com.hmdm.OPEN_WIFI", "1");
	}
        intent.putExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, adminExtras);

        Log.i(TAG, "Provisioning device with " + dpcInfo + ". Intent: " + intent);
        startActivityForResult(intent, REQUEST_CODE_STEP1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(): request=" + requestCode + ", result="
                + resultCodeToString(resultCode) + ", data=" + data);

        switch (requestCode) {
            case REQUEST_CODE_STEP1:
                onProvisioningStep1Result(resultCode);
                break;
            case REQUEST_CODE_STEP2_PO:
            case REQUEST_CODE_STEP2_DO:
                onProvisioningStep2Result(requestCode, resultCode);
                break;
            default:
                showErrorMessage("onActivityResult(): invalid request code " + requestCode);
        }
    }

    private void onProvisioningStep1Result(int resultCode) {
        int requestCodeStep2;
        switch (resultCode) {
            case RESULT_CODE_PROFILE_OWNER_SET:
                requestCodeStep2 = REQUEST_CODE_STEP2_PO;
                break;
            case RESULT_CODE_DEVICE_OWNER_SET:
                requestCodeStep2 = REQUEST_CODE_STEP2_DO;
                break;
            default:
                factoryReset("invalid response from "
                        + Const.DevicePolicyManager_ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE + ": "
                        + resultCodeToString(resultCode));
                return;
        }
        Intent intent = new Intent(PROVISION_FINALIZATION_INSIDE_SUW)
                .addCategory(Intent.CATEGORY_DEFAULT);
        Log.i(TAG, "Finalizing DPC with " + intent);
        startActivityForResult(intent, requestCodeStep2);
    }

    private void onProvisioningStep2Result(int requestCode, int resultCode) {
        // Must set state before launching the intent that finalize the DPC, because the DPC
        // implementation might not remove the back button
        setProvisioningState();

        boolean doMode = requestCode == REQUEST_CODE_STEP2_DO;
        if (resultCode != RESULT_OK) {
            factoryReset("invalid response from " + PROVISION_FINALIZATION_INSIDE_SUW + ": "
                    + resultCodeToString(resultCode));
            return;
        }

        Log.i(TAG, (doMode ? "Device owner" : "Profile owner") + " mode provisioned!");
        disableSelfAndFinish();
    }

    private static String resultCodeToString(int resultCode)  {
        StringBuilder result = new StringBuilder();
        switch (resultCode) {
            case RESULT_OK:
                result.append("RESULT_OK");
                break;
            case RESULT_CANCELED:
                result.append("RESULT_CANCELED");
                break;
            case RESULT_FIRST_USER:
                result.append("RESULT_FIRST_USER");
                break;
            case RESULT_CODE_PROFILE_OWNER_SET:
                result.append("RESULT_CODE_PROFILE_OWNER_SET");
                break;
            case RESULT_CODE_DEVICE_OWNER_SET:
                result.append("RESULT_CODE_DEVICE_OWNER_SET");
                break;
            default:
                result.append("UNKNOWN_CODE");
        }
        return result.append('(').append(resultCode).append(')').toString();
    }

    private void showErrorMessage(String message) {
        Log.e(TAG, "Error: " + message);
    }

    private void factoryReset(String reason) {
        new AlertDialog.Builder(this)
                .setMessage("Device owner provisioning failed (" + reason
                        + ") and device must be factory reset")
                .setPositiveButton("Reset", (d, w) -> sendFactoryResetIntent(reason))
                .setOnDismissListener((d) -> sendFactoryResetIntent(reason))
                .show();
    }

    private void sendFactoryResetIntent(String reason) {
        Log.e(TAG, "Factory resetting: " + reason);
        Intent intent = new Intent(Const.Intent_ACTION_FACTORY_RESET);
        intent.setPackage("android");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(Const.Intent_EXTRA_REASON, reason);

        sendBroadcast(intent);

        // Just in case the factory reset request fails...
        finishSetup();
    }
}
