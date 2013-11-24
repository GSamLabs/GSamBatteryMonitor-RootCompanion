package com.gsamlabs.bbm.rootcompanion;

import java.lang.reflect.Method;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * A simple service that a caller binds through aidl to 
 * access the battery stats on the system.  The service
 * is needed because the android service now requires
 * a privileged app (so this must run out of /system/priv-app/).
 */
public class BatteryStatsService extends Service {
    private static final String TAG = "BatteryStatsService";
    private static Method GET_STATISTICS_METHOD = null;
    private boolean mHasBatteryStatsPermission = true;
    private Object mBatteryInfoService;
    

    @Override
    public void onCreate() {
        mHasBatteryStatsPermission = (PackageManager.PERMISSION_GRANTED == getPackageManager().checkPermission("android.permission.BATTERY_STATS", getPackageName()));        
        super.onCreate();
    }
    
    /**
     * Our bound aidl interface impl.
     */
    private final IBatteryStatsService.Stub batteryStatsBinder = new IBatteryStatsService.Stub() {
        @Override
        /**
         * Retrieve the BatteryStatsImpl statistics in parcel form, which you can then
         * unparcel with the following call (likely using reflection):
         * com.android.internal.os.BatteryStatsImpl.CREATOR.createFromParcel(parcel)
         */
        public byte[] getStatistics() throws RemoteException {
            if (mBatteryInfoService == null)
            {
                String batteryServiceName = "batterystats";
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
                {
                    batteryServiceName = "batteryinfo";
                }
                try {
                    mBatteryInfoService = Class.forName("com.android.internal.app.IBatteryStats$Stub").getDeclaredMethod("asInterface", 
                            IBinder.class).invoke(null, Class.forName("android.os.ServiceManager")
                                    .getMethod("getService", String.class).invoke(null, batteryServiceName));
                } catch (Exception e) {
                    String msg = "Exception retrieving battery info service: " + e.getMessage();
                    Log.e(TAG, msg, e);
                    throw new RemoteException(msg);
                } 
            }

            if (GET_STATISTICS_METHOD == null)
            {
                try {
                    GET_STATISTICS_METHOD = Class.forName("com.android.internal.app.IBatteryStats").getMethod("getStatistics", (Class<?>[]) null);
                } catch (Exception e) {
                    String msg = "Exception obtaining getStatistics method - perhaps this version of android changed? " + e.getMessage();
                    Log.e(TAG, msg, e);
                    throw new RemoteException(msg);
                } 
            }
            try {
                return (byte[]) GET_STATISTICS_METHOD.invoke(mBatteryInfoService, (Object[]) null);
            } catch (Exception e) {
                String msg = "Exception invoking getStatistics on battery info service: " + e.getMessage();
                Log.e(TAG, msg, e);
                throw new RemoteException(msg);
            } 
        }

        @Override
        /**
         * Determine if this service has access to retrieve the battery stats.  This
         * is useful in diagnosing problems from the client end.
         */
        public boolean hasBatteryStatsPermission() throws RemoteException {
            return mHasBatteryStatsPermission;
        }
    };
    
    @Override
    public IBinder onBind(Intent intent) {
        return batteryStatsBinder;
    }
}
