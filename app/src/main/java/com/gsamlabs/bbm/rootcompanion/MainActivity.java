package com.gsamlabs.bbm.rootcompanion;

import com.stericson.RootTools.RootTools;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CheckBox removeCheckBox = (CheckBox)findViewById(R.id.idRemoveFromLauncherCheckbox);
        // If we have battery stats permission, handle it specially
        if (SystemAppUtilities.hasBatteryStatsPermission(this))
        {
            ((TextView) findViewById(R.id.idIntroText)).setText(R.string.main_intro_installed);
            findViewById(R.id.idInstructionsHeader).setVisibility(View.GONE);
            findViewById(R.id.idInstructionsDetail).setVisibility(View.GONE);
            findViewById(R.id.idPreReq).setVisibility(View.GONE);
            findViewById(R.id.idPrereqLayout).setVisibility(View.GONE);
            Button installButton = (Button)findViewById(R.id.idInstallButton);
            installButton.setText(R.string.main_install_button_uninstall);
            installButton.setOnClickListener(new View.OnClickListener() {                
                @Override
                public void onClick(View v) {
                    try {
                        SystemAppUtilities.uninstallApp(MainActivity.this);
                    } catch (SystemAppManagementException e) {
                        AlertDialog.Builder bldr = new AlertDialog.Builder(MainActivity.this);
                        bldr.setMessage(e.getMessage())
                            .setNeutralButton(android.R.string.ok, null)
                            .show();
                    }
                }
            });
            
            final ComponentName launcherActivityComponent = new ComponentName(this,  MainActivityLauncher.class);
            removeCheckBox.setChecked(getPackageManager().getComponentEnabledSetting(launcherActivityComponent) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED);            
            removeCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
                    if (isChecked)
                    {
                        AlertDialog.Builder bldr = new AlertDialog.Builder(MainActivity.this);
                        bldr.setMessage(R.string.remove_from_launcher_confirm)
                        .setTitle(R.string.remove_from_launcher)
                        .setNeutralButton(R.string.no, new DialogInterface.OnClickListener() {                            
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                buttonView.setChecked(false);
                            }
                        })
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {                                                       
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getPackageManager().setComponentEnabledSetting(launcherActivityComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {                            
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                buttonView.setChecked(false);
                            }
                        })
                        .show();
                    } else
                    {
                        getPackageManager().setComponentEnabledSetting(launcherActivityComponent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
                    }
                }
            });
            
        } else
        {
            removeCheckBox.setVisibility(View.GONE);
            
            final Button installButton = (Button)findViewById(R.id.idInstallButton);

            installButton.setText(R.string.main_install_button);
            installButton.setOnClickListener(new View.OnClickListener() {                
                @Override
                public void onClick(View v) {
                    try {
                        SystemAppUtilities.installAsSystemApp(MainActivity.this);
                    } catch (SystemAppManagementException e) {
                        AlertDialog.Builder bldr = new AlertDialog.Builder(MainActivity.this);
                        bldr.setMessage(e.getMessage() + "\n\n"+getString(R.string.error_occurred))
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
                    }
                }
            });
            
            
        	AsyncTask<Void, Void, Boolean> checkRoot = new AsyncTask<Void, Void, Boolean>()
        	{
        		ProgressDialog progress = null;
        	    @Override
	    	    protected void onPreExecute() {
	    	        super.onPreExecute();
	    	        progress = ProgressDialog.show(MainActivity.this, getText(R.string.main_progress_title), getText(R.string.main_progress_checking_for_root));  
	    	    }
	    		
				@Override
				protected Boolean doInBackground(Void... params) {
					 // Verify we do have root
					return SystemAppUtilities.isRootAvailable();
				}
	
	    		@Override
				protected void onPostExecute(Boolean result) {
	    			progress.dismiss();
	                ((TextView)findViewById(R.id.idIsRootValue)).setText(result ? R.string.yes : R.string.no);
				}
        	};    
        	checkRoot.execute((Void)null);
        }
    }

}
