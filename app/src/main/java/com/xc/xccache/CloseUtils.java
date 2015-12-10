package com.xc.xccache;

import java.io.Closeable;
import java.io.IOException;

/** 关闭Closeable对象工具方法
 * Created by caizhiming on 2015/12/3.
 */
public final class CloseUtils {
    private CloseUtils(){

    }
    /**
     * 关闭Closeable对象
    */
    public static void closeCloseable(Closeable closeable){
        if(null != closeable){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
