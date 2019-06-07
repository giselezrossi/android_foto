package com.zomercorporation.android_foto;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private ImageView photo;
    ProfilePictureService profilePictureService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        photo = findViewById(R.id.photo);

        profilePictureService = new ProfilePictureService(this, new ProfilePictureService.OnProfilePictureUploaded() {
            @Override
            public void run(Bitmap user) {
                photo.setImageBitmap(user);
                //  binder.profileCircleOnEditScreen.setImageBitmap(Utils.decodeBase64(user.photo));
            }
        }, new ProfilePictureService.OnFail() {
            @Override
            public void run() {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });

            }
        });

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askForPermissionAndShowCamera();
            }
        });

    }

    private void askForPermissionAndShowCamera() {
        profilePictureService.askForPermissionAndShowCamera();
    }

}
