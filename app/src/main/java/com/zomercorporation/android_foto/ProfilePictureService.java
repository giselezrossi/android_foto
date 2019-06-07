package com.zomercorporation.android_foto;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;

import com.soundcloud.android.crop.Crop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProfilePictureService {

    private static final String TAG = "ProfilePictureService";

    private Activity _activity;
    private ImageFileInfo _photoFileInfo;
    private ImageFileInfo _croppedPhotoFileInfo;

    private OnProfilePictureUploaded _onProfilePictureUploaded;
    private OnFail _onFail;

    public static final int PROFILE_PICTURE_FROM_CAMERA_REQUEST_CODE = 1235;
    public static final int REQUEST_PERMISSION_FOR_CAMERA_CODE = 1236;

    private static final int ONE_MEGA_BYTE = 1000000;

    public ProfilePictureService(Activity activity, OnProfilePictureUploaded onProfilePictureUploaded, OnFail onFail) {
        this._activity = activity;
        this._onProfilePictureUploaded = onProfilePictureUploaded;
        this._onFail = onFail;
    }

    public void askForPermissionAndShowCamera() {
        boolean hasPermissionForCamera = ActivityCompat.checkSelfPermission(_activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
        if (hasPermissionForCamera) {
            String[] cameraPermission = new String [] { Manifest.permission.CAMERA };
            ActivityCompat.requestPermissions(_activity, cameraPermission, REQUEST_PERMISSION_FOR_CAMERA_CODE);
        } else {
            showCamera();
        }

    }

    public void pickPictureFromGallery() {
        Crop.pickImage(_activity);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PROFILE_PICTURE_FROM_CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            onImageTakenFromCamera();
        } else if (requestCode == PROFILE_PICTURE_FROM_CAMERA_REQUEST_CODE) {
            onFail();
        } else if (requestCode == Crop.REQUEST_PICK && resultCode == Activity.RESULT_OK) {
            onImageSelectedFromGallery(data);
        } else if (requestCode == Crop.REQUEST_PICK) {
            onFail();
        } else if (requestCode == Crop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            onImageCropped();
        } else if (requestCode == Crop.REQUEST_CROP) {
            onFail();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_FOR_CAMERA_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCamera();
            }
        }
    }

    private File compressImage(Bitmap bitmap) {
        ImageFileInfo compressedPhotoFileInfo = createImageFile();
        File compressedPhotoFile = new File(compressedPhotoFileInfo.absoluteFilePath);
        FileOutputStream out;
        try {
            out = new FileOutputStream(compressedPhotoFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        return compressedPhotoFile;
    }

    private void onImageCropped() {
        Bitmap bitmap = BitmapFactory.decodeFile(_croppedPhotoFileInfo.absoluteFilePath, null);
        int byteCount = bitmap.getByteCount();
        File file;
        if (byteCount >= ONE_MEGA_BYTE) {
            file = compressImage(bitmap);
        } else {
            file = new File(_croppedPhotoFileInfo.absoluteFilePath);
        }


        //App.instance().user.photo = Utils.encodeTobase64(bitmap);
//        UserService.instance().updatePhoto();
         _onProfilePictureUploaded.run(bitmap);


    }


    private void onImageTakenFromCamera() {
        crop();
    }

    private void onImageSelectedFromGallery(Intent data) {
        _photoFileInfo = new ImageFileInfo();
        _photoFileInfo.uri = data.getData();
        crop();
    }

    private void crop() {
        _croppedPhotoFileInfo = createImageFile();
        Crop.of(_photoFileInfo.uri, _croppedPhotoFileInfo.uri).withAspect(/*x*/256, /*y*/256).start(_activity);
    }

    private ImageFileInfo createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = _activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File imageFile = File.createTempFile(/*prefix*/imageFileName, /*suffix*/".jpg", /*directory*/storageDir);
            String absoluteFilePath = imageFile.getAbsolutePath();
            Uri photoURI = FileProvider.getUriForFile(_activity, "com.zomercorporation.mac.fileprovider", imageFile);
            ImageFileInfo info = new ImageFileInfo();
            info.absoluteFilePath = absoluteFilePath;
            info.uri = photoURI;
            return info;
        } catch (IOException ex) {
            return null;
        }
    }

    private void showCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(_activity.getPackageManager()) != null) {
            // Create the File where the photo should go
            this._photoFileInfo = createImageFile();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, this._photoFileInfo.uri);
            _activity.startActivityForResult(takePictureIntent, PROFILE_PICTURE_FROM_CAMERA_REQUEST_CODE);
        }
    }

    public byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    private void onFail() {
        _onFail.run();
    }

    private static class ImageFileInfo {
        Uri uri;
        String absoluteFilePath;
    }

    public interface OnProfilePictureUploaded {
        void run(Bitmap user);
    }

    public interface OnFail {
        void run();
    }

}
