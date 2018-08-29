package foadkhezri.github.com.latestnews;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class NewsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ArrayList<News>>,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private static final int NEWS_LOADER_ID = 1;
    ListView listView;
    NewsAdapter newsAdapter;
    ArrayList<News> arrayList = new ArrayList<>();
    ProgressBar progressBar;
    TextView emptyView;
    SharedPreferences sharedPrefs;
    SwipeRefreshLayout refreshLayout;
    String orderBy;

    public static final String URL = "http://content.guardianapis.com/search?";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);
        listView = findViewById(R.id.list);
        progressBar = findViewById(R.id.progressBar);
        refreshLayout = findViewById(R.id.pullToRefresh);
        emptyView = findViewById(R.id.empty_view);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
                refreshLayout.setRefreshing(false);
            }
        });
        refreshData();
        listView.setEmptyView(emptyView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = arrayList.get(position).getUrl();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        newsAdapter = new NewsAdapter(this, arrayList);
        listView.setAdapter(newsAdapter);
    }

    @Override
    public Loader<ArrayList<News>> onCreateLoader(int id, Bundle args) {
        String query;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        orderBy = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default)
        );
        Uri baseUri = Uri.parse(URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        uriBuilder.appendQueryParameter("q", orderBy);
        query = uriBuilder.toString().concat("&api-key=efaa61ed-3d3b-4b92-9ec4-cc89b0af17af");
        return new NewsAsyncTask(this, query);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<News>> loader, ArrayList<News> data) {
        newsAdapter.clear();
        if (data == null) {
            return;
        }
        emptyView.setText(R.string.no_news_found);
        newsAdapter.addAll(data);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<News>> loader) {
        newsAdapter.clear();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.settings_order_by_key))){
            // Clear the ListView as a new query will be kicked off
            newsAdapter.clear();

            // Hide the empty state text view as the loading indicator will be displayed
            emptyView.setVisibility(View.GONE);

            // Show the loading indicator while new data is being fetched
            View loadingIndicator = findViewById(R.id.progressBar);
            loadingIndicator.setVisibility(View.VISIBLE);

            // Restart the loader to requery the USGS as the query settings have been updated
            getLoaderManager().restartLoader(NEWS_LOADER_ID, null, this);
        }
    }

    private static class NewsAsyncTask extends AsyncTaskLoader<ArrayList<News>> {
        String mUrl;
        public NewsAsyncTask(Context context, String url) {
            super(context);
            mUrl = url;
        }

        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } catch (IOException e) {
                // TODO: Handle the exception
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;
        }
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }
        private ArrayList<News> extractFeatureFromJson(String earthquakeJSON) {
            ArrayList<News> newsArrayList = new ArrayList<>();
            try {
                JSONObject baseJsonObject = new JSONObject(earthquakeJSON);
                JSONObject jsonObject = baseJsonObject.getJSONObject("response");
                JSONArray newsArray = jsonObject.getJSONArray("results");
                for (int i = 0; i < newsArray.length(); i++) {
                    JSONObject currentNews = newsArray.getJSONObject(i);
                    String title = currentNews.getString("webTitle");
                    String date = currentNews.getString("webPublicationDate");
                    String url = currentNews.getString("webUrl");
                    News news = new News(date,title,url);
                    newsArrayList.add(news);
                }

            } catch (JSONException e) {
                Log.e("QueryUtils", "Problem parsing the earthquake JSON results", e);
            }
            return newsArrayList;
        }

        @Override
        public ArrayList<News> loadInBackground() {
            URL url = null;
            try {
                url = new URL(mUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                assert url != null;
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                // TODO Handle the IOException
            }

            // Extract relevant fields from the JSON response and create an {@link Event} object
            // Return the {@link Event} object as the result fo the {@link TsunamiAsyncTask}
            return extractFeatureFromJson(jsonResponse);
        }
        @Override
        protected void onStartLoading() {
            forceLoad();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return  true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
              //  startActivity(new Intent(this, About.class));
                return true;
            case R.id.filter:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void refreshData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // And register to be notified of preference changes
        // So we know when the user has adjusted the query settings
        prefs.registerOnSharedPreferenceChangeListener(this);
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        assert manager != null;
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()) {
            LoaderManager loaderManager = getLoaderManager();
            loaderManager.initLoader(NEWS_LOADER_ID, null, this);

        } else {
            progressBar.setVisibility(View.GONE);
            emptyView.setText(R.string.no_internet_connection);
        }
    }
}
