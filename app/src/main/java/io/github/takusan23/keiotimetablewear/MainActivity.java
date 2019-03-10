package io.github.takusan23.keiotimetablewear;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;

import io.github.takusan23.keiotimetablewear.Adapter.ListAdapter;
import io.github.takusan23.keiotimetablewear.Adapter.ListItem;
import io.github.takusan23.keiotimetablewear.Utilities.ArrayListSharedPreferences;

public class MainActivity extends WearableActivity {

    private WearableNavigationDrawerView mWearableNavigationDrawer;
    private ListView listView;
    private SharedPreferences pref_setting;
    private ArrayList<ListItem> arrayList;
    private ListAdapter adapter;
    private SQLiteTimeTable helper;
    private SQLiteDatabase sqLiteDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pref_setting = PreferenceManager.getDefaultSharedPreferences(this);
        if (helper == null) {
            helper = new SQLiteTimeTable(MainActivity.this);
        }
        if (sqLiteDatabase == null) {
            sqLiteDatabase = helper.getWritableDatabase();
        }

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
                        adapter.clear();
                        loadNearAndTimeStation("home_near_name", "up", "新宿方面");
                        loadNearAndTimeStation("home_near_name", "down", "京王八王子・高尾山口方面");
                        loadNearAndTimeStation("home_near_name", "up_holiday", "休日 新宿方面");
                        loadNearAndTimeStation("home_near_name", "down_holiday", "休日 京王八王子・高尾山口方面");
                        break;
                    case 2:
                        adapter.clear();
                        loadNearAndTimeStation("work_near_name", "up", "新宿方面");
                        loadNearAndTimeStation("work_near_name", "down", "京王八王子・高尾山口方面");
                        loadNearAndTimeStation("work_near_name", "up_holiday", "休日 新宿方面");
                        loadNearAndTimeStation("work_near_name", "down_holiday", "休日 京王八王子・高尾山口方面");
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

    /**
     * 自宅・最寄りの今の時間に近い時刻表を表示
     *
     * @param name         StationName
     * @param up_down      上り下り休日上り下り
     * @param up_down_text 上りだよって文章
     */
    private void loadNearAndTimeStation(String name, String up_down, String up_down_text) {
        //一時
        int temp = 0;
        //現在の時刻
        String hour = String.valueOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        //データベース表示
        //データ取り出し
        Cursor cursor = sqLiteDatabase.query(
                "stationdb",
                new String[]{"station", "memo", "up_down", "url", "css_1", "css_2", "time", "hour", "minute"},
                "station=?",
                new String[]{pref_setting.getString(name, "") + "-" + up_down},
                null,
                null,
                null
        );
        //はじめに移動
        cursor.moveToFirst();

        //取り出し
        for (int i = 0; i < cursor.getCount(); i++) {
            try {
                JSONArray text_JsonArray = new JSONArray(cursor.getString(6));
                JSONArray url_JsonArray = new JSONArray(cursor.getString(3));
                JSONArray css_1_JsonArray = new JSONArray(cursor.getString(4));
                JSONArray css_2_JsonArray = new JSONArray(cursor.getString(5));
                JSONArray hour_JsonArray = new JSONArray(cursor.getString(7));
                JSONArray minute_JsonArray = new JSONArray(cursor.getString(8));

                for (int json_count = 0; json_count < text_JsonArray.length(); json_count++) {
                    //現在の時間
                    if (((String) hour_JsonArray.get(json_count)).contains(hour + "時")) {
                        //分
                        String minute = numberOnly(minute_JsonArray.getString(json_count));
                        String minute_old = numberOnly(minute_JsonArray.getString(json_count + 1));
                        //一個先とと比較？
                        temp = Integer.valueOf(minute) - Calendar.getInstance().get(Calendar.MINUTE);
                        if (temp > Integer.valueOf(minute_old) - Calendar.getInstance().get(Calendar.MINUTE)) {
                            //0時のとき10時や20時が出ないようにする
                            if (Calendar.getInstance().get(Calendar.MINUTE) >= 9) {
                                //1xと2x時は対象外へ
                                if (!hour_JsonArray.getString(json_count).contains("1" + String.valueOf(hour)) && !hour_JsonArray.getString(json_count).contains("2" + String.valueOf(hour))) {
                                    ArrayList<String> item = new ArrayList<>();
                                    item.add("station_list");
                                    item.add(up_down_text + "\n" + (String) text_JsonArray.get(json_count));
                                    item.add("");
                                    item.add("https://keio.ekitan.com/sp/" + url_JsonArray.get(json_count));
                                    item.add((String) css_1_JsonArray.get(json_count));
                                    item.add((String) css_2_JsonArray.get(json_count));
                                    final ListItem listItem = new ListItem(item);
                                    //UIスレッド限定な模様
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            adapter.add(listItem);
                                        }
                                    });
                                }
                            } else {
                                ArrayList<String> item = new ArrayList<>();
                                item.add("station_list");
                                item.add(up_down_text + "\n" + (String) text_JsonArray.get(json_count));
                                item.add("");
                                item.add("https://keio.ekitan.com/sp/" + url_JsonArray.get(json_count));
                                item.add((String) css_1_JsonArray.get(json_count));
                                item.add((String) css_2_JsonArray.get(json_count));
                                final ListItem listItem = new ListItem(item);
                                //UIスレッド限定な模様
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.add(listItem);
                                    }
                                });
                            }
                        }
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


            //次の項目へ
            cursor.moveToNext();
        }

        //最後
        cursor.close();
        // ListViewにArrayAdapter
        listView.setAdapter(adapter);

    }

    private String numberOnly(String text) {
        text = text.replace("分", "");
        text = text.replace("本", "");
        text = text.replace("府", "");
        text = text.replace("調", "");
        text = text.replace("シン", "");
        text = text.replace("桜", "");
        text = text.replace("瑞", "");
        text = text.replace("山", "");
        text = text.replace("高", "");
        text = text.replace("つ", "");
        text = text.replace("(", "");
        text = text.replace(")", "");
        text = text.replace(" ", "");
        return text;
    }


}
