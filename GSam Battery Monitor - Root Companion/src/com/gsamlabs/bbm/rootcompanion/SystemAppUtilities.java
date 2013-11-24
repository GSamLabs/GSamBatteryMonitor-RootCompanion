package com.gsamlabs.bbm.rootcompanion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.util.Log;

import com.stericson.RootTools.RootTools;

public class SystemAppUtilities {
    private static final String TAG = "SystemAppUtilities";

    private static void checkRootAccess() throws SystemAppManagementException
    {
        if (!RootTools.isAccessGiven())
        {
            throw new SystemAppManagementException("Unable to obtain root access.  Please make sure you grant this app root authority");
        }
    }
    
 
    /**
     * Returns the full name & path to the APK, but uses an optional wild-card
     * since we don't know whether we are -1.apk, -2.apk, etc.
     * @param ctxt
     * @param includeFullPath If false, only the apk name will be returned
     * @param doWildCard If true, we'll wildcard the -X.apk part as simply *
     * @return
     */
    private static String getAPKName(Context ctxt, boolean includeFullPath, boolean doWildCard)
    {
        String fullPath = ctxt.getApplicationInfo().sourceDir;
        if (!includeFullPath)
        {
            fullPath = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        }
        if (doWildCard)
        {
            int indexOfHyphen = fullPath.lastIndexOf('-');
            if (indexOfHyphen > 0)
            {
                return fullPath.substring(0, indexOfHyphen) + "*";
            }
        }
        return fullPath;
    }
    
    public static boolean hasBatteryStatsPermission(Context ctxt)
    {
        return (PackageManager.PERMISSION_GRANTED == ctxt.getPackageManager().checkPermission("android.permission.BATTERY_STATS", ctxt.getPackageName()));
    }
    
    /**
     * Does the work to copy the apk into /system/priv-app/.  This leverages RootTools
     * to handle the heavy lifting.
     * 
     * It prompts for a reboot when done.
     * @param ctxt
     * @throws SystemAppManagementException on any error
     */
    public static void installAsSystemApp(Context ctxt) throws SystemAppManagementException
    {
        // Verify we do have root
        checkRootAccess();
        
        // Copy the file to /system/priv-app
        String privAppFile = "/system/priv-app/"+getAPKName(ctxt, false, false);
        String currentFile = getAPKName(ctxt, true, false);
        boolean copiedApp = RootTools.copyFile(currentFile, privAppFile, true, true);
        Log.d(TAG, "Used RootTools to copy app from: "+currentFile+", to: "+privAppFile+".  Was it successful? "+copiedApp);

        if (!copiedApp)
        {
            throw new SystemAppManagementException("Unable to copy the file \""+currentFile+"\" to \""+privAppFile+"\".  You may need to try this manually using a tool such as Root Explorer.");
        }

        AlertDialog.Builder bldr = new AlertDialog.Builder(ctxt);
        bldr.setMessage(R.string.main_please_reboot)
            .setTitle(R.string.main_complete_title)
            .setNeutralButton(R.string.yes, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RootTools.restartAndroid();
                }
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }
    
    public static boolean isBusyBoxInstalled()
    {
        return RootTools.isBusyboxAvailable();
    }
    
    /**
     * In general doesn't actually call SU, but may in certain cases.
     * @return
     */
    public static boolean isRootAvailable()
    {
        return RootTools.isRootAvailable();
    }

    /**
     * Does the work to uninstall the app and all associated assets.  This removes
     * the app from /system/priv-app and from /data/app/, and assets from /data/data/APP_NAME
     * This leverages RootTools to handle the heavy lifting.
     * 
     * It prompts for a reboot when done.
     * @param ctxt
     * @throws SystemAppManagementException on any error.
     */
    public static void uninstallApp(Context ctxt) throws SystemAppManagementException
    {
        // Verify we do have root
        checkRootAccess();
        
        // Delete /system/priv-app
        String privAppFile = "/system/priv-app/"+getAPKName(ctxt, false, true);
        boolean deletedPrivApp = RootTools.deleteFileOrDirectory(privAppFile, true);
        Log.d(TAG, "Used RootTools to delete app from: "+privAppFile+".  Was it successful? "+deletedPrivApp);

        // Now delete /data/app
        boolean deletedDataApp = RootTools.deleteFileOrDirectory(getAPKName(ctxt, true, false), false);
        Log.d(TAG, "Used RootTools to delete app from: "+deletedDataApp+".  Was it successful? "+deletedDataApp);

        // Now delete any files etc.
        boolean deletedDataDir = RootTools.deleteFileOrDirectory(ctxt.getApplicationInfo().dataDir, false);
        Log.d(TAG, "Used RootTools to delete data from: "+deletedDataDir+".  Was it successful? "+deletedDataDir);

        if (hasBatteryStatsPermission(ctxt) && !deletedPrivApp)
        {
            throw new SystemAppManagementException("Unable to delete the file: "+privAppFile);
        }

        AlertDialog.Builder bldr = new AlertDialog.Builder(ctxt);
        bldr.setMessage(R.string.main_please_reboot)
            .setTitle(R.string.main_complete_title)
            .setNeutralButton(R.string.yes, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RootTools.restartAndroid();
                }
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }
}
