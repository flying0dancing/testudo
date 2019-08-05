package com.lombardrisk.utils;

import com.lombardrisk.status.BuildStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Helper {

    private final static Logger logger = LoggerFactory.getLogger(Helper.class);

    public static <T> Object filterListByID(
            List<T> list,
            Object value) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return filterListBy(list, "getID", value);
    }

    public static <T> Object filterListByPrefix(
            List<T> list,
            Object value) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return filterListBy(list, "getPrefix", value);
    }

    /**
     * get an generic object from a list by specified method
     *
     * @param list
     * @param by
     * @param value
     * @return an generic object
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    protected static <T> Object filterListBy(
            List<T> list,
            String by,
            Object value) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (list != null && !list.isEmpty()) {
            Method method = list.get(0).getClass().getDeclaredMethod(by);
            for (T element : list) {
                if (method.invoke(element).equals(value)) {
                    return element;
                }
            }
        }
        return null;
    }

    public static String convertBlobToStr(Blob blob) {
        try {
            byte[] bytes = blob.getBytes(1l, (int) blob.length());
            return new String(bytes, "UTF-8");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * revise/uniform file path separator, let file's separator followed OS's.
     *
     * @param path
     * @return
     */
    public static String reviseFilePath(String path) {
        if (StringUtils.isNotBlank(path)) {
            path = path.replace("\"", "");

            if (System.getProperty("file.separator").equals("/")) {
                path = path.replace("\\\\", "/");
                path = path.replaceAll("/+", "/");
            } else {
                path = path.replace("/", "\\");
                path = path.replaceAll("\\\\+", "\\\\");
            }
        }
        return path;
    }

    /***
     * remove last separator of path if it has.
     * @param path
     * @return
     */
    public static String removeLastSlash(String path) {
        if (StringUtils.isNotBlank(path)) {
            path = path.replace("\"", "");
            if (path.endsWith("/") || path.endsWith("\\")) {
                path = path.substring(0, path.length() - 1);
            }
        }
        return path;
    }

    /***
     * it can get all relative parent folders, for example /grandfather/fater/foo, result is [grandfather/,grandfather/fater/,grandfather/fater/foo/]
     * it will remove ", and started or ended \ or //, adding / or \\ at the end
     * @param relativePath path is a relative path
     * @return
     */
    public static List<String> getRelativePaths(String startPath, String relativePath) {
        List<String> paths = null;
        if (StringUtils.isNotBlank(relativePath)) {
            paths = new ArrayList<String>();
            relativePath = relativePath.replace("\"", "");
            relativePath = relativePath.replaceAll("^(?:[\\/\\\\]+)?(.*?)(?:[\\/\\\\]+)?$", "$1");
            String[] pathsTmp = relativePath.split("[\\/\\\\]+");
            for (int i = 0; i < pathsTmp.length; i++) {
                String tmp = startPath;
                for (int j = 0; j <= i; j++) {
                    tmp = tmp + pathsTmp[j] + System.getProperty("file.separator");
                }
                paths.add(tmp.substring(0, tmp.length() - 1));
                //paths.add(tmp);
            }
        }
        return paths;
    }

    /***
     * get parent path, if it is the top folder, return itself
     * @param path
     * @return
     */
    public static String getParentPath(String path) {
        if (StringUtils.isNotBlank(path)) {
            path = removeLastSlash(path);
            int lastSlash = path.lastIndexOf("\\") == -1 ? path.lastIndexOf("/") : path.lastIndexOf("\\");//get parent path
            if (lastSlash > 0) {
                path = path.substring(0, lastSlash) + System.getProperty("file.separator");
            } else {
                path = path + System.getProperty("file.separator");
            }
        }
        return path;
    }

    public static void readme(String file) {
        try (InputStream is = ClassLoader.getSystemResourceAsStream(file)) {
            String line;

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * run external command
     *
     * @param commons
     * @return
     */
    public static Boolean runCmdCommand(String[] commons) {
        Boolean flag = true;
        logger.info(String.join(" ", commons));
        try {
            Process process = Runtime.getRuntime().exec(commons);
            process.waitFor();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String str = null;
            logger.debug("Here is the standard output of the command:");
            while ((str = stdInput.readLine()) != null) {
                logger.debug(str);
                if (str.toLowerCase().contains("error")) {
                    flag = false;
                    break;
                }
            }
            logger.debug("Here is the standard error of the command (if any):");
            while ((str = stdError.readLine()) != null) {
                BuildStatus.getInstance().recordError();
                logger.error(str);
                if (str.toLowerCase().contains("error")) {
                    flag = false;
                    break;
                }
            }
        } catch (InterruptedException | IOException e) {
            flag = false;
            BuildStatus.getInstance().recordError();
            logger.error(e.getMessage(), e);
        }
        if (flag) {
            logger.info("cmd run OK.");
        } else {
            BuildStatus.getInstance().recordError();
            logger.error("cmd run failed.");
        }
        return flag;
    }


    public static <T> void removeDuplicatedElements(List<T> list){
        if(list!=null){
            removeBlanks(list);
            int size=list.size();
            if(size>1){
                for(int i=0;i<size-1;i++){
                    for(int j=i+1;j<size;j++){
                        if(list.get(i).equals(list.get(j))){
                            list.remove(j);
                            size--;
                        }
                    }
                }
            }
        }
    }

    public static <T> void removeBlanks(List<T> list){
        if(list!=null) {
            Iterator it=list.iterator();
            while(it.hasNext()){
                T obj=(T)it.next();
                if(obj.equals("")){
                    it.remove();
                }
            }
            it=null;
        }
    }

    public static <T> Boolean isEmptyList(List<T> list){
        if(list!=null){
            if(list.size()>0){
                return false;
            }
        }
        return true;
    }

}
