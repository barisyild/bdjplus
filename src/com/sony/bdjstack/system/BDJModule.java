package com.sony.bdjstack.system;

import com.sony.bdjstack.core.CoreXletContext;
import com.sony.bdjstack.init.Init;
import com.sony.bdjstack.security.BdjSecurityManager;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BDJModule {
    public static final int STATUS_UNSELECTED = 0;

    public static final int STATUS_PREPARED = 1;

    public static final int STATUS_STARTED = 2;

    private static BDJModule instance;

    private static boolean titleRestart;

    private static int titleRestartNumber = -1;

    private static boolean titleChange;

    private static String logs = "";

    private static URL[] urls = new URL[0];

    public static Map properties = new HashMap();
    public static boolean isInitialized = false;
    private static List readyList = new ArrayList();
    protected BDJModule(boolean paramBoolean, String[] paramArrayOfString) {

        //TODO: Find a proper way to get discKey
        String discKey = "Unknown";
        DataInputStream dataInputStream = null;
        try {
            dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream("/VP/CERTIFICATE/id.bdmv")));

            int startOffset = 0x33;
            dataInputStream.skipBytes(startOffset);

            for (int i = 0; i < dataInputStream.available(); i++) {
                int byteData = dataInputStream.readByte();
                if(byteData == 0x00 || byteData == 0x7F)
                {
                    // Disc name end
                    break;
                }

                if(discKey.equals("Unknown"))
                {
                    discKey = "";
                }

                discKey += (char) byteData;
            }
        } catch (Exception e) {
            log(e);
        }

        boolean isBlacklisted = false;

        // Read blacklist.txt
        File blackListFile = new File("/app0/cdc/blacklist.txt");
        if(blackListFile.exists())
        {
            // Read blacklist.txt line by line
            try {
                BufferedReader reader = new BufferedReader(new FileReader(blackListFile.getPath()));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if(line.startsWith("#") || line.equals(""))
                    {
                        continue;
                    }

                    if(line.equals(discKey))
                    {
                        System.setSecurityManager(new BdjSecurityManager());
                        isBlacklisted = true;
                    }
                }
            } catch (Exception e) {

            }
        }else{
            try {
                blackListFile.createNewFile();
            } catch (Exception e) {

            }
        }

        boolean isBypassed = false;
        if(!isBlacklisted)
        {
            String modulePath;

            if(System.getProperty("os.version").equals("ORBIS"))
            {
                modulePath = "/app0/cdc/modules";
            }else{
                // Windows
                modulePath = "modules";
            }

            int loadedModuleCount = 0;
            int failedModuleCount = 0;
            File modules = new File(modulePath + File.separator + "modules.txt");
            if(modules.exists())
            {
                BufferedReader reader;

                try {
                    reader = new BufferedReader(new FileReader(modules.getPath()));
                    String fileName = reader.readLine();

                    while (fileName != null) {
                        if(fileName.startsWith("#") || fileName.equals(""))
                        {
                            fileName = reader.readLine();
                            continue;
                        }

                        File file = new File(modulePath + File.separator + fileName);
                        if(file.exists() && file.isFile() && file.getName().endsWith(".jar"))
                        {
                            if(loadJar(file.getPath()) == null)
                            {
                                debug("Failed to load module: " + file.getName() + "\nCheck the logs for more information");
                                failedModuleCount++;
                            }else{
                                loadedModuleCount++;
                            }
                        } else if(!file.exists())
                        {
                            debug("Module not found: " + file.getName());
                            failedModuleCount++;
                        }
                        // read next line
                        fileName = reader.readLine();
                    }

                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            debug("BDJPlus created by barisyild");
            debug(loadedModuleCount + " BDJPlus modules loaded");

            if(failedModuleCount > 0)
            {
                debug(failedModuleCount + " BDJPlus modules failed to load!!!");
            }

            // Read blacklist.txt
            File bypassFile = new File("/app0/cdc/bypass.txt");
            if(bypassFile.exists())
            {
                // Read blacklist.txt line by line
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(bypassFile.getPath()));
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        if(line.startsWith("#") || line.equals(""))
                        {
                            continue;
                        }

                        if(line.equals(discKey))
                        {
                            isBypassed = true;
                            System.setProperty("bdjplus.bypass", "true");
                            System.setProperty("sony.rootcert", "/");
                        }
                    }
                } catch (Exception e) {

                }
            }else{
                try {
                    bypassFile.createNewFile();
                } catch (Exception e) {

                }
            }

            if(!isBypassed)
            {
                debug("Disc key: " + discKey + "\nIf the web server is not working properly, add disc key to bypass.txt file.");
            }
        }

        Init.start(paramBoolean, paramArrayOfString);

        isInitialized = true;
        for(int i = 0; i < readyList.size(); i++)
        {
            try {
                Object instance = readyList.get(i);
                Method method = instance.getClass().getDeclaredMethod("ready", new Class[0]);
                method.invoke(instance, new Object[0]);
            }
            catch (Exception e)
            {
                log(e);
            }
        }
    }

    public static void main(String[] paramArrayOfString) {
        BDJModule bDJModule = getInstance(true, paramArrayOfString);
        bDJModule.waitForExit();
        // Close Event
        System.exit(0);
    }

    public static boolean escapeSandbox(Class bypassClass)
    {
        Class kernelModuleClass = (Class) properties.get("KernelModule");
        if(kernelModuleClass != null)
        {
            try {
                kernelModuleClass.getMethod("escapeSandbox", new Class[]{Class.class}).invoke(null, new Object[]{bypassClass});
                log("Sandbox escaped thanks to TheOfficialFloW and sleirsgoevy");
                return true;
            }
            catch (Exception e) {
                log(e);
            }
        }
        return false;
    }

    public static Object loadJar(String path) {
        return loadJar(path, false);
    }
    public static Object loadJar(String path, boolean forceToFindXlet) {
        log("Loading module jar: " + path);
        log("Reading JAR Manifest...");

        File file = new File(path);
        File bdjoFile = new File(path.substring(0, path.length() - "jar".length()) + "bdjo");
        BDJModule.log("BDJO File: " + bdjoFile.getAbsolutePath());

        URLClassLoader urlClassLoader = null;

        try {
            URL[] cloneUrls = new URL[urls.length + 1];
            cloneUrls[0] = file.toURL();
            for(int i = 0; i < urls.length; i++)
            {
                cloneUrls[i + 1] = urls[i];
            }

            urlClassLoader = new URLClassLoader(
                    cloneUrls,
                    BDJModule.class.getClassLoader()
            );
        }
        catch (Exception e)
        {
            log(e);
        }

        Class classToLoad = null;
        try {
            if(bdjoFile.exists())
            {
                String xletMainClass = readXletMainClass(new FileInputStream(bdjoFile));
                BDJModule.log(xletMainClass);
                classToLoad = Class.forName(xletMainClass, true, urlClassLoader);
            } else if(forceToFindXlet)
            {
                // Find xlet class
                List classNames = new ArrayList();
                ZipInputStream zip = new ZipInputStream(new FileInputStream(file.getPath()));
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        // This ZipEntry represents a class. Now, what class does it represent?
                        String className = entry.getName().replace('/', '.'); // including ".class"
                        classNames.add(className.substring(0, className.length() - ".class".length()));
                    }
                }

                for(int i = 0; i < classNames.size(); i++)
                {
                    String className = (String) classNames.get(i);
                    Class clazz = Class.forName(className, false, urlClassLoader);
                    Class[] interfaces = clazz.getInterfaces();
                    for(int j = 0; j < interfaces.length; j++)
                    {
                        if(interfaces[j].getName().equals("javax.tv.xlet.Xlet"))
                        {
                            log("Found xlet class: " + className);
                            classToLoad = clazz;
                            break;
                        }
                    }
                    if(classToLoad != null)
                        break;
                }
            }

            if (classToLoad != null) {
                // Found xlet class
                log("Found xlet class: " + classToLoad);

                // Try xlet
                Method initXletMethod = null;
                Method startXletMethod = null;

                log("Continuing with xlet class: " + classToLoad);

                log("No xlet class found, aborting");
                Method[] methods = classToLoad.getMethods();

                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    if (method.getName().equals("initXlet")) {
                        initXletMethod = method;
                    } else if (method.getName().equals("startXlet")) {
                        startXletMethod = method;
                    }
                }

                if(initXletMethod != null && startXletMethod != null)
                {
                    log("Found xlet methods, starting xlet");
                    log("Creating xlet instance");

                    escapeSandbox(classToLoad);
                    Object instance = classToLoad.newInstance();

                    log("Xlet instance created: " + instance);

                    getInstance().terminateTitle(getInstance().getCurrentTitle());
                    getInstance().terminateTitle(0);

                    initXletMethod.invoke(instance, new Object[]{new CoreXletContext(0, new String[0], 0, 0)});
                    startXletMethod.invoke(instance, new Object[]{});
                    return instance;
                }

                log("No xlet methods found, aborting");
            }
        }
        catch (Exception e)
        {
            log(e);
        }

        try {
            log("Trying main method");

            JarFile jarFile = new JarFile(file);
            String className = null;

            try {
                JarEntry manifestEntry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
                if (manifestEntry == null) {
                    throw new FileNotFoundException("Unable to find JAR manifest");
                }

                InputStream manifestStream = jarFile.getInputStream(manifestEntry);
                try {
                    Manifest mf = new Manifest(manifestStream);
                    className = mf.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                } finally {
                    manifestStream.close();
                }
            } finally {
                jarFile.close();
            }

            log("Reading JAR Manifest...Main Class: " + className);
            log("Loading JAR...");

            Method mainMethod = null;
            if(className != null)
            {
                classToLoad = Class.forName(className, true, urlClassLoader);
                for(int i = 0; i < classToLoad.getMethods().length; i++)
                {
                    if(classToLoad.getMethods()[i].getName().equals("main"))
                        mainMethod = classToLoad.getMethods()[i];
                }

                escapeSandbox(classToLoad);
                Object instance = classToLoad.newInstance();

                if(mainMethod != null)
                {
                    log("Jar instance created");
                    mainMethod.invoke(instance, new Object[]{new String[] {file.getAbsolutePath()}});
                    if(!isInitialized)
                    {
                        readyList.add(instance);
                    }else{
                        try {
                            Method method = classToLoad.getDeclaredMethod("ready", new Class[0]);
                            method.invoke(instance, new Object[0]);
                        }
                        catch (Exception e)
                        {
                            log(e);
                        }
                    }
                    return instance;
                }
            }
        }
        catch (Exception e)
        {
            log(e);
            e.printStackTrace();
        }

        return null;
    }

    public static String readXletMainClass(InputStream inputStream) throws IOException
    {
        int size = inputStream.available();

        DataInputStream dataInputStream = new DataInputStream(inputStream);

        byte[] headerBytes = new byte[4];
        dataInputStream.read(headerBytes);
        String header = new String(headerBytes);
        if (!header.equals("BDJO")) {
            throw new IOException("BDJO header is unknown");
        }

        // Skip unknown addresses
        dataInputStream.skip(16);

        // Read metadata address
        int metadataAddress = dataInputStream.readInt();
        BDJModule.log("metadataAddress: " + metadataAddress);

        // Skip to metadata address
        int jumpAddress = metadataAddress - (headerBytes.length + 16 + 4);
        dataInputStream.skip(jumpAddress);
        BDJModule.log("jump to address: " + jumpAddress);

        dataInputStream.skip(4);

        // Read metadata size
        int numApps = dataInputStream.readUnsignedByte();
        BDJModule.log("numApps: " + numApps);
        dataInputStream.skip(1);

        for(int i = 0; i < numApps; i++)
        {
            dataInputStream.skip(18);
            //00000112

            /*
            final int appProfileCount
                = readBits("        application profiles count", dis, bio, 4);
            readBits("        padding", dis, bio, 12);
             */

            // 2 byte
            int data = dataInputStream.readUnsignedByte();
            int profileCount = data >> 4;
            BDJModule.log("profileCount: " + profileCount);
            dataInputStream.skip(1);

            for(int j = 0; j < profileCount; j++)
            {
                dataInputStream.skip(6);
            }

            dataInputStream.skip(2);

            int totalNameBytes = dataInputStream.readUnsignedShort();
            if (totalNameBytes > 0) {
                int nameBytesRead = 0;
                while (nameBytesRead < totalNameBytes) {
                    dataInputStream.skip(3);
                    nameBytesRead += 3;
                    int nameLen = dataInputStream.readUnsignedByte();
                    nameBytesRead++;
                    byte[] nameBytes = new byte[nameLen];
                    dataInputStream.read(nameBytes);
                    nameBytesRead += nameLen;
                }
            }
            if ((totalNameBytes & 0x1) != 0) {
                dataInputStream.skip(1);
            }

            int iconLength = dataInputStream.readUnsignedByte();
            dataInputStream.skip(iconLength);
            if ((iconLength & 0x1) == 0) {
                dataInputStream.skip(1);
            }

            dataInputStream.skip(2);

            int baseDirLength = dataInputStream.readUnsignedByte();
            dataInputStream.skip(baseDirLength);
            if ((baseDirLength & 0x1) == 0) {
                dataInputStream.skip(1);
            }

            int classPathLength = dataInputStream.readUnsignedByte();
            dataInputStream.skip(classPathLength);
            if ((classPathLength & 0x1) == 0) {
                dataInputStream.skip(1);
            }

            int initClassLength = dataInputStream.readUnsignedByte();
            byte[] initClassBytes = new byte[initClassLength];
            dataInputStream.read(initClassBytes);
            return new String(initClassBytes);
        }

        return null;
    }

    public static void log(String data)
    {
        log(new Object[]{data});
    }

    public static void log(Object data)
    {
        String dataVal = data == null ? "null" : data.toString();
        log(new Object[]{dataVal});
    }

    public static void log(Throwable throwable)
    {
        log(throwable, new Object[0]);
    }

    public static void getAllThreads()
    {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ((parentGroup = rootGroup.getParent()) != null) {
            rootGroup = parentGroup;
        }
    }

    public static void log(Throwable throwable, Object[] tmpData)
    {
        Object[] data = new Object[tmpData.length + 1];

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        data[0] = stringWriter.toString();

        for (int i = 0; i < tmpData.length; i++)
        {
            data[i + 1] = tmpData[i];
        }
        log(data);
    }

    public static String log()
    {
        return logs;
    }

    public static void log(Object[] data)
    {
        char logSeperator = ',';
        String requestBuffer = "";
        for (int i = 0; i < data.length; i++)
        {
            if(data[i] == null)
                data[i] = "null";

            requestBuffer = data[i].toString() + logSeperator;
        }
        if(requestBuffer.charAt(requestBuffer.length() - 1) == logSeperator)
        {
            requestBuffer = requestBuffer.substring(0, requestBuffer.length() - 1);
        }

        logs += requestBuffer + "\n";

        if(System.getProperty("os.version").equals("ORBIS"))
        {
            requestBuffer = URLEncoder.encode(requestBuffer);

            try
            {
                /*URL url = new URL("http://webhook.site/85500fde-dd82-4c56-bea9-f68103e7a50f");

                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", Integer.toString(requestBuffer.length()));

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(requestBuffer);

                conn.getInputStream(); // Request is sent*/
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }else{
            System.out.println(requestBuffer);
        }
    }
    public static String debug(String s)
    {
        log("DEBUG: " + s);
        Object kernelRW = properties.get("KernelRW");
        if(kernelRW != null)
        {
            try {
                kernelRW.getClass().getMethod("notify", new Class[]{String.class}).invoke(kernelRW, new Object[]{s});
            }
            catch (Exception e)
            {

            }
        }
        return null;
    }

    public static URLClassLoader getClassLoader()
    {
        return new URLClassLoader(
                urls,
                BDJModule.class.getClassLoader()
        );
    }

    public static BDJModule getInstance() {
        return (instance != null) ? instance : getInstance(false, new String[0]);
    }

    public static synchronized BDJModule getInstance(boolean paramBoolean, String[] paramArrayOfString) {
        if (instance == null)
            instance = new BDJModule(paramBoolean, paramArrayOfString);
        return instance;
    }

    public boolean getTitleRestart(int paramInt) {
        return (titleRestart && paramInt == titleRestartNumber);
    }

    public void setTitleRestart(boolean paramBoolean, int paramInt) {
        titleRestart = paramBoolean;
        titleRestartNumber = paramBoolean ? paramInt : -1;
    }

    public boolean getTitleChange() {
        return titleChange;
    }

    public void setTitleChange(boolean paramBoolean) {
        titleChange = paramBoolean;
    }

    public void invokeTitleCallback(int paramInt) {
        invokeTitleCallback(paramInt, 0);
        titleRestart = false;
    }

    public native int getCurrentTitle();

    public native void invokeTitleCallback(int paramInt1, int paramInt2);

    public native int waitForExit();

    public native int prepareTitle(int paramInt1, int paramInt2);

    public native int prepareTitle(int paramInt);

    public native int prepareTitle(String paramString);

    public native int startTitle();

    public native int terminateTitle(int paramInt);

    public native int destroy();

    public native int getState();

    public native void postKeyEvent(int paramInt1, int paramInt2, int paramInt3);
}