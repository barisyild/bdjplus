package com.sony.bdjstack.security;

public class BdjSecurityManager extends SecurityManager {
    public static void init() {

    }

    public void checkPropertyAccess(String var1) {
        if (!var1.startsWith("java.awt.")) {
            if (var1.equals("dvb.persistent.root") || var1.equals("bluray.bindingunit.root") || var1.equals("bluray.vfs.root") || var1.equals("user.name") || var1.equals("user.home") || var1.startsWith("java.") || var1.startsWith("os.")) {
                super.checkPropertyAccess(var1);
            }

        }
    }

    public void checkPackageAccess(String var1) throws SecurityException {
        if (!var1.equals("java.nio")) {
            super.checkPackageAccess(var1);
        }

    }
}