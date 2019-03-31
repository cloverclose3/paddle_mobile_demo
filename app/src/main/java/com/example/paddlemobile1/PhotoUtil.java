package com.example.paddlemobile1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;


public class PhotoUtil {

    // start camera
    public static Uri start_camera(Activity activity, int requestCode) {
        Uri imageUri;
        // save image in cache path
        File outputImage = new File(activity.getExternalCacheDir(), "out_image.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= 24) {
            // compatible with Android 7.0 or over
            imageUri = FileProvider.getUriForFile(activity,
                    "com.example.paddlemobile1", outputImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
        }
        // set system camera Action
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // set save photo path
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        // set photo quality, min is 0, max is 1
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        activity.startActivityForResult(intent, requestCode);
        return imageUri;
    }

    // get picture in photo
    public static void use_photo(Activity activity, int requestCode){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    // get photo from Uri
    /*
    public static String get_path_from_URI(Context context, Uri uri) {
        String result;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }*/

    public static String get_path_from_URI(final Context context, final Uri uri ) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    // Compress the image to the size of the training image，and change RGB
    public static float[] getScaledMatrix(Bitmap bitmap, int desWidth,
                                   int desHeight) {
        float[] dataBuf = new float[3 * desWidth * desHeight];
        int rIndex;
        int gIndex;
        int bIndex;
        int[] pixels = new int[desWidth * desHeight];
        Bitmap bm = Bitmap.createScaledBitmap(bitmap, desWidth, desHeight, false);
        bm.getPixels(pixels, 0, desWidth, 0, 0, desWidth, desHeight);
        int j = 0;
        int k = 0;
        for (int i = 0; i < pixels.length; i++) {
            int clr = pixels[i];
            j = i / desHeight;
            k = i % desWidth;
            rIndex = j * desWidth + k;
            gIndex = rIndex + desHeight * desWidth;
            bIndex = gIndex + desHeight * desWidth;
            dataBuf[rIndex] = (float) ((clr & 0x00ff0000) >> 16) - 148;
            dataBuf[gIndex] = (float) ((clr & 0x0000ff00) >> 8) - 148;
            dataBuf[bIndex] = (float) ((clr & 0x000000ff)) - 148;

        }
        if (bm.isRecycled()) {
            bm.recycle();
        }
        return dataBuf;
    }

    // compress picture
    public static Bitmap getScaleBitmap(String filePath) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, opt);

        int bmpWidth = opt.outWidth;
        int bmpHeight = opt.outHeight;

        int maxSize = 500;

        // compress picture with inSampleSize
        opt.inSampleSize = 1;
        while (true) {
            if (bmpWidth / opt.inSampleSize < maxSize || bmpHeight / opt.inSampleSize < maxSize) {
                break;
            }
            opt.inSampleSize *= 2;
        }
        opt.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, opt);
    }
}