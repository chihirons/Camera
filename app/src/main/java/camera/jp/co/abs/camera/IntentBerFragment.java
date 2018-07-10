package camera.jp.co.abs.camera;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class IntentBerFragment extends Fragment {

    //変数
    //shardPreference change;

    Context context;

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
        ImageView image = view.findViewById(R.id.intent_button);

        image.setOnClickListener(imageGallery);

        return view;
    }


    /**
     * 画像タップ時の処理(Fragmentのボタンをタップした場合にIntentするようにするボタン)
     */
    public View.OnClickListener imageGallery = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent galleryIntent = new Intent(getActivity(), galleryPopupActivity.class);
            startActivity(galleryIntent);
        }
    };
}
