package camera.jp.co.abs.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import camera.jp.co.abs.camera.FragmentCamera;
import camera.jp.co.abs.camera.FragmentImageGet;
import camera.jp.co.abs.camera.ItemFragment;
import camera.jp.co.abs.camera.dummy.DummyContent;

public class CameraActivity extends AppCompatActivity implements ItemFragment.OnListFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showCameraFragment();
            }
    }

    private void setFragment(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.content_frame,fragment);
        transaction.commit();
    }

    @Override
    public void onListFragmentInteraction(DummyContent.DummyItem item) {

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showCameraFragment() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},1001);
            return;
            }

            FragmentCamera f = new FragmentCamera();
        this.setFragment(f);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != 1001) {
            return;
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            FragmentCamera f = new FragmentCamera();
            this.setFragment(f);
        } else {

        }
    }
}