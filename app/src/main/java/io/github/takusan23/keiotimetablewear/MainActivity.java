package io.github.takusan23.keiotimetablewear;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import io.github.takusan23.keiotimetablewear.Adapter.ListAdapter;
import io.github.takusan23.keiotimetablewear.Adapter.ListItem;
import io.github.takusan23.keiotimetablewear.Utilities.ArrayListSharedPreferences;

public class MainActivity extends WearableActivity {

    private WearableNavigationDrawerView mWearableNavigationDrawer;
    private ListView listView;
    private SharedPreferences pref_setting;
    private ArrayList<ListItem> arrayList;
    private ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pref_setting = PreferenceManager.getDefaultSharedPreferences(this);
        // Top Navigation Drawer
        mWearableNavigationDrawer =
                (WearableNavigationDrawerView) findViewById(R.id.top_navigation_drawer);
        mWearableNavigationDrawer.setAdapter(new NavigationAdapter(this));
        // Peeks navigation drawer on the top.
        mWearableNavigationDrawer.getController().peekDrawer();
        mWearableNavigationDrawer.addOnItemSelectedListener(new WearableNavigationDrawerView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int i) {
                switch (i) {
                    case 0:
                        loadFavouriteStationList();
                        break;
                    case 1:
                        loadNearStation("home_near_url");
                        break;
                    case 2:
                        loadNearStation("work_near_url");
                        break;
                    case 3:
                        loadTimeTable();
                        break;
                }
            }
        });

        listView = findViewById(R.id.wear_listview);

        //ListView
        arrayList = new ArrayList<>();
        adapter = new ListAdapter(MainActivity.this, R.layout.listview_layout, arrayList);

        //さいしょによみこむやーつ
        loadFavouriteStationList();

        // Enables Always-on
        setAmbientEnabled();
    }

    //メニューにあるアイコン、タイトルの設定
    private final class NavigationAdapter
            extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {

        private final Context mContext;

        public NavigationAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            //メニューの数？
            return 4;
        }

        @Override
        public String getItemText(int pos) {
            //なんかサンプルからかけ離れた実装方法だけどこれでいいわ
            String title = "ホーム";
            switch (pos) {
                case 0:
                    title = "お気に入り";
                    break;
                case 1:
                    title = "自宅最寄り";
                    break;
                case 2:
                    title = "仕事\n学校最寄り";
                    break;
                case 3:
                    title = "駅一覧";
                    break;
            }
            return title;
        }

        @Override
        public Drawable getItemDrawable(int pos) {
            //アイコンとか
            //これもサンプルからかけ離れた実装だから
            Drawable drawable = getDrawable(R.drawable.ic_train_white_24dp);
            switch (pos) {
                case 0:
                    drawable = getDrawable(R.drawable.ic_favorite_border_white_24dp);
                    break;
                case 1:
                    drawable = getDrawable(R.drawable.ic_home_white_24dp);
                    break;
                case 2:
                    drawable = getDrawable(R.drawable.ic_work_white_24dp);
                    break;
                case 3:
                    drawable = getDrawable(R.drawable.ic_train_white_24dp);
                    break;
            }

            return drawable;
        }
    }

    /**
     * 時刻表を読み込む
     */
    private void loadTimeTable() {
        //ListView
        ArrayList<ListItem> arrayList = new ArrayList<>();
        final ListAdapter adapter = new ListAdapter(MainActivity.this, R.layout.listview_layout, arrayList);

        //駅一覧
        String[] a = getResources().getStringArray(R.array.keio_station);
        //for
        for (int i = 1; i < a.length; i++) {
            //Adapter用List
            ArrayList<String> item = new ArrayList<>();
            item.add("station_list");
            item.add(a[i - 1] + " / KO-" + String.valueOf((i)));
            item.add(a[i - 1]);
            item.add(urlGenerator(i));
            item.add("");
            item.add("");
            ListItem listItem = new ListItem(item);
            adapter.add(listItem);
        }

        // ListViewにArrayAdapter
        listView.setAdapter(adapter);

    }

    //URL生成
    private String urlGenerator(int station_number) {
        //上り
        String url = "https://keio.ekitan.com/pc/T5?slCode=";


        String d = "&d=1&dw=0";

        //初台
        if (station_number == 2 || station_number == 3) {
            url += "263-" + String.valueOf(station_number - 1) + d;
        } else if (station_number >= 35 && station_number <= 45) {
            //京王多摩川とか
            url += "261-" + String.valueOf(station_number - 34) + d;
        } else if (station_number == 46) {
            //府中競馬
            url += "265-1" + d;
        } else if (station_number == 47) {
            //動物公園
            url += "260-1" + d;
        } else if (station_number >= 48) {
            //京王片倉駅
            url += "264-" + String.valueOf(station_number - 47) + d;
        } else if (station_number == 1) {
            //新宿
            url += "262-0" + d;
        } else {
            //笹塚から京王八王子まで
            url += "262-" + String.valueOf(station_number - 3) + d;
        }
        //System.out.println("リンク : " + url);
        return url;
    }

    /**
     * お気に入り一覧
     */
    private void loadFavouriteStationList() {
        adapter.clear();
        final ArrayList<String> name = ArrayListSharedPreferences.loadSharedPreferencesArrayList("favourite_name", pref_setting);
        final ArrayList<String> url = ArrayListSharedPreferences.loadSharedPreferencesArrayList("favourite_url", pref_setting);
        for (int i = 0; i < name.size(); i++) {
            //Adapter用List
            ArrayList<String> item = new ArrayList<>();
            item.add("favourite_list");
            item.add(name.get(i));
            item.add(name.get(i));
            item.add(url.get(i));
            item.add("");
            item.add("");
            ListItem listItem = new ListItem(item);
            adapter.add(listItem);
            listView.setAdapter(adapter);
        }
    }

    /**
     * 自宅、会社最寄り駅時刻表読み込み
     *
     * @param name 読み込むPreferenceの名前
     */
    private void loadNearStation(String name) {
        if (name.length() != 0) {
            Intent intent = new Intent(this, TimeTableActivity.class);
            intent.putExtra("URL", pref_setting.getString(name, ""));
            intent.putExtra("name", name);
            startActivity(intent);
        } else {
            Toast.makeText(this, "登録されていません", Toast.LENGTH_SHORT).show();
        }
    }


}
