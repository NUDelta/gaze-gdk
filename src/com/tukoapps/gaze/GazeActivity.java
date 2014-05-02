package com.tukoapps.gaze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.android.glass.app.Card;
import com.google.android.glass.app.Card.ImageLayout;
import com.tukoapps.gaze.CardAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

public class GazeActivity extends Activity {
	
	private GestureDetector mGestureDetector = null;
	LocationManager locationManager; // initialized elsewhere
	private CardScrollView mCardScroller;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//mCardScroller = new CardScrollView(this);
        //mCardScroller.setAdapter(new CardAdapter(createCards(this)));
        //setContentView(mCardScroller);
		ArrayList<String> voiceResults = getIntent().getExtras()
		        .getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
		
		Intent service = new Intent(GazeActivity.this, GazeService.class);
		service.putExtra("voicetag", voiceResults.get(0));
		startService(service);
	}
}