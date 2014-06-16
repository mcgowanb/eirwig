package com.beezer.eirwig;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

public class MainActivity extends FragmentActivity implements
		OnItemSelectedListener {

	private GoogleMap mMap;
	static final LatLng Ireland = new LatLng(53.5056, -7.8289);
	static final LatLng IrelandCenter = new LatLng(53.4026137, -7.969749234);
	private UiSettings Settings;
	private HeatmapTileProvider csoProvider;
	private HeatmapTileProvider twitterProvider;
	private TileOverlay csoOverlay;
	private TileOverlay twitterOverlay;
	public static ArrayList<WeightedLatLng> weightedList = new ArrayList<WeightedLatLng>();
	private static ArrayList<LatLng> tweetSpots = new ArrayList<LatLng>();
	private ArrayList<Marker> popByNat = new ArrayList<Marker>();
	private ArrayList<Marker> unemployment = new ArrayList<Marker>();
	private ArrayList<Marker> singletons = new ArrayList<Marker>();
	private String stringUrl = "http://ec2-54-194-27-150.eu-west-1.compute.amazonaws.com:8080/eirwig-spring-mvc/TwitterIrelandREST";
	int twiterLoopCount = 0;
	private String lastTweet = "";
	private Bitmap masterImg;
	int badDataCount = 0;
	
	
	
	/* Recursive method called to continually loop the twitter feed,
	 * Called every second
	 */
	final static long REFRESH=1*1000;
	final static int SUBJECT=0;
	Handler locationHandler= new Handler(){
	    public void handleMessage(Message msg){
	        if (msg.what==SUBJECT){
	            plotTwitterMarker();
	            this.sendEmptyMessageDelayed(SUBJECT, REFRESH);
	        }
	    }
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		/** This disables the check to prevent network access in the main thread */
		/*StrictMode.ThreadPolicy policy = new StrictMode.
				ThreadPolicy.Builder().permitAll().build();
				StrictMode.setThreadPolicy(policy);  */ 

		Spinner csoSpinner = (Spinner) findViewById(R.id.cso_spinner);

		ArrayAdapter<CharSequence> cso_adapter = ArrayAdapter
				.createFromResource(this, R.array.cso_selection_array,
						R.layout.spinner_item);
		cso_adapter
				.setDropDownViewResource(R.layout.spinner_dropdown);
		csoSpinner.setAdapter(cso_adapter);
		csoSpinner.setOnItemSelectedListener(this);

		Spinner twitter_spinner = (Spinner) findViewById(R.id.twitter_spinner);
		ArrayAdapter<CharSequence> twitter_adapter = ArrayAdapter
				.createFromResource(this, R.array.twitter_selection_array,
						R.layout.spinner_item);
		twitter_adapter
				.setDropDownViewResource(R.layout.spinner_dropdown);
		twitter_spinner.setAdapter(twitter_adapter);
		twitter_spinner.setOnItemSelectedListener(this);

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);

		if (savedInstanceState == null) {
			mapFragment.setRetainInstance(true);
		} else {
			mMap = mapFragment.getMap();
		}

		try {
			setUpMapIfNeeded();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override		//Create menu icons
	public boolean onCreateOptionsMenu(Menu menu){
	// Inflate the menu items for use in the action bar
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_activity_actions, menu);
    return super.onCreateOptionsMenu(menu);
	}
	
	@Override		//action methods for menu options
	public boolean onOptionsItemSelected(MenuItem item) {
	    int itemId = item.getItemId();
		if (itemId == R.id.action_hybrid) {
			mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
			return true;
		} else if (itemId == R.id.action_normal) {
			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			return true;
		} else if (itemId == R.id.action_sattelite) {
			mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			return true;
		} else if (itemId == R.id.action_terrain) {
			mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
			return true;
		} else if (itemId == R.id.action_about) {
			Intent i = new Intent(getApplicationContext(),DisplayAppInfo.class);
			startActivity(i);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			setUpMapIfNeeded();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setUpMapIfNeeded() throws IOException {
		if (mMap == null) {
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			if (mMap != null) {
				configureMap();
				locateMap(IrelandCenter);
				
				try {
					loadCSOData();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				addCSOHeatMap();
			}
		}
	}

	private void configureMap() throws IOException {
		mMap.setMyLocationEnabled(true);
		mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		Settings = mMap.getUiSettings();
		Settings.setRotateGesturesEnabled(false);
		Settings.setZoomGesturesEnabled(true);
		Settings.setCompassEnabled(true);
		mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
	}

	private void locateMap(LatLng location) {
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 7));
	}

	private void addCSOHeatMap() throws IOException {
		csoProvider = new HeatmapTileProvider.Builder()
				.weightedData(weightedList).radius(50).build();
		csoOverlay = mMap.addTileOverlay(new TileOverlayOptions()
				.tileProvider(csoProvider));
	}
	
	private void twitterHeatMap() throws IOException {
		twitterProvider = new HeatmapTileProvider.Builder()
				.weightedData(weightedList).radius(50).build();
		twitterOverlay = mMap.addTileOverlay(new TileOverlayOptions()
				.tileProvider(csoProvider));
	}


	public void clear(View view) {
		mMap.clear();
		Toast.makeText(this, "Resetting map data", Toast.LENGTH_SHORT).show();
		try {
			addCSOHeatMap();
			locateMap(IrelandCenter);
			loadCSOData();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void loadCSOData() throws JSONException {

		InputStream inputStream = getResources()
				.openRawResource(R.raw.cso_data);
		String json = new Scanner(inputStream).useDelimiter("\\A").next();
		JSONArray array = new JSONArray(json);
		for (int i = 0; i < array.length(); i++) {
			JSONObject object = array.getJSONObject(i);
			Double lat = object.getDouble("LATTITUDE");
			Double lng = object.getDouble("LONGITUDE");
			int pop = object.getInt("T1_1AGETT");
			String city = object.getString("GEOGDESC");
			int employmentMale = object.getInt("T8_1_TM");
			int employmentFemale = object.getInt("T8_1_TF");
			int atWorkMale = object.getInt("T8_1_WM");
			int atWorkFemale = object.getInt("T8_1_WF");
			int unemployedMale = (object.getInt("T8_1_LFFJM") + object
					.getInt("T8_1_ULGUPJM"));
			int unemployedFemale = (object.getInt("T8_1_LFFJF") + object
					.getInt("T8_1_ULGUPJF"));
			int atWorkPercent = (atWorkMale + atWorkFemale) * 100
					/ (employmentMale + employmentFemale);
			int unemployedPercent = (unemployedMale + unemployedFemale) * 100
					/ (employmentMale + employmentFemale);
			int irishNatPercent = (object.getInt("T2_1IEBP") * 100 / (object
					.getInt("T1_1AGETT")));
			int britishNatPercent = (object.getInt("T2_1UKBP") * 100 / (object
					.getInt("T1_1AGETT")));
			int polishNatPercent = (object.getInt("T2_1PLBP") * 100 / (object
					.getInt("T1_1AGETT")));
			int lituanNatPercent = (object.getInt("T2_1LTBP") * 100 / (object
					.getInt("T1_1AGETT")));
			int singleMale = (object.getInt("T1_2SGLM"));
			int singleFemale = (object.getInt("T1_2SGLF"));

			LatLng latlng = new LatLng(lat, lng);
			WeightedLatLng wLatLng = new WeightedLatLng(latlng, pop);
			weightedList.add(wLatLng);
			String titledata = city;
			String popInfo = "Total Population: " + pop
					+ " \nIrish nationals: " + irishNatPercent
					+ "% \nBritish nationals: " + britishNatPercent
					+ "% \nPolish nationals: " + polishNatPercent
					+ "% \nLituanian nationals: " + lituanNatPercent + "%";
			String employInfo = "Available workforce: "
					+ (employmentMale + employmentFemale) + "\nEmployed: "
					+ atWorkPercent + "% \nUnemployed: " + unemployedPercent
					+ "%";
			String singleData = "Total Singletons: " + (singleMale + singleFemale)
					+ "\nSingle Males: " + singleMale 
					+ "\nSingle Females: " + singleFemale;

			Marker popMarker = mMap.addMarker(new MarkerOptions()
							.position(new LatLng(lat, lng))
							.draggable(false)
							.title(titledata)
							.snippet(popInfo));
			popMarker.setVisible(false);
			popByNat.add(popMarker);

			Marker employMarker = mMap
					.addMarker(new MarkerOptions()
							.position(new LatLng(lat, lng))
							.draggable(false)
							.title(titledata)
							.snippet(employInfo)
							.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

			employMarker.setVisible(false);
			unemployment.add(employMarker);
			
			Marker singletonMarker = mMap
					.addMarker(new MarkerOptions()
							.position(new LatLng(lat, lng))
							.draggable(false)
							.title(titledata)
							.snippet(singleData)
							.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));

			singletonMarker.setVisible(false);
			singletons.add(singletonMarker);

		}

	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {

		try {
			setLayer((String) parent.getItemAtPosition(position));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setLayer(String option_selected) throws IOException {

		if (option_selected.equals(getString(R.string.option_1_cso))) {

		} else if (option_selected.equals(getString(R.string.option_2_cso))) {
			for (Marker m : popByNat) {
				m.setVisible(true);
			}
			for (Marker m : unemployment) {
				m.setVisible(false);
			}
			for (Marker m : singletons){
				m.setVisible(false);
			}
			locateMap(IrelandCenter);
			Toast.makeText(this, "Loading population data", Toast.LENGTH_SHORT)
					.show();

		} else if (option_selected.equals(getString(R.string.option_3_cso))) {
			for (Marker m : popByNat) {
				m.setVisible(false);
			}
			for (Marker m : unemployment) {
				m.setVisible(true);
			}
			for (Marker m : singletons){
				m.setVisible(false);
			}
			locateMap(IrelandCenter);
			Toast.makeText(this, "Loading Employment data", Toast.LENGTH_SHORT)
					.show();
		} else if (option_selected.equals(getString(R.string.option_4_cso))) {
			for (Marker m : popByNat) {
				m.setVisible(false);
			}
			for (Marker m : unemployment) {
				m.setVisible(false);
			}
			for (Marker m : singletons){
				m.setVisible(true);
			}

		} else if (option_selected.equals(getString(R.string.option_5_cso))) {
			Toast.makeText(this, "CSO Option 4 Selected", Toast.LENGTH_SHORT)
					.show();

		} else if (option_selected.equals(getString(R.string.option_1_twi))) {
			
		} else if (option_selected.equals(getString(R.string.option_2_twi))) {
			Toast.makeText(this, "Loading twitter stream",
					Toast.LENGTH_SHORT).show();
			locationHandler.sendEmptyMessage(SUBJECT); //calls the recursive twitter method
			locateMap(IrelandCenter);
			
			for (Marker m : popByNat) {
				m.setVisible(false);
			}
			for (Marker m : unemployment) {
				m.setVisible(false);
			}
			for (Marker m : singletons){
				m.setVisible(false);
			}
			
		} else if (option_selected.equals(getString(R.string.option_3_twi))) {
			Toast.makeText(this, "Stopping twitter stream",
					Toast.LENGTH_SHORT).show();
			
			locationHandler.removeMessages(SUBJECT);
		} else {
		//	Log.i("LDA", "Error loading data " + option_selected);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// Do nothing.
	}

	class CustomInfoWindowAdapter implements InfoWindowAdapter {
		private final RadioGroup mOptions;

		// These a both viewgroups containing an ImageView with id "badge" and
		// two TextViews with id
		// "title" and "snippet".
		private final View mWindow;
		private final View mContents;

		CustomInfoWindowAdapter() {
			mWindow = getLayoutInflater().inflate(R.layout.custom_info_window,
					null);
			mContents = getLayoutInflater().inflate(
					R.layout.custom_info_contents, null);
			mOptions = null;
		}

		@Override
		public View getInfoWindow(Marker marker) {

			render(marker, mWindow);
			return mWindow;
		}

		@Override
		public View getInfoContents(Marker marker) {
			return mContents;
		}

		private void render(Marker marker, View view) {
			int badge = R.drawable.ireland_flag;
			// Use the equals() method on a Marker to check for equals. Do not
			// use ==.
			if (marker.getTitle().equals("Carlow")) {
				badge = R.drawable.carlow_flag;
			} else if (marker.getTitle().equals("Dublin City")) {
				badge = R.drawable.dublin_flag;
			} else if (marker.getTitle().equals("South Dublin")) {
				badge = R.drawable.dublin_flag;
			} else if (marker.getTitle().equals("Fingal")) {
				badge = R.drawable.dublin_flag;
			} else if (marker.getTitle().equals("Dun Laoghaire-Rathdown")) {
				badge = R.drawable.dublin_flag;
			} else if (marker.getTitle().equals("Kildare")) {
				badge = R.drawable.kildare_flag;
			} else if (marker.getTitle().equals("Kilkenny")) {
				badge = R.drawable.kilkenny_flag;
			} else if (marker.getTitle().equals("Laois")) {
				badge = R.drawable.laois_flag;
			} else if (marker.getTitle().equals("Longford")) {
				badge = R.drawable.longford_flag;
			} else if (marker.getTitle().equals("Louth")) {
				badge = R.drawable.louth_flag;
			} else if (marker.getTitle().equals("Meath")) {
				badge = R.drawable.meath_flag;
			} else if (marker.getTitle().equals("Offaly")) {
				badge = R.drawable.offaly_flag;
			} else if (marker.getTitle().equals("Westmeath")) {
				badge = R.drawable.westmeath_flag;
			} else if (marker.getTitle().equals("Wexford")) {
				badge = R.drawable.wexford_flag;
			} else if (marker.getTitle().equals("Wicklow")) {
				badge = R.drawable.wicklow_flag;
			} else if (marker.getTitle().equals("Clare")) {
				badge = R.drawable.clare_flag;
			} else if (marker.getTitle().equals("Cork City")) {
				badge = R.drawable.cork_flag;
			} else if (marker.getTitle().equals("Cork")) {
				badge = R.drawable.cork_flag;
			} else if (marker.getTitle().equals("Kerry")) {
				badge = R.drawable.kerry_flag;
			} else if (marker.getTitle().equals("Limerick City")) {
				badge = R.drawable.limerick_flag;
			} else if (marker.getTitle().equals("Limerick")) {
				badge = R.drawable.limerick_flag;
			} else if (marker.getTitle().equals("Tipperary North")) {
				badge = R.drawable.tipp_flag;
			} else if (marker.getTitle().equals("Tipperary South")) {
				badge = R.drawable.tipp_flag;
			} else if (marker.getTitle().equals("Waterford City")) {
				badge = R.drawable.waterford_flag;
			} else if (marker.getTitle().equals("Waterford")) {
				badge = R.drawable.waterford_flag;
			} else if (marker.getTitle().equals("Galway City")) {
				badge = R.drawable.galway_flag;
			} else if (marker.getTitle().equals("Galway")) {
				badge = R.drawable.galway_flag;
			} else if (marker.getTitle().equals("Leitrim")) {
				badge = R.drawable.leitrim_flag;
			} else if (marker.getTitle().equals("Mayo")) {
				badge = R.drawable.mayo_flag;
			} else if (marker.getTitle().equals("Roscommon")) {
				badge = R.drawable.roscommon_flag;
			} else if (marker.getTitle().equals("Sligo")) {
				badge = R.drawable.sligo_flag;
			} else if (marker.getTitle().equals("Cavan")) {
				badge = R.drawable.cavan_flag;
			} else if (marker.getTitle().equals("Donegal")) {
				badge = R.drawable.donegal_flag;
			} else if (marker.getTitle().equals("Monaghan")) {
				badge = R.drawable.monaghan_flag;

			} else {
				// Passing 0 to setImageResource will clear the image view.
				badge = 0;
			}
			((ImageView) view.findViewById(R.id.badge)).setImageResource(badge);

			String title = marker.getTitle();
			TextView titleUi = ((TextView) view.findViewById(R.id.title));
			if (title != null) {
				// Spannable string allows us to edit the formatting of the
				// text.
				SpannableString titleText = new SpannableString(title);
				titleText.setSpan(new ForegroundColorSpan(Color.RED), 0,
						titleText.length(), 0);
				titleUi.setText(titleText);
			} else {
				titleUi.setText("");
			}

			String snippet = marker.getSnippet();
			TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
			if (snippet != null && snippet.length() > 12) {
				SpannableString snippetText = new SpannableString(snippet);
				// snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), 0,
				// 10, 0);
				// snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), 12,
				// snippet.length(), 0);
				snippetUi.setText(snippetText);
			} else {
				snippetUi.setText("");
			}
		}
	}
	private void plotTwitterMarker(){
		Double lat;
	    Double lng;
	    String title = null;
	    String snippet = null;
	    LatLng location = null;
	    String iconUrl = null;
	    String urlScrape = null;
	    
		ConnectivityManager connMgr = (ConnectivityManager) 
		        getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		
		    if (networkInfo != null && networkInfo.isConnected()) {
		    	
		    	
				try {
					urlScrape = new DownloadWebpageTask().execute(stringUrl).get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				
				if (lastTweet.equals(urlScrape)){
					
				}
				else{
					try{
				lastTweet = urlScrape;
				twiterLoopCount++;
		   // 	Toast.makeText(this, "Method call: "+ twiterLoopCount ,	Toast.LENGTH_SHORT).show();		    	
		    	try {
	    			JSONObject jsonObj = new JSONObject(urlScrape);	
	    			lat = jsonObj.getDouble("lat");
	    			lng = jsonObj.getDouble("lng");
	    			title = jsonObj.getString("user");
	    			snippet = jsonObj.getString("text"); 
	    			location = new LatLng(lat,lng);
	    			iconUrl = jsonObj.getString("profileImageUrl");
	    			//tweetSpots.add(location);
	    			
	        } catch (JSONException e) {
	            e.printStackTrace();

			}
		    
		    //masterImg = getBitmapFromURL(iconUrl);

		 	Marker tweetMarker = mMap
					.addMarker(new MarkerOptions()
							.position(location)
							.draggable(false)
							.title(title)
							.snippet(snippet)
							//.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromURL(iconUrl))));
							.icon(BitmapDescriptorFactory.fromResource(R.drawable.twitter_icon)));

			tweetMarker.setVisible(true); 
			
				}
				 catch (Exception e) {
					 e.printStackTrace();
				//	Toast.makeText(this, "Bad Data", Toast.LENGTH_SHORT).show();
					 badDataCount ++;
				 }
		    }
		    	
		    	
		   
		    } else {
		    	Toast.makeText(this, "Network connection unavailable" ,
						Toast.LENGTH_LONG).show();
		        // display error
		    }

    	
	}
	public static Bitmap getBitmapFromURL(String src) {
	    try {
	        URL url = new URL(src);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setDoInput(true);
	        connection.connect();
	        InputStream input = connection.getInputStream();
	        Bitmap myBitmap = BitmapFactory.decodeStream(input);

	        
	        return myBitmap;
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
	}
}
