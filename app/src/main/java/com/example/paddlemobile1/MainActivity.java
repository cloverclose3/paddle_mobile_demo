package com.example.paddlemobile1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final int USE_PHOTO = 1001;
    private static final int START_CAMERA = 1002;
    private Uri image_uri;
    private ImageView show_image;
    private TextView result_text;
    private String assets_path = "paddle_models";
    private boolean load_result = false;
    private int[] ddims = {1, 3, 224, 224};

    private static final String[] PADDLE_MODEL = {
            "googlenet",
            "mobilenet_v1",
            "mobilenet_v2"
    };

    // load paddle-mobile api
    static {
        try {
            System.loadLibrary("paddle-mobile");

        } catch (SecurityException e) {
            e.printStackTrace();

        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();

        } catch (NullPointerException e) {
            e.printStackTrace();

        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    // initialize view
    private void init() {
        request_permissions();
        show_image = (ImageView) findViewById(R.id.show_image);
        result_text = (TextView) findViewById(R.id.result_text);
        Button use_photo = (Button) findViewById(R.id.use_photo);
        Button start_photo = (Button) findViewById(R.id.start_camera);

        // use photo click
        use_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotoUtil.use_photo(MainActivity.this, USE_PHOTO);
//                load_model();
            }
        });

        // start camera click
        start_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                image_uri = PhotoUtil.start_camera(MainActivity.this, START_CAMERA);
            }
        });

        // copy file from assets to sdcard
        String sdcard_path = Environment.getExternalStorageDirectory()
                + File.separator + assets_path;
        copy_file_from_asset(this, assets_path, sdcard_path);

        // load model
        load_model();
    }

    // load infer model
    private void load_model() {
        String model_path = Environment.getExternalStorageDirectory()
                + File.separator + assets_path + File.separator + PADDLE_MODEL[0];
        Log.d(TAG, model_path);
        load_result = ImageRecognition.load(model_path);
        if (load_result) {
            Log.d(TAG, "model load success");
        } else {
            Log.d(TAG, "model load fail");
        }
    }

    // clear infer model
    private void clear_model() {
        ImageRecognition.clear();
        Log.d(TAG, "model is clear");
    }

    // copy file from asset to sdcard
    public void copy_file_from_asset(Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);
            if (fileNames.length > 0) {
                // directory
                File file = new File(newPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                // copy recursivelyC
                for (String fileName : fileNames) {
                    copy_file_from_asset(context, oldPath + "/" + fileName, newPath + "/" + fileName);
                }
                Log.d(TAG, "copy files finish");
            } else {
                // file
                File file = new File(newPath);
                // if file exists will never copy
                if (file.exists()) {
                    return;
                }

                // copy file to new path
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        String image_path;
        RequestOptions options = new RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case USE_PHOTO:
                    if (data == null) {
                        Log.w(TAG, "user photo data is null");
                        return;
                    }
                    image_uri = data.getData();
                    Glide.with(MainActivity.this).load(image_uri).apply(options).into(show_image);
                    // get image path from uri
                    image_path = PhotoUtil.get_path_from_URI(MainActivity.this, image_uri);
                    // show result
                    result_text.setText(image_path);
                    // predict image
                    predict_image(PhotoUtil.get_path_from_URI(MainActivity.this, image_uri));
                    break;
                case START_CAMERA:
                    // show photo
                    Glide.with(MainActivity.this).load(image_uri).apply(options).into(show_image);
                    // get image path from uri
                    //image_path = PhotoUtil.get_path_from_URI(MainActivity.this, image_uri);
                    // show result
                    //System.out.println("image_path is: " + image_path);
                    //result_text.setText(image_path);
                    // predict image
                    //predict_image(PhotoUtil.get_path_from_URI(MainActivity.this, image_uri));

                    File CopyPhoto=new File(getExternalCacheDir(),"copy_image.jpg");
                    try{
                        if(CopyPhoto.exists()){
                            CopyPhoto.delete();
                        }
                        CopyPhoto.createNewFile();
                    }catch(IOException e){
                        e.printStackTrace();
                    }

                    copyFile(CopyPhoto.getAbsolutePath(), image_uri);
                    System.out.println("image_path is: " + CopyPhoto.getAbsolutePath());
                    result_text.setText(CopyPhoto.getAbsolutePath());
                    predict_image(CopyPhoto.getAbsolutePath());

                    break;
            }
        }
    }
    private void copyFile(String audioDst, Uri uri) {
        try {
            AssetFileDescriptor audioAsset = getContentResolver()
                    .openAssetFileDescriptor(uri, "r");
            InputStream in = audioAsset.createInputStream();
            OutputStream out = new FileOutputStream(audioDst);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    private void predict_image(String image_path) {
        // picture to float array
        Bitmap bmp = PhotoUtil.getScaleBitmap(image_path);
        float[] inputData = PhotoUtil.getScaledMatrix(bmp, ddims[2], ddims[3]);
        try {
            long start = System.currentTimeMillis();
            // get predict result
            float[] result = ImageRecognition.predictImage(inputData, ddims);
            Log.d(TAG, "origin predict result:" + Arrays.toString(result));
            long end = System.currentTimeMillis();
            long time = end - start;
            Log.d("result length", String.valueOf(result.length));
            // show predict result and time
            int r = get_max_result(result);
            String show_text = "result：" + r + "\nprobability：" + result[r] + "\ntime：" + time + "ms";
            result_text.setText(show_text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int get_max_result(float[] result) {
        float probability = result[0];
        int r = 0;
        for (int i = 0; i < result.length; i++) {
            if (probability < result[i]) {
                probability = result[i];
                r = i;
            }
        }
        return r;
    }

    // request permissions
    private void request_permissions() {

        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        // if list is not empty will request permissions
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {

                        int grantResult = grantResults[i];
                        if (grantResult == PackageManager.PERMISSION_DENIED) {
                            String s = permissions[i];
                            Toast.makeText(this, s + " permission was denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        // clear model before destroy app
        clear_model();
        super.onDestroy();
    }
}