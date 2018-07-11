package camera.jp.co.abs.camera;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

public class IntentBerFragment extends Fragment {

    /** 作成したファイルパスと一致するものはないか（最後尾検索） */
    private static final String CAMERA_PATH = Storage.generateOriginal() + "%";
    /** 一致したファイルの中身をあいまい検索 */
    private static final String SELECT_BY_PATH = MediaStore.MediaColumns.DATA + " LIKE ?";

    //変数
    //shardPreference change;

    Context context;
    Bitmap bmp;

    /**
     * 画面作成の設定
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    /**
     * 画面が破棄された場合の画面保存
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    /**
     * 画面開始時に呼び出される
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_intent_ber, container, false);

        //ボタンの取得
        final ImageView image = view.findViewById(R.id.intent_button);

        //更新までの時間を稼ぐためフリーズ
        image.setEnabled(false);
        new Handler().postDelayed(new Runnable() { //UIに反映させるためにhandler制御
            @Override
            public void run() {

                //画像の調整
                bmp = selectPhotoPicture();

                //画像の出力を変える
                if (bmp != null) {
                    image.setImageBitmap(bmp);
                }else{
                    image.setImageResource(R.drawable.ic_photo_black_24dp);
                }

                //画像のロック解除
                image.setEnabled(true);
            }
        }, 500);

        //クリックされた時の処理
        image.setOnClickListener(imageGallery);

        return view;
    }

    /**
     * このPhotoアプリで撮影した最新の画像の出力と圧縮
     * @return
     */
    @SuppressLint("Recycle")
    private Bitmap selectPhotoPicture(){
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        Bitmap bmp = null;

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        // 取得するフィールド
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.SIZE
        };
        String selection = SELECT_BY_PATH + " AND " + MediaStore.MediaColumns._ID + " > ?";
        String[] selectionArgs = new String[] { CAMERA_PATH, Long.toString(-1) };
        // 降順検索
        String sortOrder = MediaStore.Images.Media._ID + " desc";

        try{
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);

        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(context, "例外が発生、Permissionを許可していますか？", Toast.LENGTH_SHORT).show();
            return null;
        }

        assert cursor != null;
        if (!cursor.moveToFirst()) return null;

        //Uriからファイル名を取得
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID));
        Uri uriPath = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));

        try {
            bmp = getBitmap(uriPath);
        }catch (IOException e){
            e.printStackTrace();
        }

        return bmp;
    }


    //Bitmapのサイズ調整
    private Bitmap getBitmap(Uri imageUri) throws IOException{
        // 画像出力用の圧縮
        BitmapFactory.Options mOptions = new BitmapFactory.Options();
        // 画像の画素数を操作できる
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap resizeBitmap;
        InputStream is = context.getContentResolver().openInputStream(imageUri);

        //画像を出力するためBitmapに変換する
        resizeBitmap = BitmapFactory.decodeStream(is, null, mOptions);
        assert is != null;
        is.close();

        return resizeBitmap;
    }

    /**
     * 画像タップ時の処理(Fragmentのボタンをタップした場合にIntentするようにするボタン)
     */
    public View.OnClickListener imageGallery = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (bmp != null) {
                Intent galleryIntent = new Intent(getActivity(), galleryPopupActivity.class);
                startActivity(galleryIntent);
            }
        }
    };
}
