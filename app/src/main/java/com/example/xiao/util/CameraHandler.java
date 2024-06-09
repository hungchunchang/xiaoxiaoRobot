package com.example.xiao.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.fragment.app.Fragment;

import com.example.xiao.viewmodel.RobotViewModel;

public class CameraHandler {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private final Fragment fragment;
    private final RobotViewModel robotViewModel;


    public CameraHandler(Fragment fragment, RobotViewModel robotViewModel) {
        this.fragment = fragment;
        this.robotViewModel = robotViewModel;
    }

    public void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(fragment.requireActivity().getPackageManager()) != null) {
            fragment.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // 通知 ViewModel 處理圖片
            robotViewModel.handleCapturedImage(imageBitmap);
        }
    }
}
