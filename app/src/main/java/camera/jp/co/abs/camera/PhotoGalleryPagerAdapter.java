package camera.jp.co.abs.camera;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class PhotoGalleryPagerAdapter extends PagerAdapter{

    /** コンテキスト */
    private Context mContext;

    /** ContentResolver. */
    private ContentResolver mResolver;

    /** IDのリスト */
    private ArrayList<Long> mList;

    /**
     * コンストラクタ.
     * @param context
     */
    PhotoGalleryPagerAdapter(Context context){
        this.mContext = context;
        this.mResolver = mContext.getContentResolver();
        this.mList = new ArrayList<>();
    }

    /**
     * アイテムを追加する
     * @param id
     */
    public void add(Long id){
        this.mList.add(id);
    }

    /**
     * アイテムを追加する際に呼ばれる
     * @param container
     * @param position
     * @return
     */
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {

        //リストから取得
        Long id = this.mList.get(position);
        Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString());
        Bitmap bitmap = null;

        try {
            bitmap = getBitmap(uri);
        }catch (IOException e){
            e.printStackTrace();
        }

        //Viewを生成
        ImageView imageView = new ImageView(mContext);
        imageView.setImageBitmap(bitmap);

        //コンテナに追加
        container.addView(imageView);

        return imageView;
    }

    /**
     * アイテムを消去する際に呼ばれ、このメソッド内でViewの消去をする
     * @param container
     * @param position
     * @param object
     */
    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        //コンテナから View を削除
        container.removeView((View) object);
    }

    /**
     * アイテムを登録する数
     * @return
     */
    @Override
    public int getCount() {
        return this.mList.size();
    }

    /**
     * ObjectにViewが入っているか
     * @param view
     * @param object
     * @return
     */
    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view.equals(object);
    }


    //Bitmapのサイズ調整
    private Bitmap getBitmap(Uri imageUri) throws IOException{
        //画像出力用の圧縮
        BitmapFactory.Options mOptions = new BitmapFactory.Options();
        // 画像の画素数を操作できる
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap resizeBitmap;
        InputStream is = mResolver.openInputStream(imageUri);

        //画像を出力するためBitmapに変換する
        resizeBitmap = BitmapFactory.decodeStream(is, null, mOptions);
        assert is != null;
        is.close();

        return resizeBitmap;
    }
}
