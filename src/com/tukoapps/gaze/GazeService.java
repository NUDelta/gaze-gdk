package com.tukoapps.gaze;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
//import com.google.android.glass.timeline.TimelineManager;


import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;

public class GazeService extends Service {

    private static final String LIVE_CARD_TAG = "LiveCardDemo";

    //private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;
    private RemoteViews mLiveCardView;
    private ImageView image;
    private String requestUrl;

    private int homeScore, awayScore;
    private Random mPointsGenerator;

    private final Handler mHandler = new Handler();
    private final UpdateLiveCardRunnable mUpdateLiveCardRunnable =
        new UpdateLiveCardRunnable();
    private static final long DELAY_MILLIS = 30000;

    @Override
    public void onCreate() {
        super.onCreate();
        //mTimelineManager = TimelineManager.from(this);
        mPointsGenerator = new Random();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {

            // Get an instance of a live card
            //mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_TAG);
        	mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            // Inflate a layout into a remote view
            mLiveCardView = new RemoteViews(getPackageName(),
                R.layout.main_layout);
            
//            mLiveCardView.setTextViewText(R.id.textviewdes,
//                    "Hey there" + getLastLocation(this).getLatitude());
            //GetXMLTask task = new GetXMLTask();
            // Execute the task
            //task.execute(new String[] { "http://distilleryimage4.ak.instagram.com/98f17df8cbda11e3bd4b0002c99af64c_8.jpg" });
            Location loc = GazeService.getLastLocation(GazeService.this);
            String requestUrl = "https://api.instagram.com/v1/media/search?lat="+loc.getLatitude()+"&lng="+loc.getLongitude()+"&distance=10&access_token=257974112.b828a5d.1090e8d181b64d81a2d653d2dc60ffcd";
            JSONObject json = null;
            JSONArray array = null;
            String picloc = "";
            try {
				json = readJsonFromUrl(requestUrl);
				array = json.getJSONArray("data");
				json = array.getJSONObject(0);
				json = json.getJSONObject("images");
				json = json.getJSONObject("standard_resolution");
				picloc = json.getString("url");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            Log.d("JSON", picloc);
            GetXMLTask task = new GetXMLTask();
            task.execute(new String[] { picloc });
            mLiveCard.setViews(mLiveCardView);

            //image = (ImageView) (new RemoteViews(getPackageName(), R.id.picture));

            // Set up the live card's action with a pending intent
            // to show a menu when tapped
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(
                this, 0, menuIntent, 0));

            // Publish the live card
            mLiveCard.publish(PublishMode.REVEAL);

            // Queue the update text runnable
            mHandler.post(mUpdateLiveCardRunnable);
        }
        return START_STICKY;
    }
    
    public static Location getLastLocation(Context context) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.NO_REQUIREMENT);
        List<String> providers = manager.getProviders(criteria, true);
        List<Location> locations = new ArrayList<Location>();
        for (String provider : providers) {
             Location location = manager.getLastKnownLocation(provider);
             if (location != null && location.getAccuracy() !=0.0) {
                 locations.add(location);
             }
        }
        Collections.sort(locations, new Comparator<Location>() {
            @Override
            public int compare(Location location, Location location2) {
                return (int) (location.getAccuracy() - location2.getAccuracy());
            }
        });
        if (locations.size() > 0) {
            return locations.get(0);
        }
        return null;
   }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
          //Stop the handler from queuing more Runnable jobs
            mUpdateLiveCardRunnable.setStop(true);

            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }

    /**
     * Runnable that updates live card contents
     */
    private class UpdateLiveCardRunnable implements Runnable{

        private boolean mIsStopped = false;

        /*
         * Updates the card with a fake score every 30 seconds as a demonstration.
         * You also probably want to display something useful in your live card.
         *
         * If you are executing a long running task to get data to update a
         * live card(e.g, making a web call), do this in another thread or
         * AsyncTask.
         */
        public void run(){
            if(!isStopped()){
              // Generate fake points.
//                Location loc = GazeService.getLastLocation(GazeService.this);
//                String requestUrl = "https://api.instagram.com/v1/media/search?lat="+loc.getLatitude()+"&lng="+loc.getLongitude()+"&distance=10&access_token=257974112.b828a5d.1090e8d181b64d81a2d653d2dc60ffcd";
//                JSONObject json = null;
//                JSONArray array = null;
//                String picloc = "";
//                try {
//					json = readJsonFromUrl(requestUrl);
//					array = json.getJSONArray("data");
//					json = array.getJSONObject(0);
//					json = json.getJSONObject("images");
//					json = json.getJSONObject("standard_resolution");
//					picloc = json.getString("url");
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (JSONException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//                Log.d("JSON", picloc);
//                GetXMLTask task = new GetXMLTask();
//                task.execute(new String[] { picloc });
                mLiveCardView.setTextViewText(R.id.textviewdes,
                        "Hey there" + GazeService.getLastLocation(GazeService.this).getLatitude());
                // Update the remote view with the new scores.
                Log.d("LOOK HERE", "home score: " + homeScore);
                // Always call setViews() to update the live card's RemoteViews.
                mLiveCard.setViews(mLiveCardView);

                // Queue another score update in 30 seconds.
                mHandler.postDelayed(mUpdateLiveCardRunnable, DELAY_MILLIS);
            }
        }

        public boolean isStopped() {
            return mIsStopped;
        }

        public void setStop(boolean isStopped) {
            this.mIsStopped = isStopped;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
      /*
       *  If you need to set up interprocess communication
       * (activity to a service, for instance), return a binder object
       * so that the client can receive and modify data in this service.
       *
       * A typical use is to give a menu activity access to a binder object
       * if it is trying to change a setting that is managed by the live card
       * service. The menu activity in this sample does not require any
       * of these capabilities, so this just returns null.
       */
        return null;
    }
    
    private class GetXMLTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            Bitmap map = null;
            for (String url : urls) {
                map = downloadImage(url);
            }
            return map;
        }
 
        // Sets the Bitmap returned by doInBackground
        @Override
        protected void onPostExecute(Bitmap result) {
        	mLiveCardView.setImageViewBitmap(R.id.picture, result);
        	mLiveCard.setViews(mLiveCardView);
        }
 
        // Creates Bitmap from InputStream and returns it
        private Bitmap downloadImage(String url) {
            Bitmap bitmap = null;
            InputStream stream = null;
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inSampleSize = 1;
 
            try {
                stream = getHttpConnection(url);
                bitmap = BitmapFactory.
                        decodeStream(stream, null, bmOptions);
                stream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return bitmap;
        }
 
        // Makes HttpURLConnection and returns InputStream
        private InputStream getHttpConnection(String urlString)
                throws IOException {
            InputStream stream = null;
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
 
            try {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                httpConnection.setRequestMethod("GET");
                httpConnection.connect();
 
                if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    stream = httpConnection.getInputStream();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return stream;
        }
    }
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
          sb.append((char) cp);
        }
        return sb.toString();
      }

      public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
          BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
          String jsonText = readAll(rd);
          JSONObject json = new JSONObject(jsonText);
          return json;
        } finally {
          is.close();
        }
      }
}