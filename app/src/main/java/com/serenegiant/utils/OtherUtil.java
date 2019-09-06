package com.serenegiant.utils;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import com.serenegiant.gles.core.GlUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by yong on 2019/9/6.
 */

public class OtherUtil {
    public static void saveFrame(String fileName, byte[] arrayBytes) throws IOException {

        FileOutputStream file = new FileOutputStream(fileName, true);
        file.write(arrayBytes);
        file.close();
    }
}
