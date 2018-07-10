package camera.jp.co.abs.camera;

import android.os.Environment;

import java.io.File;

public class Storage {

    /** ストレージ管理
     *　今回は、自作のフォルダーに見に行くためのファイルパスを作成
     */
    private static String sRoot = Environment.getExternalStorageDirectory().toString();
    private static String DIRECTORY_ORIGINAL = "SaveToImage_Iwamoto";

    public static String generateOriginal() {
        return new File(sRoot, DIRECTORY_ORIGINAL).toString();
    }
}
