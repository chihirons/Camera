package camera.jp.co.abs.camera;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;

/**
 * 画像を使った ViewPager のサンプル.
 */
public class galleryPopupActivity extends FragmentActivity {

    /** ViewPager */
    private ViewPager mPager;
    /** 作成したファイルパスと一致するものはないか（最後尾検索） */
    private static final String CAMERA_PATH = Storage.generateOriginal() + "%";
    /** 一致したファイルの中身をあいまい検索 */
    private static final String SELECT_BY_PATH = MediaStore.MediaColumns.DATA + " LIKE ?";

    /**
     * ページャーの中身を作成する
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ViewPagerをレイアウトにセット
        mPager = new ViewPager(this);
        setContentView(mPager);

        //CursorLoaderを呼び出す
        getSupportLoaderManager().initLoader(0, null, callbacks);
    }

    /** CursorLoaderのコールバック. */
    private LoaderManager.LoaderCallbacks<Cursor> callbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        /**
         * 指定されたIDの新しいLoaderをインスタンス化する
         * @param id
         * @param bundle
         * @return
         */
        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle bundle) {
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

            return new CursorLoader(getApplicationContext(), uri, projection, selection, selectionArgs, sortOrder);
        }

        /**
         * 前に作成されたローダーがロードを完了したときに呼び出される
         * @param loader
         * @param c
         */
        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor c) {
            //CursorからIdを取得してPagerAdapterに入れる
            PhotoGalleryPagerAdapter adapter = new PhotoGalleryPagerAdapter(galleryPopupActivity.this);
            c.moveToFirst();

            do{
                //Uriからファイル名を取得
                long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID));
                adapter.add(id);
            }while (c.moveToNext());

            //ViewPagerにセット
            mPager.setAdapter(adapter);
        }

        /**
         * 前に作成されたローダーがリセットされデータ利用が不可能になったときに呼び出される
         * @param loader
         */
        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        }
    };
}
