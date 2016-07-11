package com.gsamlabs.bbm.rootcompanion;

public class SystemAppManagementException extends Exception {

    private static final long serialVersionUID = 5745558614933336197L;
    public SystemAppManagementException(String msg)
    {
        super(msg);
    }
    public SystemAppManagementException(String msg, Throwable e)
    {
        super(msg, e);
    }
}
