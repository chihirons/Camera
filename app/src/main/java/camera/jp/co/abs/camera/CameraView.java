package camera.jp.co.abs.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;


public class CameraView extends TextureView{
    //FIELD_MEMBER
    private Activity mactivity; //activity
    private Handler uiHandler; //UIHandler
    private Handler workHandler; //workHandler
    private boolean active; //アクティブ
    private CameraManager mManager;

    //CAMERA_MEMBER
    private String cameraId; //Camera内外のID
    private CameraCharacteristics cameraInfo; //Camera情報
    private Size previewSize; //Previewサイズ
    private Size pictureSize; //写真サイズ
    private CameraDevice cameraDevice; //CameraDevice
    private CaptureRequest.Builder previweBilder; //Previewビルダー
    private CameraCaptureSession previewSession; //PreviewSession


    //コンストラクタ
    public CameraView(Context context){
        super(context);
        mactivity = (Activity)context;
        active = false;

        //handler生成
        uiHandler = new Handler();
        HandlerThread thread = new HandlerThread("work");
        thread.start();
        workHandler = new Handler(thread.getLooper());

        //Cameraマネージャーの取得
        mManager = (CameraManager)mactivity.getSystemService(Context.CAMERA_SERVICE);

        //TextureViewのリスナー指定
        setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            //Texture開始時
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                startCamera();
            }

            //Textureのサイズ変更があったとき
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            //Textureが消滅したとき
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                //Cameraの停止
                stopCamera();
                return true;
            }

            //Textureが変化したとき
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    //Cameraの開始
    @SuppressLint("MissingPermission")
    private void startCamera(){
        try {
            //Cameraの取得
            cameraId = getCameraId();
            cameraInfo = mManager.getCameraCharacteristics(cameraId);

            //Previewサイズと写真サイズ取得
            previewSize = getPreviewSize(cameraInfo);
            pictureSize = getPictureSize(cameraInfo);

            //CameraOpen
            mManager.openCamera(cameraId, new CameraDevice.StateCallback() {

                //接続
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;

                    //Preview開始
                    startPreview();
                }

                //切断
                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                    //Cameraの内容を初期化
                    camera.close();
                    cameraDevice = null;
                }

                //エラー
                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    toast("CameraOpenに失敗");
                }
            }, null);
        }catch (SecurityException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
            toast(e.toString());
        }
    }

    //Camera停止
    private void stopCamera(){
        if (cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    //CameraID取得
    private String getCameraId(){
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
        StreamConfigurationMap map = cc.get(cc.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
        for (int i =0; i < sizes.length; i++){

            //サイズ2000*2000以下
            if (sizes[i].getWidth() < 2000 && sizes[i].getHeight() < 2000) {
                return sizes[i];
            }
        }
        return sizes[0];
    }

    //写真サイズの取得
    private Size getPictureSize(CameraCharacteristics cc){
        Size[] sizes = cc.get(cc.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
        for (int i = 0; i < sizes.length; i++){

            //サイズ2000*2000以下の場合
            if (sizes[i].getWidth() < 2000 && sizes[i].getHeight() < 2000){
                return sizes[i];
            }
        }
        return null;
    }

    //Previewの開始
    private void startPreview(){
        if (cameraDevice == null) return;
        active = true;

        //出力先となるTexture
        SurfaceTexture texture = getSurfaceTexture();

        if (texture == null) return;

        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = new Surface(texture);

        //PreviewSession生成
        try {
            previweBilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previweBilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {

                //成功時に呼ばれる
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    previewSession = session;
                    updatePreview();
                }

                //失敗時に呼ばれる
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    toast("PreviewSessionの生成に失敗");
                }
            }, null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    //Previewの更新
    protected void updatePreview(){
        if (cameraDevice == null) return;

        //オートフォーカスの指定
        previweBilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        //カメラ画像をTextureに表示
        try{
            previewSession.setRepeatingRequest(previweBilder.build(), null, workHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    //タッチ時に呼ばれる

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            if (!active) return true;

            active = false;

            //撮影
            takePicture();
        }
        return true;
    }

    //撮影
    private void takePicture(){
        if (cameraDevice == null) return;

        try {
            //出力先となるイメージリーダー
            ImageReader reader = ImageReader.newInstance(
                    pictureSize.getWidth(), pictureSize.getHeight(),ImageFormat.JPEG, 2
            );

            reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

                //イメージ可能時に呼ばれる
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;

                    try {
                        //画像のバイト配列の取得
                        image = reader.acquireLatestImage();
                        byte[] data = image2data(image);
                        savePhoto(data);
                    }catch (Exception e){
                        if (image != null) image.close();
                    }
                }
            }, workHandler);

            //CaptureSessionの開始
            final CaptureRequest.Builder captureBilder =
                    cameraDevice.createCaptureRequest(cameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBilder.addTarget(reader.getSurface());
            captureBilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBilder.set(CaptureRequest.JPEG_ORIENTATION, getPhotoOrientation());
            List<Surface> outputSurface = new LinkedList<>();
            outputSurface.add(reader.getSurface());

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {

                //成功時に呼ばれる
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBilder.build(), new CameraCaptureSession.CaptureCallback() {

                            //完了時に呼ばれる
                            @Override
                            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber);

                                toast("撮影成功");

                                //Preview再開
                                startPreview();
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
                    startPreview();
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

    //写真の向きの計算
    private int getPhotoOrientation(){
        int displayRotation = 0;
        int rotation = mactivity.getWindowManager().getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_0) displayRotation = 0;
        if (rotation == Surface.ROTATION_90) displayRotation = 90;
        if (rotation == Surface.ROTATION_180) displayRotation = 180;
        if (rotation == Surface.ROTATION_270) displayRotation = 270;

        int sensorOrientation = cameraInfo.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return (sensorOrientation - displayRotation + 360) % 360;
    }

    //写真の保存
    private void savePhoto(byte[] data){
        try {
            //保存先パス生成
            SimpleDateFormat format = new SimpleDateFormat(
                    "'IMG' _yyyyMMdd_HHmmss'.jpg'", Locale.getDefault()
            );

            String fileName = format.format(
                    new Date(System.currentTimeMillis())
            );

            String path = Environment.getExternalStorageDirectory() + "/" + fileName;

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
