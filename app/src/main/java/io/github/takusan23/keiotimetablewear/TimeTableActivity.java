package io.github.takusan23.keiotimetablewear;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;

import io.github.takusan23.keiotimetablewear.Adapter.ListAdapter;
import io.github.takusan23.keiotimetablewear.Adapter.ListItem;
import io.github.takusan23.keiotimetablewear.Utilities.ArrayListSharedPreferences;

public class TimeTableActivity extends WearableActivity {

    private ListView listView;
    private WearableActionDrawerView mWearableActionDrawer;
    private String up_url;
    private String name;
    private ArrayList<ListItem> arrayList;
    private ListAdapter adapter;
    private SharedPreferences pref_setting;

    private SQLiteTimeTable helper;
    private SQLiteDatabase sqLiteDatabase;

    private ArrayList<String> text_ArrayList = new ArrayList<>();
    private ArrayList<String> url_ArrayList = new ArrayList<>();
    private ArrayList<String> css_1_ArrayList = new ArrayList<>();
    private ArrayList<String> css_2_ArrayList = new ArrayList<>();
    private ArrayList<String> hour_ArrayList = new ArrayList<>();
    private ArrayList<String> minute_ArrayList = new ArrayList<>();
    //上り？
    private boolean up_train = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_table);
        pref_setting = PreferenceManager.getDefaultSharedPreferences(TimeTableActivity.this);
        final SharedPreferences.Editor editor = pref_setting.edit();

        listView = findViewById(R.id.timetable_listview);

        up_url = getIntent().getStringExtra("URL");
        name = getIntent().getStringExtra("name");


        if (helper == null) {
            helper = new SQLiteTimeTable(TimeTableActivity.this);
        }
        if (sqLiteDatabase == null) {
            sqLiteDatabase = helper.getWritableDatabase();
        }

        //ListView
        arrayList = new ArrayList<>();
        adapter = new ListAdapter(TimeTableActivity.this, R.layout.listview_layout, arrayList);

        // Bottom Action Drawer
        mWearableActionDrawer =
                (WearableActionDrawerView) findViewById(R.id.bottom_action_drawer);
        //登録済みか確認
        //登録
        //配列取得
        final ArrayList<String> station_name = ArrayListSharedPreferences.loadSharedPreferencesArrayList("favourite_name", pref_setting);
        final ArrayList<String> station_url = ArrayListSharedPreferences.loadSharedPreferencesArrayList("favourite_url", pref_setting);
        //配列追加
        //同じものがあったら削除
        if (station_name.contains(name)) {
            mWearableActionDrawer.getMenu().findItem(R.id.station_add_menu).setTitle("お気に入り解除").setIcon(R.drawable.ic_remove_white_24dp);
        } else {
            mWearableActionDrawer.getMenu().findItem(R.id.station_add_menu).setTitle("お気に入り登録").setIcon(R.drawable.ic_favorite_border_white_24dp);
        }

        // Peeks action drawer on the bottom.
        mWearableActionDrawer.getController().peekDrawer();
        mWearableActionDrawer.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //メニュー選択
                switch (item.getItemId()) {
                    case R.id.train_up_down_menu:
                        if (item.getTitle().toString().contains("新宿方面\n→京王八王子方面")) {
                            up_url = up_url.replace("d=1", "d=2");
                            item.setTitle("京王八王子\n→新宿方面");
                            if (checkSQLiteTimeTableData(TimeTableActivity.this.name) != 0) {
                                loadSQLiteTimeTable("down");
                            } else {
                                getHTMLAndPerse(up_url);
                            }
                            up_train = false;
                            item.setIcon(R.drawable.ic_arrow_downward_white_24dp);
                        } else {
                            up_url = up_url.replace("d=2", "d=1");
                            if (checkSQLiteTimeTableData(TimeTableActivity.this.name) != 0) {
                                loadSQLiteTimeTable("up");
                            } else {
                                getHTMLAndPerse(up_url);
                            }
                            up_train = true;
                            item.setTitle("新宿方面\n→京王八王子方面");
                            item.setIcon(R.drawable.ic_arrow_upward_white_24dp);
                        }
                        break;
                    case R.id.day_menu:
                        if (item.getTitle().toString().contains("平日\n→休日")) {
                            up_url = up_url.replace("dw=0", "dw=1");
                            if (checkSQLiteTimeTableData(TimeTableActivity.this.name) != 0) {
                                if (up_train) {
                                    loadSQLiteTimeTable("up_holiday");
                                } else {
                                    loadSQLiteTimeTable("down_holiday");
                                }
                            } else {
                                getHTMLAndPerse(up_url);
                            }
                            item.setTitle("休日\n→平日");
                            item.setIcon(R.drawable.ic_home_white_24dp);
                        } else {
                            up_url = up_url.replace("dw=1", "dw=0");
                            if (checkSQLiteTimeTableData(TimeTableActivity.this.name) != 0) {
                                //上りの休日
                                if (up_train) {
                                    loadSQLiteTimeTable("up");
                                } else {
                                    loadSQLiteTimeTable("down");
                                }
                            } else {
                                getHTMLAndPerse(up_url);
                            }
                            item.setTitle("平日\n→休日");
                            item.setIcon(R.drawable.ic_work_white_24dp);
                        }
                        break;
                    case R.id.station_add_menu:
                        //同じものがあったら削除
                        if (station_name.contains(name)) {
                            station_name.remove(name);
                            station_url.remove(up_url);
                            //タイトル切り替え
                            item.setTitle("お気に入り解除");
                            item.setIcon(R.drawable.ic_remove_white_24dp);

                        } else {
                            station_name.add(name);
                            station_url.add(up_url);
                            //タイトル切り替え
                            item.setTitle("お気に入り登録");
                            item.setIcon(R.drawable.ic_favorite_border_white_24dp);
                        }
                        //保存
                        ArrayListSharedPreferences.saveArrayListSharedPreferences(station_name, "favourite_name", pref_setting);
                        ArrayListSharedPreferences.saveArrayListSharedPreferences(station_url, "favourite_url", pref_setting);
                        break;
                    //登録
                    case R.id.home_near_menu:
                        editor.putString("home_near_url", getIntent().getStringExtra("URL"));
                        editor.apply();
                        break;
                    case R.id.work_near_menu:
                        editor.putString("work_near_url", getIntent().getStringExtra("URL"));
                        editor.apply();
                        break;
                    case R.id.download_menu:
                        saveSQLite(getIntent().getStringExtra("URL"), "up");
                        break;
                }
                mWearableActionDrawer.getController().peekDrawer();
                return false;
            }
        });

        //読み込み
        //ダウンロード済みとか
        if (checkSQLiteTimeTableData(this.name) != 0) {
            loadSQLiteTimeTable("up");
            downloadMenu();
        } else {
            getHTMLAndPerse(up_url);
        }


        // Enables Always-on
        setAmbientEnabled();
    }

    /**
     * 時刻表読み込み
     */
    private void getHTMLAndPerse(final String url) {
        adapter.clear();
        //ネットワークは非同期処理
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... aVoid) {

                try {
                    //時刻表URL
                    Document doc = Jsoup.connect(url).get();
                    //HTML Table
                    //なんか３個めで行けた
                    final Element tables = doc.select("table").get(3);
                    final Elements tr = tables.select("tr");
                    for (int i = 3; i < tr.size(); i++) {
                        final int finalI = i;
                        //Tableの要素
                        final Elements td = tr.get(finalI).select("td");
                        //時間取り出し
                        //Class
                        //平日　weekday
                        //休日　holiday
                        String class_name = "weekday";
                        if (url.contains("&dw=0")) {
                            class_name = "weekday";
                        } else {
                            class_name = "holiday";
                        }
                        final Elements time = tr.get(finalI).getElementsByClass(class_name);

                        //到着取り出し
                        final Elements time_td = tr.get(finalI).getElementsByClass("jikokuhyo");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                String hour = "";
                                String minute = "";

                                //余分にforが回っている
                                //時刻表最後まで終わったら終了するようにする
                                if (time.text().length() > 0) {
                                    hour = time.text() + "時 ";
                                    //到着
                                    for (int train = 0; train < time_td.size(); train++) {
                                        minute = time_td.get(train).text() + "分";
                                        //Class(CSS)取得→各駅、区間急行等
                                        String css = time_td.get(train).select("a").get(0).select("span").get(0).className();
                                        String css_2nd = time_td.get(train).select("a").get(0).select("span").select("span").get(1).className();
                                        //電車URL
                                        String train_info = time_td.get(train).select("a").attr("href");
                                        //いろいろ
                                        String text = hour + minute.replace("(", "").replace(")", "");

                                        //Adapter用List
                                        ArrayList<String> item = new ArrayList<>();
                                        item.add("time_table_list");
                                        item.add(text);
                                        item.add("");
                                        item.add("https://keio.ekitan.com/sp/" + train_info);
                                        item.add(css);
                                        item.add(css_2nd);
                                        ListItem listItem = new ListItem(item);
                                        adapter.add(listItem);
                                        listView.setAdapter(adapter);
                                    }
                                }
                            }
                        });

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //SQLite
    private void saveSQLite(final String url, final String mode) {
        text_ArrayList.clear();
        url_ArrayList.clear();
        css_1_ArrayList.clear();
        css_2_ArrayList.clear();
        hour_ArrayList.clear();
        minute_ArrayList.clear();
        //ネットワークは非同期処理
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... aVoid) {

                try {
                    //時刻表URL
                    Document doc = Jsoup.connect(url).get();
                    //HTML Table
                    //なんか３個めで行けた
                    final Element tables = doc.select("table").get(3);
                    final Elements tr = tables.select("tr");
                    for (int i = 3; i < tr.size(); i++) {
                        final int finalI = i;
                        //Tableの要素
                        final Elements td = tr.get(finalI).select("td");
                        //時間取り出し
                        //Class
                        //平日　weekday
                        //休日　holiday
                        String class_name = "weekday";
                        if (url.contains("&dw=0")) {
                            class_name = "weekday";
                        } else {
                            class_name = "holiday";
                        }
                        final Elements time = tr.get(finalI).getElementsByClass(class_name);

                        //到着取り出し
                        final Elements time_td = tr.get(finalI).getElementsByClass("jikokuhyo");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                String hour = "";
                                String minute = "";

                                //余分にforが回っている
                                //時刻表最後まで終わったら終了するようにする
                                if (time.text().length() > 0) {
                                    hour = time.text() + "時 ";
                                    //到着
                                    for (int train = 0; train < time_td.size(); train++) {
                                        minute = time_td.get(train).text() + "分";
                                        //Class(CSS)取得→各駅、区間急行等
                                        String css = time_td.get(train).select("a").get(0).select("span").get(0).className();
                                        String css_2nd = time_td.get(train).select("a").get(0).select("span").select("span").get(1).className();
                                        //電車URL
                                        String train_info = time_td.get(train).select("a").attr("href");
                                        //いろいろ
                                        String text = hour + minute.replace("(", "").replace(")", "");

                                        //SQLite準備
                                        text_ArrayList.add(text);
                                        css_1_ArrayList.add(css);
                                        css_2_ArrayList.add(css_2nd);
                                        url_ArrayList.add(train_info);
                                        hour_ArrayList.add(hour);
                                        minute_ArrayList.add(minute);

                                    }
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }


                return null;
            }

            @Override
            protected void onPostExecute(final Void aVoid) {
                super.onPostExecute(aVoid);

                //ArrayListを変換
                //DB保存
                ContentValues values = new ContentValues();
                values.put("station", TimeTableActivity.this.name + "-" + mode);
                values.put("memo", "");
                values.put("up_down", mode);
                values.put("url", ArrayListSharedPreferences.setArrayListToJSONArray(url_ArrayList).toString());
                values.put("css_1", ArrayListSharedPreferences.setArrayListToJSONArray(css_1_ArrayList).toString());
                values.put("css_2", ArrayListSharedPreferences.setArrayListToJSONArray(css_2_ArrayList).toString());
                values.put("time", ArrayListSharedPreferences.setArrayListToJSONArray(text_ArrayList).toString());
                values.put("hour", ArrayListSharedPreferences.setArrayListToJSONArray(hour_ArrayList).toString());
                values.put("minute", ArrayListSharedPreferences.setArrayListToJSONArray(minute_ArrayList).toString());

                //すでにある場合は削除する？
                sqLiteDatabase.delete("stationdb", "station=?", new String[]{TimeTableActivity.this.name + "-" + mode});
                //登録
                sqLiteDatabase.insert("stationdb", null, values);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mode.contains("up") && !mode.contains("up_holiday")) {
                            Toast.makeText(TimeTableActivity.this, "上りダウンロード完了", Toast.LENGTH_SHORT).show();
                            saveSQLite(getIntent().getStringExtra("URL").replace("d=1", "d=2"), "down");
                        }
                        if (mode.contains("down") && !mode.contains("down_holiday")) {
                            Toast.makeText(TimeTableActivity.this, "下りダウンロード完了", Toast.LENGTH_SHORT).show();
                            String getURL = getIntent().getStringExtra("URL");
                            getURL = getURL.replace("dw=0", "dw=1");
                            getURL = getURL.replace("d=2", "d=1");
                            saveSQLite(getURL, "up_holiday");
                        }
                        if (mode.contains("up_holiday")) {
                            Toast.makeText(TimeTableActivity.this, "休日上りダウンロード完了", Toast.LENGTH_SHORT).show();
                            String getURL = getIntent().getStringExtra("URL");
                            getURL = getURL.replace("dw=0", "dw=1");
                            getURL = getURL.replace("d=1", "d=2");
                            saveSQLite(getURL, "down_holiday");
                        }
                        if (mode.contains("down_holiday")) {
                            Toast.makeText(TimeTableActivity.this, "休日下りダウンロード完了", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * SQLiteに駅の時刻表データがあるか確認
     *
     * @return 0か1を返します。0のときは無く、1のときは有る状態です
     */
    private int checkSQLiteTimeTableData(String name) {
        int return_int = 0;
        //データ取り出し
        Cursor cursor = sqLiteDatabase.query(
                "stationdb",
                new String[]{"station", "memo", "up_down", "url", "css_1", "css_2", "time", "hour", "minute"},
                "station=?",
                new String[]{TimeTableActivity.this.name + "-up"},
                null,
                null,
                null
        );
        //はじめに移動
        cursor.moveToFirst();
        //取り出し
        for (int i = 0; i < cursor.getCount(); i++) {
            //確認
            if (cursor.getString(0).contains(name)) {
                return_int = 1;
            }
            //次の項目へ
            cursor.moveToNext();
        }
        //最後
        cursor.close();
        return return_int;
    }

    /**
     * SQLiteからデータを読み込む
     */
    private void loadSQLiteTimeTable(final String mode) {
        adapter.clear();
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... aVoid) {
                //データベース表示
                //データ取り出し
                Cursor cursor = sqLiteDatabase.query(
                        "stationdb",
                        new String[]{"station", "memo", "up_down", "url", "css_1", "css_2", "time", "hour", "minute"},
                        "station=?",
                        new String[]{TimeTableActivity.this.name + "-" + mode},
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

                        for (int json_count = 0; json_count < text_JsonArray.length(); json_count++) {
                            ArrayList<String> item = new ArrayList<>();
                            item.add("time_table_list");
                            item.add((String) text_JsonArray.get(json_count));
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

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    //次の項目へ
                    cursor.moveToNext();
                }

                //最後
                cursor.close();


                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Toast.makeText(TimeTableActivity.this, "ダウンロード済みのデータを表示中です", Toast.LENGTH_SHORT).show();
                listView.setAdapter(adapter);
                super.onPostExecute(aVoid);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * ダウンロード用メニュー
     */
    private void downloadMenu() {
        mWearableActionDrawer.getMenu().removeItem(R.id.download_menu);
        mWearableActionDrawer.getMenu().add(0, 1, 0, "ウェブから取得").setIcon(R.drawable.ic_language_white_24dp).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                getHTMLAndPerse(up_url);
                return false;
            }
        });
        mWearableActionDrawer.getMenu().add(0, 1, 0, "データ更新").setIcon(R.drawable.ic_autorenew_white_24dp).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                saveSQLite(getIntent().getStringExtra("URL"), "up");
                return false;
            }
        });
        mWearableActionDrawer.getMenu().add(0, 2, 0, "DLデータ削除").setIcon(R.drawable.ic_delete_white_24dp).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                sqLiteDatabase.delete("stationdb", "station=?", new String[]{TimeTableActivity.this.name + "-" + "up"});
                sqLiteDatabase.delete("stationdb", "station=?", new String[]{TimeTableActivity.this.name + "-" + "down"});
                sqLiteDatabase.delete("stationdb", "station=?", new String[]{TimeTableActivity.this.name + "-" + "up_holiday"});
                sqLiteDatabase.delete("stationdb", "station=?", new String[]{TimeTableActivity.this.name + "-" + "down_holiday"});
                return false;
            }
        });
    }

}
