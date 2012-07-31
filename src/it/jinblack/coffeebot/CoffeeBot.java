package it.jinblack.coffeebot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.text.AttributedString;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.GridLayout;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class CoffeeBot extends Activity implements OnSeekBarChangeListener {
	public static Integer TOKEN_REQUEST = 1;
	Context mContext;
	Twitter mTwitter;
	Intent auth;
	AccessToken aToken;
	SeekBar bar;
	TextView toDrink;
	GridLayout coffeegrid;
	
	private class TweetAsync extends AsyncTask<String, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(String... params) {
			System.out.println(aToken);
			if (mTwitter == null){
				mTwitter = getTwitter();
			}
			try {
				mTwitter.updateStatus(params[0]);
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	}

	private class UpdateBalance extends AsyncTask<String, Integer, String>{

		@Override
		protected String doInBackground(String... params) {
			Pattern pattern = Pattern.compile(mContext.getString(R.string.updateregexp));
			System.out.println(aToken);
			if (mTwitter == null){
				mTwitter = getTwitter();
			}
			try {
				Iterator<DirectMessage> msg = mTwitter.getDirectMessages().iterator();
				while (msg.hasNext()){
					DirectMessage dmsg = msg.next();
					if (dmsg.getSenderScreenName().equals(mContext.getString(R.string.BotAccount))){
						System.out.println(dmsg.getText());
						Matcher matcher = pattern.matcher(dmsg.getText());
						if (matcher.find()){
							return matcher.group(1);
						}
					}
				}
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		protected void onPostExecute(String result) {
			TextView counter = (TextView) findViewById(R.id.coffeCount);
			counter.setText(result);
		}

	}

	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_coffee_bot);
		mContext = getApplicationContext();
		bar = (SeekBar) findViewById(R.id.seekBar);
		toDrink = (TextView) findViewById(R.id.countCoffeToDrink);
		toDrink.setText(String.valueOf(1));
		bar.setOnSeekBarChangeListener(this);
		coffeegrid = (GridLayout) findViewById(R.id.coffeegrid);
		auth = new Intent(this,Authentication.class);
		initAccessToken();
	}

	@Override
	public void onResume(){
		super.onResume();
		updateBalance(null);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_coffee_bot, menu);
		return true;
	}
	
	private Twitter getTwitter() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthConsumerKey(mContext.getString(R.string.twitter_oauth_key));
		cb.setOAuthConsumerSecret(mContext.getString(R.string.twitter_oauth_secret));
		return new TwitterFactory(cb.build()).getInstance(aToken);
	}

	public void tweet(View view){
		if (aToken == null){
			initAccessToken();
		}
		else{
			new TweetAsync().execute("");
		}
	}
	public void drinkCoffees(View view){
		if (aToken == null){
			initAccessToken();
		}
		else{
			String numCof = toDrink.getText().toString();
			new TweetAsync().execute("@"+mContext.getString(R.string.BotAccount)+" "+numCof+" #coffee plz!");
			updateBalance(view);
		}
		
	}

	private void initAccessToken(){
		FileInputStream file;

		try {
			file = openFileInput("token");
			ObjectInputStream objin = new ObjectInputStream(file);
			aToken = (AccessToken) objin.readObject();

		} catch (FileNotFoundException e) {
			System.out.println("filenotfound");
			startActivityForResult(auth, TOKEN_REQUEST);
		} catch (StreamCorruptedException e) {
			System.out.println("corruptedstream");
			startActivityForResult(auth, TOKEN_REQUEST);
		} catch (IOException e) {
			System.out.println("IOexception");
			startActivityForResult(auth, TOKEN_REQUEST);
		} catch (ClassNotFoundException e) {
			System.out.println("classnotfound");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void updateBalance(View view){
		if (aToken == null){
			initAccessToken();
		}
		else{
			new UpdateBalance().execute("");
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == TOKEN_REQUEST) {
			// Make sure the request was successful
			if (resultCode == RESULT_OK) {
				try {
					FileInputStream file = openFileInput("token");
					ObjectInputStream objin = new ObjectInputStream(file);
					aToken = (AccessToken) objin.readObject();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (StreamCorruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		
		toDrink.setText(String.valueOf(progress));
		coffeegrid.removeAllViews();
		for (int i=0; i< progress;i++){
			ImageView coffee = new ImageView(CoffeeBot.this);
			coffee.setImageResource(R.drawable.caffe100);
			coffeegrid.addView(coffee);
		}
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
}
