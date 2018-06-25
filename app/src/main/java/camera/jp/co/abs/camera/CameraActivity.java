package camera.jp.co.abs.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class CameraActivity extends AppCompatActivity{

    private final static int REQUEST_PERMISSIONS = 0;
    private final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
        );

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        checkPermissions();
    }

    private void checkPermissions(){
        //パーミッション許可
        if (isGranted()){ //成功
            initContentView();
        }else{ //失敗
            ActivityCompat.requestPermissions(this,PERMISSIONS,REQUEST_PERMISSIONS);
        }
    }

    //Userが許可したかどうか
    private boolean isGranted(){
        for (String PERMISSION : PERMISSIONS) {
            if (PermissionChecker.checkSelfPermission(
                    CameraActivity.this, PERMISSION) != PackageManager.PERMISSION_GRANTED
                    ) {
                return false;
            } //forExit
        }
        return true;
    }

    //許可ダイアログ選択時に呼ばれる

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            //許可
            if (isGranted()) {
                initContentView();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //contentViewの初期化
    private void initContentView(){
        Log.d("DEBUG", "Empty");
        setContentView(new CameraView(this));
    }
}