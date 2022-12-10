/*
 * Copyright (C) 2020 The Android Open Source Project
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

public class Const {
    // Base URL (scheme and domain) of your Headwind MDM instance
    public static String SERVER_URL = "https://demo.h-mdm.com";
    // URL path (e.g. hmdm) or empty string if Headwind MDM is installed 
    // at the root of the domain
    public static String SERVER_PATH = "";
    // Device ID strategy: "imei", "serial" or null (user selection)
    public static String DEVICE_ID_USE = "imei";
    // Assign configuration for unregistered devices
    // Place the configuration ID here or null if unregistered devices
    // shouldn't be registered
    public static String ASSIGN_CONFIG = null;
    // Assign groups for unregistered devices
    // Comma-separated group IDs here or null if groups shouldn't be assigned
    public static String ASSIGN_GROUPS = null;
    // Set to true if Headwind MDM should open WiFi settings at first start
    // (in AOSP, the provisioning app is very simple and doesn't set up WiFI
    // so it's recommended to set it to true)
    public static boolean OPEN_WIFI_SETTINGS = true;

    // These constants are only available in AOSP and not available in the SDK
    // So we determine them here
    public static String DevicePolicyManager_EXTRA_PROVISIONING_TRIGGER =
            "android.app.extra.PROVISIONING_TRIGGER";
    public static String DevicePolicyManager_ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE =
            "android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE";
    public static int DevicePolicyManager_PROVISIONING_TRIGGER_QR_CODE = 2;
    public static String Intent_ACTION_FACTORY_RESET =
            "android.intent.action.FACTORY_RESET";
    public static String Intent_EXTRA_REASON =
            "android.intent.extra.REASON";
    public static String Settings_Secure_USER_SETUP_COMPLETE =
            "user_setup_complete";
}
