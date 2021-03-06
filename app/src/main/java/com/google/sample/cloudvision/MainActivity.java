/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.cloudvision;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "YOUR_API_KEY";
    public static final String FILE_NAME = "temp.jpg";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private TextView mImageDetails;
    private ImageView mMainImage;
    private long startTimeMS;
    private float uploadDurationSec;

    private TextView textNo;
    private TextView textNama;
    private TextView textTtl;
    private TextView textJk;
    private TextView textAlamat1;
    private TextView textAlamat2;
    private TextView textAlamat3;
    private TextView textAlamat4;
    private TextView textAgama;
    private TextView textMasa;
    private String[] ary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setMessage(R.string.dialog_select_prompt)
                        .setPositiveButton(R.string.dialog_select_gallery, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startGalleryChooser();
                            }
                        })
                        .setNegativeButton(R.string.dialog_select_camera, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startCamera();
                            }
                        });
                builder.create().show();
                textNo.setText("");
                textNama.setText("");
                textTtl.setText("");
                textJk.setText("");
                textAlamat1.setText("");
                textAlamat2.setText("");
                textAlamat3.setText("");
                textAlamat4.setText("");
                textAgama.setText("");
                textMasa.setText("");
//                Arrays.fill(ary, null);
                ary = new String[35];
                System.out.println(ary.length);
            }
        });

        mImageDetails = (TextView) findViewById(R.id.image_details);
        mMainImage = (ImageView) findViewById(R.id.main_image);

        textNo = (TextView) findViewById(R.id.textNo);
        textNama = (TextView) findViewById(R.id.textNama);
        textTtl = (TextView) findViewById(R.id.textTtl);
        textJk = (TextView) findViewById(R.id.textJk);
        textAlamat1 = (TextView) findViewById(R.id.textAlamat1);
        textAlamat2 = (TextView) findViewById(R.id.textAlamat2);
        textAlamat3 = (TextView) findViewById(R.id.textAlamat3);
        textAlamat4 = (TextView) findViewById(R.id.textAlamat4);
        textAgama = (TextView) findViewById(R.id.textAgama);
        textMasa = (TextView) findViewById(R.id.textBerlaku);

    ary = new String[35];
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getCameraFile()));
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            uploadImage(Uri.fromFile(getCameraFile()));
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public void uploadImage(Uri uri) {
        startTimeMS = System.currentTimeMillis();
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                1200);

                callCloudVision(bitmap);
                mMainImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Switch text to loading
        mImageDetails.setText(R.string.loading_message);

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(new
                            VisionRequestInitializer(CLOUD_VISION_API_KEY));
                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("TEXT_DETECTION");
                            labelDetection.setMaxResults(10);
                            add(labelDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    long sendMS = System.currentTimeMillis();
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    uploadDurationSec = (System.currentTimeMillis() - sendMS) / 1000f;
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {
//                mImageDetails.setText(result); // semua data ditampilkan
                mImageDetails.setText("Data masuk");

                if (ary != null) {
//                    textNo.setText("NIK :" + ary[3]);
//                    textNama.setText("Nama :" + ary[4]);
//                    textTtl.setText("TTL :" + ary[6]);
//                    textJk.setText("Jenis Kelamin :" + ary[7]);
//                    textAlamat1.setText("Alamat:" + ary[10]);
//                    textAlamat2.setText("RT/RW :" + ary[12]);
//                    textAlamat3.setText("Kelurahan :" + ary[13]);
//                    textAlamat4.setText("Kota:" + ary[14]);
//                    textAgama.setText("Agama :" + ary[15]);
//                    textMasa.setText("Masa Berlaku :" + ary[22]);

                    for (int i = 0; i < ary.length; i++) {
                        if (ary[i].toLowerCase().replace(" ","").matches(".*"+"nik".toLowerCase()+".*")){
                            if (!ary[i].substring(3).isEmpty()){
                                textNo.setText("NIK :" + ary[i].substring(4).replace(" ",""));
                            }else {
                                textNo.setText("NIK :" + ary[i + 1].replace(" ","").replaceAll("[^\\d.]",""));
                            }
                        }
                        if (ary[i].toLowerCase().replace(" ","").matches(".*"+"nama".toLowerCase()+".*")){
                            if (!ary[i].substring(4).isEmpty()){
                                textNama.setText("Nama :" + ary[i].substring(4));
                            }else {
                                textNama.setText("Nama :" + ary[i + 1]);
                            }
                        }
                        if (ary[i].toLowerCase().replace(" ","").matches(".*"+"lahir".toLowerCase()+".*")){
                            if (!ary[i].substring(15).isEmpty()){
                                textTtl.setText("TTL :" + ary[i].substring(15));
                            }else {
                                textTtl.setText("TTL :" + ary[i + 1]);
                            }
                        }
                        if (ary[i].toLowerCase().replace(" ","").matches(".*"+"kelamin".toLowerCase()+".*")){
                            if (!ary[i].substring(13).isEmpty()){
                                textJk.setText("Jenis Kelamin :" + ary[i].substring(13).replace("Gol Darah",""));
                            }else {
                                textJk.setText("Jenis Kelamin :" + ary[i + 1].replace("Gol Darah", ""));
                            }
                        }
                        if (ary[i].toLowerCase().replace(" ","").matches(".*"+"alamat".toLowerCase()+".*")){
                            if (!ary[i].substring(6).isEmpty()){
                                textAlamat1.setText("Alamat:" + ary[i].substring(6));
                            }else {
                                textAlamat1.setText("Alamat:" + ary[i + 1]);
                            }
                        }
                        if (ary[i].toLowerCase().replace(" ","").matches(".*"+"rw".toLowerCase()+".*")){
                            if (!ary[i].substring(4).isEmpty()){
                                textAlamat2.setText("RT/RW :" + ary[i].substring(4));
                            }else {
                                textAlamat2.setText("RT/RW :" + ary[i + 1]);
                            }
                        }
                        if (ary[i].toLowerCase().replace(" ","").matches(".*"+"kelurah".toLowerCase()+".*")){
                            if (!ary[i].substring(6).isEmpty()){
                                textAlamat3.setText("Kelurahan :" + ary[i].substring(6));
                            }else {
                                textAlamat3.setText("Kelurahan :" + ary[i + 1]);
                            }
                        }
                        if (ary[i].toLowerCase().replace(" ","").matches(".*"+"kecamatan".toLowerCase()+".*")){
                            if (!ary[i].substring(9).isEmpty()){
                                textAlamat4.setText("Kecamatan:" + ary[i].substring(9));
                            }else {
                                textAlamat4.setText("Kecamatan:" + ary[i + 1]);
                            }
                        }
                        if (ary[i].toLowerCase().replace(" ","").matches(".*"+"gama".toLowerCase()+".*")){
                            if (!ary[i].substring(5).isEmpty()){
                                textAgama.setText("Agama :" + ary[i].substring(5));
                            }else {
                                textAgama.setText("Agama :" + ary[i + 1]);
                            }
                        }
                        if (ary[i].toLowerCase().replace(" ","").matches(".*"+"berlak".toLowerCase()+".*")){
                            if (!ary[i].substring(14).isEmpty()){
                                textMasa.setText("Masa Berlaku :" + ary[i].substring(14));
                            }else {
                                textMasa.setText("Masa Berlaku :" + ary[i + 1]);
                            }
                        }
                    }

//                    System.out.println(nik);
                }else{
                    textNo.setText("DATA TIDAK TERSEDIA");
                }
            }
        }.execute();
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int originalWidth = bitmap.getWidth();
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        long spentMS = System.currentTimeMillis() - startTimeMS;
        String message = "Recognition results:\n";
        StringBuilder builder = new StringBuilder(message);
        builder.append(String.format("(Total spent %.2f secs, including %.2f secs for upload)\n\n", spentMS / 1000f, uploadDurationSec));

        List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
        Log.i("JackTest", "total labels:" + labels.size());
        if (labels != null) {
            for (int i = 0; i < labels.size(); i++ ) {
                EntityAnnotation label = labels.get(i);
                if (i == 0) {
                    builder.append("Locale: ");
                    builder.append(label.getLocale());
                }
                builder.append(label.getDescription());
                ary=label.getDescription().split("\n");
                builder.append("\n");
                //TODO: Draw rectangles later
                break;
            }
        } else {
            builder.append("nothing");
        }
        return builder.toString();
    }


}
