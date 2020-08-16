package com.xl.redisaux.common.utils;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author: lele
 * @date: 2020/08/14 下午18:24
 */
public class ClassUtil {

    public static final String FILE = "file";

    /**
     * 获取包下的集合
     * 1，获取类加载器
     * 2.通过类加载器获取到的资源
     * 3. 依据不同的资源类型，采用不同的方式获取资源的集合
     *
     * @param packageName
     * @return
     */
    public static Set<Class<?>> extractPackageClass(String packageName) {
        ClassLoader classLoader = getClassLoader();
        URL url = classLoader.getResource(packageName.replace('.', '/'));
        if (Objects.isNull(url)) {
            return null;
        }
        Set<Class<?>> classSet = null;
        if (url.getProtocol().equalsIgnoreCase(FILE)) {
            classSet = new HashSet<>();
            File packDirectory = new File(url.getPath());
            extractClassFile(classSet, packDirectory, packageName);
        }
        return classSet;

    }

    private static void extractClassFile(Set<Class<?>> emptyClassSet, File fileSource, String packageName) {
        if (!fileSource.isDirectory()) {
            return;
        }
        //如果是一个文件夹，调用器listFile方法获取文件夹下的文件或文件夹
        File[] files = fileSource.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                } else {
                    String absoluteFilePath = file.getAbsolutePath();
                    if (absoluteFilePath.endsWith(".class")) {
                        addToClassSet(absoluteFilePath);
                    }
                }
                return false;
            }

            private void addToClassSet(String absoluteFilePath) {
                absoluteFilePath = absoluteFilePath.replace(File.separator, ".");
                String clazzName = absoluteFilePath.substring(absoluteFilePath.indexOf(packageName));
                clazzName = clazzName.substring(0, clazzName.lastIndexOf("."));
                Class<?> targetClass = loadClass(clazzName);
                emptyClassSet.add(targetClass);
            }
        });
        if (files != null) {
            for (File file : files) {
                extractClassFile(emptyClassSet, file, packageName);
            }
        }

    }

    /**
     * package+类名
     *
     * @param className
     * @return
     */
    public static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ClassLoader getClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader;
    }

    public static <T> T newInstance(Class<?> clazz, boolean access) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(access);
            return (T) constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}