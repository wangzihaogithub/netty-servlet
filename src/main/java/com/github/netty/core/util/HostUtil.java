package com.github.netty.core.util;

import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * <p>
 * Common functions that involve the host platform
 *
 * @author wangzihao
 */
public class HostUtil {

    private static String osName;
    private static int pid;

    static {
        AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            osName = System.getProperty("os.name").toLowerCase();

            String name = ManagementFactory.getRuntimeMXBean().getName();
            pid = Integer.parseInt(name.split("@")[0]);

            return Boolean.getBoolean("com.sun.javafx.isEmbedded");
        });
    }

    public static boolean isLocalhost(String host) {
        return "localhost".equalsIgnoreCase(host) || host.contains("0.0.0.0") || host.contains("127.0.0.1");
    }

    public static int getPid() {
        return pid;
    }

    public static String getOsName() {
        return osName;
    }


}
