package com.gsamlabs.bbm.rootcompanion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

public class SystemAppUtilities {
    private static final String TAG = "SystemAppUtilities";
    private static String privAppFile = "/system/priv-app/gsamrootcompanion.apk";
    
    private static final String backupScriptAssetName = "91-gsamrootcompanion_backup.sh";

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
     * @throws SystemAppManagementException 
     */
    private static String getAPKName(Context ctxt, boolean includeFullPath, boolean doWildCard) throws SystemAppManagementException
    {
        String fullPath = ctxt.getApplicationInfo().sourceDir;
        if (fullPath.isEmpty() || (fullPath.lastIndexOf('/') == -1))
        {
        	throw new SystemAppManagementException("Unable to find the path to the APK.  Is it already uninsatlled?  Did you remember to reboot after uninstalling?  Current location appears to be: "+fullPath);
        }
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
    public static void installAsSystemApp(final Context ctxt) throws SystemAppManagementException
    {
    	AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>()
    	{
			SystemAppManagementException error = null;
    		ProgressDialog progress = null;
    		
    	    @Override
    	    protected void onPreExecute() {
    	        super.onPreExecute();
    	        progress = ProgressDialog.show(ctxt, ctxt.getText(R.string.main_progress_title),ctxt.getText(R.string.main_progress_copy_to_system));  
    	    }
    		
			@Override
			protected Boolean doInBackground(Void... params) {
				// Verify we do have root
		        try {
					checkRootAccess();
				} catch (SystemAppManagementException e) {
					error = e;
					return false;
				}
		        
		        // Copy the file to /system/priv-app
		        String currentFile;
				try {
					currentFile = getAPKName(ctxt, true, false);
				} catch (SystemAppManagementException e) {
					error = e;
					return false;
				}
		        boolean copiedApp = RootTools.copyFile(currentFile, privAppFile, true, true);
		        Log.d(TAG, "Used RootTools to copy app from: "+currentFile+", to: "+privAppFile+".  Was it successful? "+copiedApp);

		        if (!copiedApp)
		        {
		            error = new SystemAppManagementException("Unable to copy the file \""+currentFile+"\" to \""+privAppFile+"\".  You may need to try this manually using a tool such as Root Explorer.");
		            return false;
		        }

		        // Install the backup script if any...
		        try {
					installBackupScript(ctxt);
				} catch (SystemAppManagementException e) {
					error = e;
					return false;
				}
		        
		        return true;
			}

    		@Override
			protected void onPostExecute(Boolean result) {
    			progress.dismiss();

    			if (result)
    			{
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
    			} else
    			{
    				AlertDialog.Builder bldr = new AlertDialog.Builder(ctxt);
    				String message = (error != null) ? error.getMessage() : "Unknown Error";
                    bldr.setMessage(message)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
    			}
			}
    	};    
    	task.execute((Void)null);
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
    private static void rebootDevice(final Context ctxt)
    {
    	AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>()
    	{
    		ProgressDialog progress = null;
    		
    	    @Override
    	    protected void onPreExecute() {
    	        super.onPreExecute();
    	        progress = ProgressDialog.show(ctxt, ctxt.getText(R.string.main_progress_title),ctxt.getText(R.string.main_progress_title));  
    	    }
    		
			@Override
			protected Boolean doInBackground(Void... params) {
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
		        	return false;
		        }
		        
		        return true;
			}

    		@Override
			protected void onPostExecute(Boolean result) {
    			progress.dismiss();

    			if (!result)
    			{
    	            Log.d(TAG, "Restarting phone via rootTools as reboot failed...");
    	            AlertDialog.Builder bldr = new AlertDialog.Builder(ctxt);
    	            bldr.setMessage("Unable to reboot automatically.  Please reboot your phone manually.")
    	                .setNeutralButton(android.R.string.ok, null)
    	                .show();
    			} 
			}
    	};    
    	task.execute((Void)null);    	
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
    public static void uninstallApp(final Context ctxt) throws SystemAppManagementException
    {
    	AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>()
    	{

			SystemAppManagementException error = null;
    		ProgressDialog progress = null;
    		
    	    @Override
    	    protected void onPreExecute() {
    	        super.onPreExecute();
    	        progress = ProgressDialog.show(ctxt, ctxt.getText(R.string.main_progress_title),ctxt.getText(R.string.main_progress_deleting));  
    	    }
    		
			@Override
			protected Boolean doInBackground(Void... params) {
				 // Verify we do have root
		        try {
					checkRootAccess();
				} catch (SystemAppManagementException e) {
					error = e;
					return false;
				}
		        		        
		        // Delete /system/priv-app
		        // First try the 'properly' named one.  This is a hard-coded name:
		        boolean deletedPrivApp = RootTools.deleteFileOrDirectory(privAppFile, true);
		        Log.d(TAG, "Used RootTools to delete app from: "+privAppFile+".  Was it successful? "+deletedPrivApp);
		        
		        if (!deletedPrivApp)
		        {		        
		        	// If that didn't work, we'll try to 'discover' it.  This works if we don't have any updates installed.
		        	String apkLocation;
					try {
						apkLocation = getAPKName(ctxt, false, true);
						if (apkLocation.isEmpty() || apkLocation.startsWith("."))
						{
							throw new SystemAppManagementException("Invalid APK location - this should NEVER happen.  Location: "+apkLocation);
						}
					} catch (SystemAppManagementException e) {
						error = e;
						return false;
					}
		        	String otherPathToPrivAppFile = "/system/priv-app/"+apkLocation;
		        	deletedPrivApp = RootTools.deleteFileOrDirectory(otherPathToPrivAppFile, true);
		        	Log.d(TAG, "Used RootTools to delete app from: "+otherPathToPrivAppFile+".  Was it successful? "+deletedPrivApp);
		        }
		        if (deletedPrivApp)
		        {
		        	// Now delete /data/app
		        	String dataAppDirectory = ctxt.getApplicationInfo().sourceDir.substring(0, ctxt.getApplicationInfo().sourceDir.lastIndexOf('/'));
		        	if (dataAppDirectory.startsWith("/data/app/") && 
		        		(dataAppDirectory.length() > "/data/app/".length()))
		        	{
		        		boolean deletedDataApp = RootTools.deleteFileOrDirectory(dataAppDirectory, false);
		        		Log.d(TAG, "Used RootTools to delete app from: "+dataAppDirectory+".  Was it successful? "+deletedDataApp);
		        	}

		        	// Now delete any files etc.
		        	String dataDir = ctxt.getApplicationInfo().dataDir;
		        	if (dataDir.contains("gsamlabs"))
		        	{
		        		boolean deletedDataDir = RootTools.deleteFileOrDirectory(ctxt.getApplicationInfo().dataDir, false);
		        		Log.d(TAG, "Used RootTools to delete data from: "+ctxt.getApplicationInfo().dataDir+".  Was it successful? "+deletedDataDir);
		        	}

		        	// And any backup scripts
		        	String backupScript = "/system/addon.d/"+backupScriptAssetName;        
		        	boolean deletedBackupScripts = RootTools.deleteFileOrDirectory(backupScript, true);
		        	Log.d(TAG, "Used RootTools to delete backup script from: "+backupScript+".  Was it successful? "+deletedBackupScripts);
		        }
		        
		        if (hasBatteryStatsPermission(ctxt) && !deletedPrivApp)
		        {
		            error = new SystemAppManagementException("Unable to delete the file: "+privAppFile);
		            return false;
		        }
		        return true;
			}

    		@Override
			protected void onPostExecute(Boolean result) {
    			progress.dismiss();

    			if (result)
    			{
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
    			} else
    			{
    				AlertDialog.Builder bldr = new AlertDialog.Builder(ctxt);
    				String message = (error != null) ? error.getMessage() : "Unknown Error";
                    bldr.setMessage(message)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
    			}
			}
    	};    
    	task.execute((Void)null);
    }
}
