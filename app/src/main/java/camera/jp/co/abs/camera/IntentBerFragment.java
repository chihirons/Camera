package camera.jp.co.abs.camera;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_intent_ber, container, false);

        //ボタンの取得
        ImageView image = (ImageView) view.findViewById(R.id.intent_button);

        image.setOnClickListener(imageGallery);

        return view;
    }

    public View.OnClickListener imageGallery = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Intent galleryActivity = new Intent();

            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivity(intent);
        }
    };
}
