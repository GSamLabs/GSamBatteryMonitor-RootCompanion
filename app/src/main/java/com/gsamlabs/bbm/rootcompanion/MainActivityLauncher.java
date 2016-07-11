package com.gsamlabs.bbm.rootcompanion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * The launcher activity is really just a dummy activity that launches the 'real' main activity.
 * We use this so that we can disable/enable this activity to allow us to hide/show the app
 * in the launcher tray. 
 */
public class MainActivityLauncher extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Simply launch the real activity.  
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName(this, MainActivity.class));
        startActivity(intent);
    }

}
