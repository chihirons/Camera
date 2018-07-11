package camera.jp.co.abs.camera;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.content.Context.WINDOW_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 */

public class FragmentCamera extends Fragment {

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Member
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    private Context mParentActivity;
    private TextureView mTextureView;

    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private Size mPictureSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCaptureSession;

    private CameraCharacteristics cameraInfo;
    SimpleDateFormat format;
    String NAME;
    String path;

    private Handler uiHandler;
    private Handler workHandler;

    private Button button;


     ////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Listener
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * カメラ画面のリスナ
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            // Textureが有効化されたとき
            Log.d("DEBUG","Textureが有効化されたとき");

            prepareCameraView();
            }

            //TextureViewサイズ変更
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
            }

            //消滅
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                //Camera停止
                stopCamera();
                return true;
            }

            //変化
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            }

    };

    //Cameraが停止
    private void stopCamera(){
        if (mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    /**
     * 撮影ボタンタップ
     */
    private View.OnClickListener mButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            takePicture();

            //連射防止のため2.5秒間ボタンフリーズ
            button.setEnabled(false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    button.setEnabled(true);
                }
            }, 2000L);

        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Fragment
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * FragmentCameraが呼ばれた際のコンストラクタ
     */
    public FragmentCamera() {
        //handlerの生成
        uiHandler = new Handler();
        HandlerThread thread = new HandlerThread("work");
        thread.start();
        workHandler = new Handler(thread.getLooper());
    }

    /**
     * ボタンなどのレイヤー開示時の初期設定
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_fragment_camera, container, false);
        mTextureView = view.findViewById(R.id.texture_view);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

        button = view.findViewById(R.id.button_take_picture);
        button.setOnClickListener(mButtonOnClickListener);


        return view;
    }


    /**
     *  galleryへのボタンの設定Fragmentのネストで別Activityに飛ぶ
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        IntentBerFragment intentBerFragment = new IntentBerFragment();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.gallery,intentBerFragment);
        transaction.commit();
    }

    /**
     * 画面設定Fragmentの画面を得る
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mParentActivity = context;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Private Method
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * TextureViewが有効化されたらカメラを準備する
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void prepareCameraView() {
        //Camera
        CameraManager cameraManager = (CameraManager) mParentActivity.getSystemService(Context.CAMERA_SERVICE);
        try {

            //内部と外部あともう一個のIDを管理するためのもの
            String cameraId = getCameraId(cameraManager);
            assert cameraManager != null;
            assert cameraId != null;
            cameraInfo = cameraManager.getCameraCharacteristics(cameraId);

            //privateサイズと写真サイズ取得
            mPreviewSize = getPreviewSize(cameraInfo);
            mPictureSize = getPictureSize(cameraInfo);

            if (cameraInfo == null) return;

            //////////////////////////////////////////////////////////////////////////////////////////
            //Cameraの開始
            //////////////////////////////////////////////////////////////////////////////////////////

            Log.d("DEBUG","checkSelfPermission");
            if (ActivityCompat.checkSelfPermission(mParentActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
             
            Log.d("DEBUG","openCamera");
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() { ///ERROR出るかも
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d("DEBUG","CameraDeviceが有効化されたとき");
                    mCameraDevice = camera;
                    createCameraCaptureSession(); //Previewの開始
                }
 
                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    //Cameraの内容を初期化
                    camera.close();
                    mCameraDevice = null;
                }
 
                @Override
                public void onError(@NonNull CameraDevice camera, int i) {
                    camera.close();
                    mCameraDevice = null;
                    toast("CameraOpenに失敗");
                }
            }, null);

            //                this.configureTransform();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
 
    /**
     * CameraCaptureSessionを有効化する
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createCameraCaptureSession() {
        if (mCameraDevice  == null || !mTextureView.isAvailable() || mPreviewSize == null) return;

        //出力先となるTextureViewの取得
        SurfaceTexture texture =  mTextureView.getSurfaceTexture();

        if (texture == null) return;

        //TextureViewのサイズなどの取得,Surfaceの生成
        texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        Surface surface = new Surface(texture);

        try {
            //PreviewSessionの生成
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            } catch (CameraAccessException e) {
            e.printStackTrace();
            }
            mCaptureRequestBuilder.addTarget(surface);

        try {
            //PreviewSessionのコールバック
            mCameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {

                //成功時に呼ばれる
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d("DEBUG","Sessionが有効化された");
                    mCaptureSession = session;
                    updatePreview();
                }

                //失敗時に呼ばれる
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    toast("onConfigureFailed");
                    }
                }, null);

            } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
 
    /**
     * CaptureSessionから画像を繰り返し取得してTextureViewに表示する
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void updatePreview() {
        if (mCameraDevice == null) return;

        //オートフォーカスの指定
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        Log.d("DEBUG","リクエストスタート");
        try {
            mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),null,workHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    //撮影サイズの取得?
    /////////////////////////////////////////////////////////////////////////////////

    //CameraID取得
    private String getCameraId(CameraManager mManager){
        try {
            for (String cameraId : mManager.getCameraIdList()){
                CameraCharacteristics cameraInfo = mManager.getCameraCharacteristics(cameraId);

                //背面カメラ
                if (cameraInfo.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK){

                    return cameraId;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //Previewサイズの取得
    private Size getPreviewSize(CameraCharacteristics cc){
        StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        assert map != null;
        Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
        for (Size size : sizes) {

            //サイズ2000*2000以下
            if (size.getWidth() < 2000 && size.getHeight() < 2000) {
                return size;
            }
        }
        return sizes[0];
    }

    //写真サイズの取得
    private Size getPictureSize(CameraCharacteristics cc){
        Size[] sizes = Objects.requireNonNull(cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(ImageFormat.JPEG);
        for (Size size : sizes) {

            //サイズ2000*2000以下の場合
            if (size.getWidth() < 2000 && size.getHeight() < 2000) {
                return size;
            }
        }
        return null;
    }

    //写真の向きの計算
    private int getPhotoOrientation(){

        WindowManager windowManager = (WindowManager) mParentActivity.getSystemService(WINDOW_SERVICE);
        assert windowManager != null;
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int displayRotation = 0;
        if (rotation == Surface.ROTATION_0) displayRotation = 0;
        if (rotation == Surface.ROTATION_90) displayRotation = 90;
        if (rotation == Surface.ROTATION_180) displayRotation = 180;
        if (rotation == Surface.ROTATION_270) displayRotation = 270;

        int sensorOrientation = cameraInfo.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return (sensorOrientation - displayRotation + 360) % 360;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //撮影用のメソッド
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void takePicture(){
        if (mCameraDevice == null) return;

        try{
            //出力先となるイメージリーダー
            ImageReader reader = ImageReader.newInstance(mPictureSize.getWidth(), mPictureSize.getHeight(), ImageFormat.JPEG, 2);
            reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

                //イメージ可能時に呼ばれる
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try{
                        //画像のバイト配列を取得
                        image = reader.acquireLatestImage();
                        byte[] data = image2data(image);
                        savePhoto(data);
                    }catch (Exception e){
                        if (image != null) image.close();
                    }
                }
            }, workHandler);

            //CaptureSessionの開始
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //画像の調整
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getPhotoOrientation());
            List<Surface> outputSurface = new LinkedList<>();
            outputSurface.add(reader.getSurface());

            mCameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {

                //成功時に呼ばれる
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {

                            //完了時に呼ばれる
                            @Override
                            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber);

                                toast("撮影成功");

                                IntentBerFragment intentBerFragment = new IntentBerFragment();
                                FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                                transaction.replace(R.id.gallery,intentBerFragment);
                                transaction.commit();

                                //Preview再開
                                createCameraCaptureSession();
                            }
                        }, workHandler);
                    }catch (CameraAccessException e){
                        e.printStackTrace();
                    }
                }

                //失敗時に呼ばれる
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    toast("CaptureSessionの作成に失敗");

                    //Preview再開
                    createCameraCaptureSession();
                }
            }, workHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    //image→バイト配列
    private byte[] image2data(Image image){
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        byte[] data = new byte[buffer.capacity()];
        buffer.get(data);
        return data;
    }

    //写真の保存
    private void savePhoto(byte[] data){
        try {

            NAME = "SaveToImage_Iwamoto";

            //保存先パス生成
            format = new SimpleDateFormat(
                    "'IMG' _yyyyMMdd_HHmmss'.jpg'", Locale.getDefault()
            );

            String fileName = format.format(new Date(System.currentTimeMillis()));

            path = Environment.getExternalStorageDirectory() + "/" + NAME + "/" + fileName;

            //バイト配列の保存
            saveData(data, path);

            //フォトの登録
            MediaScannerConnection.scanFile(getContext(), new String[]{path}, null, null);
        }catch (Exception e){
            toast(e.toString());
        }
    }

    //バイト配列の保存
    private void saveData(byte[] w, String path) throws Exception{

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(path);
            out.write(w);
            out.close();
        }catch (Exception e){
            if (out != null) out.close();
            throw e;
        }
    }

    //トーストの表示
    private void toast(final String text){
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

 }