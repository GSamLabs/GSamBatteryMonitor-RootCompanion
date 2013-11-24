package com.gsamlabs.bbm.rootcompanion;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            installButton.setOnClickListener(new OnClickListener() {                
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
        } else
        {
            Button installButton = (Button)findViewById(R.id.idInstallButton);

            installButton.setText(R.string.main_install_button);
            installButton.setOnClickListener(new OnClickListener() {                
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

            ((TextView)findViewById(R.id.idIsRootValue)).setText(SystemAppUtilities.isRootAvailable() ? R.string.yes : R.string.no);
        }
    }

}
