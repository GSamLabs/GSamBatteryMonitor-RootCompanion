GSam Battery Monitor - Root Companion
=====================================

Unfortunately, Google has removed the ability in KitKat (4.4) for non-system applications to access battery statistics.  This companion app restores this function by running as a privileged system application.   Root access to your device is required.

Why Install?

* If you are using GSam Battery Monitor on KitKat (4.4) or later, and the app informs you that it is unable to retrieve battery statistics, you should install this app.

What does this do?

* This  runs as a system privileged app and provides the caller access to the Battery Statistics that - prior to Android 4.4 - used to be accessible to any normal application.
* This also provides the ability to read the content of small files to which the regular battery monitor has no access - wakelock files for example.

Is Root really required?

* Unfortunately, root is required to copy this app into /system/priv-app, which allows this app to run as a privileged system application.  There are other alternatives, but all of them require root. 

Is this safe?

* Yes - the code is open source, and very simple.  GSam Labs believes that any app that you install as root must be fully open source to allow you to inspect the application and ensure it doesn't do anything malicious.  With that principle in mind, you can find the source here: https://github.com/GSamLabs/GSamBatteryMonitor-RootCompanion
* NOTE:  If you are unfamiliar with what 'root' means, this app is not for you.

How do I install it?

* Install this just like any other application from the store.  Once installed, there will be a button that will copy the app to /system/priv-app, and prompt you to restart your device.  
* Flashable Zip:  You can find a flashable zip in the github repository under the RecoveryInstallImages directory (http://goo.gl/0zcqbL).  Flash this in the recovery of your choice.  This should work for most phones, but since the google update binary differs between devices, there is no guarantee.
* NOTE:  This only works if you have root access to your device!

How do I uninstall it?

* Once a system app is installed, the normal uninstall procedures don't work.  To uninstall this app once it is a system app, simply launch the app and click on the Uninstall button.

Will this work with other battery monitors?

* Not by default - it is up to the author of the battery monitor whether they would like to leverage this tool or not.  GSam Battery Monitor of course is fully supported.

Help!  Things don't work after installing this!

* Like all root applications, some care must be taken by the device owner when installing this app.  This application is free and open source.  GSam Labs is not responsible for any problems, though we will try and help - simply contact us via the Support link.

Credits

* Thanks to stericson for providing such a great RootTools library (http://code.google.com/p/roottools/)
