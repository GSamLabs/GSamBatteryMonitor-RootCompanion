package com.gsamlabs.bbm.rootcompanion;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

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

        @Override
        /**
         * Reads the specified file into the byte buffer.  This should be used when
         * the calling program is unable to read the file for some reason (typically
         * permission denied).  The kernel wakelock (wakeup_sources) file for example.  
         * The buffer is 32K max.
         */
        public byte[] readProcFile(String fileName) throws RemoteException {
            FileInputStream is = null;
            byte[] buffer = new byte[32768];
            int len;

            try {
                is = new FileInputStream(fileName);
                len = is.read(buffer);
                is.close();
                is = null;
            } catch (java.io.IOException e) {
                if (e.getMessage().contains("EACCES"))
                {
                    // We got a permission denied error.  This happens when
                    // SELinux is set to enforcing (most Lollipop ROMs).  We
                    // Will open a shell, and simply cat the file instead.
                    final StringBuilder sb = new StringBuilder();
                    Command command = new Command(0, 3000, "cat "+fileName)
                    {
                        @Override
                        public void commandOutput(int id, String line)
                        {
                            sb.append(line).append('\n');
                            super.commandOutput(id, line);
                        }
                    };
                    boolean errorHappened = false;
                    boolean rootDenied = false;
                    try {
                        RootTools.getShell(true).add(command);
                    } catch (RootDeniedException e1)
                    {
                        errorHappened = true;
                        rootDenied = true;
                    } catch (Exception e2) {
                        errorHappened = true;
                    }
                    // Since we put a timeout on the command, we can just loop...
                    while(!command.isFinished() && !errorHappened)
                    {
                        try {
                            Thread.sleep(5);
                        } catch (Exception e1){};
                    }
                    if (errorHappened || (command.getExitCode() != 0))
                    {
                        String error = "Unable to read file: "+fileName+". "+command.getExitCode() + ": "+sb.toString();
                        Log.d(TAG, error);
                        sb.setLength(0);
                        if (rootDenied)
                        {
                            buffer = new byte[]{(byte)'R'};
                        } else if (errorHappened)
                        {
                            buffer = new byte[]{(byte)'E'};
                        } else
                        {
                            buffer = new byte[]{(byte)'E', (byte)command.getExitCode()};
                        }
                        len = buffer.length;
                    } else
                    {
                        // We're good - create the byte array & length
                        buffer = sb.toString().getBytes();
                        len = buffer.length;
                    }
                } else 
                {
                    Log.e(TAG, e.getMessage(), e);
                    buffer = new byte[]{(byte)'E'};
                    len = buffer.length;
                }
            } finally
            {
                if (is != null)
                {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                        buffer = new byte[]{(byte)'E'};
                        len = buffer.length;
                    }
                }
            }

            if (len > 0) {
                int i;
                for (i=0; i<len; i++) {
                    if (buffer[i] == '\0') {
                        len = i;
                        break;
                    }
                }
            }
            return Arrays.copyOf(buffer, len);
        }
    };
    
    @Override
    public IBinder onBind(Intent intent) {
        return batteryStatsBinder;
    }
}
