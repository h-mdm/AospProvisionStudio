# Headwind MDM Provisioning Utility

Those users who don't have source code of their AOSP image, can build this project in Android Studio.

1. Get your ROM's platform keys, [prepare](https://qa.h-mdm.com/14300/) the keystore file 'platform.jks' and store it in the app/keystore directory
2. Update the Const.java as appropriate and build the 'Release' variant
3. Preinstall the Provision APK into your ROM in the /system_ext/priv-app directory
4. Place the permission file com.android.provision.xml in the /system_ext/etc/permissions directory
5. Preinstall Headwind MDM APK into your ROM in the /system/app directory

At first start, your devices should be provisioned with Headwind MDM.
