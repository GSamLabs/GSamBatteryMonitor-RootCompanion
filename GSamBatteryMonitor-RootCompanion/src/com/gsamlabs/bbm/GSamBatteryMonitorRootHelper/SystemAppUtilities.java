package com.gsamlabs.bbm.GSamBatteryMonitorRootHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.util.Log;

import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

public class SystemAppUtilities {
    private static final String TAG = "SystemAppUtilities";

    private static final String backupScriptAssetName = "91-gsamrootcompanion_backup.sh";
    private static final String LOLLIPOP_PRIVAPP_PATH = "/system/priv-app/GSamBatteryMonitorRootHelper/GSamBatteryMonitorRootHelper.apk";

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
     * Return an appropriate path to the priv-app APK.
     * On lollipop and higher devices, this path reflects lollipop's priv-app path conventions
     * @param ctxt
     * @param doWildcard (see {@link #getAPKName(Context, boolean, boolean) getAPKName}
     * @return an android-appropriate destination for this app's "future privileged system app" self
     */
     public static String getPrivAppPath(Context ctxt, boolean doWildcard) {
        // on lollipop, the privileged system app paths have one extra directory layer preceding the APK, and they're named the package name, not a name assigned by PackageInstaller
        int majorVersionNumber = 0;
        String buildVersion = System.getProperty("ro.build.version.release"); //on LP, this would be 5.#.#
        try {
            // we're interested in the "5"
            majorVersionNumber = Integer.parseInt(buildVersion.split(".")[0]);
        } catch (NumberFormatException e) {
            // this is unlikely
        }

        if (majorVersionNumber < 5) {
            // pre-lollipop
            return "/system/priv-app/"+getAPKName(ctxt, false, doWildcard);
        }

        // post-lollipop
        return LOLLIPOP_PRIVAPP_PATH;
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

        String privAppFile = getPrivAppPath(ctxt, false);

        // Copy the file to /system/priv-app, named appropriately if on lollipop
        String currentFile = getAPKName(ctxt, true, false);
        boolean copiedApp = RootTools.copyFile(currentFile, privAppFile, true, true);
        Log.d(TAG, "Used RootTools to copy app from: "+currentFile+", to: "+privAppFile+".  Was it successful? "+copiedApp);

        if (!copiedApp)
        {
            throw new SystemAppManagementException("Unable to copy the file \""+currentFile+"\" to \""+privAppFile+"\".  You may need to try this manually using a tool such as Root Explorer.");
        }

        // Install the backup script if any...
        installBackupScript(ctxt);
        
        AlertDialog.Builder bldr = new AlertDialog.Builder(ctxt);
        bldr.setMessage(R.string.main_please_reboot)
            .setTitle(R.string.main_complete_title)
            .setNeutralButton(R.string.yes, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    rebootDevice(((Dialog)dialog).getContext());
                }
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }
    
    /**
     * On Cyanogenmod, /system/addon.d lets you place backup/restore scripts so that
     * after a nightly update, you don't have to reinstall the system app.  This is
     * very specific to cyanogenmod (AFAIK).
     * @param ctxt
     * @throws SystemAppManagementException
     */
    public static void installBackupScript(Context ctxt) throws SystemAppManagementException
    {
        File addonDir = new File("/system/addon.d");
        if (addonDir.exists() && addonDir.isDirectory())
        {
            // Verify we do have root
            checkRootAccess();
            
            // First copy the file out of our assets so we can use roottools to copy it into the filesystem.
            InputStream in = null;
            FileOutputStream fos = null;
            try {
                in = ctxt.getAssets().open(backupScriptAssetName);
                fos = ctxt.openFileOutput(backupScriptAssetName, Context.MODE_PRIVATE);                
                
                byte[] buffer = new byte[1024];
                int read;
                while((read = in.read(buffer)) != -1){
                    fos.write(buffer, 0, read);
                }            
            } catch (IOException e) 
            {
                Log.e(TAG, "Yikes - can't create asset file - this should never happen...", e);
                throw new SystemAppManagementException("Unable to install backup script - this is probably OK.  Failed creating local asset file.");
            } finally 
            {
                if (in != null)
                {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                if (fos != null)
                {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
            
            // Copy the file to /system/addon.d
            String backupFile = addonDir.getAbsolutePath() + "/"+ backupScriptAssetName;
            String currentFile = ctxt.getFilesDir() + "/"+ backupScriptAssetName;
            // Set the asset file to executable so copyFile will maintain that
            new File(currentFile).setExecutable(true, false);
            boolean copiedFile = RootTools.copyFile(currentFile, backupFile, true, true);
            Log.d(TAG, "Used RootTools to copy file from: "+currentFile+", to: "+backupFile+".  Was it successful? "+copiedFile);

            if (!copiedFile)
            {
                throw new SystemAppManagementException("Unable to copy the file \""+currentFile+"\" to \""+backupFile+"\".  You may need to try this manually using a tool such as Root Explorer.");
            }
        }        
    }
    
    /**
     * Reboots the device. It uses the 'reboot' shell command instead of the fast restart
     * This change was made because fast reboot doesn't work on some devices - notably the 
     * HTC One M8 - or rather it does work, but ends up taking 30 minutes and makes users 
     * panic that they bricked their phone - not cool.
     * @param ctxt
     */
    private static void rebootDevice(Context ctxt)
    {
        Command command = new Command(0, "reboot");
        boolean showError = false;
        try {
            RootTools.getShell(true).add(command);
        } catch (Exception e) {
            showError = true;
        }
        int count = 0;
        while(!command.isFinished() && (count < 40) && !showError)
        {
            try {
                Thread.sleep(500);
            } catch (Exception e){};
            ++count;
        }
        if (showError || (command.getExitCode() != 0))
        {
            Log.d(TAG, "Restarting phone via rootTools as reboot failed...");
            AlertDialog.Builder bldr = new AlertDialog.Builder(ctxt);
            bldr.setMessage("Unable to reboot automatically.  Please reboot your phone manually.")
                .setNeutralButton(android.R.string.ok, null)
                .show();
        }
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
        String privAppFile = getPrivAppPath(ctxt, true);
        boolean deletedPrivApp = RootTools.deleteFileOrDirectory(privAppFile, true);
        Log.d(TAG, "Used RootTools to delete app from: "+privAppFile+".  Was it successful? "+deletedPrivApp);

        // Now delete /data/app
        boolean deletedDataApp = RootTools.deleteFileOrDirectory(getAPKName(ctxt, true, false), false);
        Log.d(TAG, "Used RootTools to delete app from: "+deletedDataApp+".  Was it successful? "+deletedDataApp);

        // Now delete any files etc.
        boolean deletedDataDir = RootTools.deleteFileOrDirectory(ctxt.getApplicationInfo().dataDir, false);
        Log.d(TAG, "Used RootTools to delete data from: "+deletedDataDir+".  Was it successful? "+deletedDataDir);

        // And any backup scripts
        String backupScript = "/system/addon.d/"+backupScriptAssetName;        
        boolean deletedBackupScripts = RootTools.deleteFileOrDirectory(backupScript, true);
        Log.d(TAG, "Used RootTools to delete backup script from: "+backupScript+".  Was it successful? "+deletedBackupScripts);
        
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
                    rebootDevice(((Dialog)dialog).getContext());
                }
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }
}
