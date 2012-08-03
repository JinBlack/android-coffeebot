package it.jinblack.coffeebot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class CoffeeBot extends Activity implements OnSeekBarChangeListener, OnFocusChangeListener {
	public static Integer TOKEN_REQUEST = 1;
	public static Float COFFEE_PRICE = (float) 0.20;
	private static long POLLING_TIME = 5000;
	Context mContext;
	Twitter mTwitter;
	Intent auth;
	AccessToken aToken;
	SeekBar bar;
	TextView toDrink;
	GridLayout coffeegrid;
	EditText money,numcoffes;
	TimerTask updateTask;
	final Handler handler = new Handler();
	Timer t ;

	int coffeeleft;

	private void updateBalacePoll(){
		t = new Timer();
		updateTask = new TimerTask() {
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						updateBalance(null); 
					}
				});
			}};


			t.schedule(updateTask, POLLING_TIME, POLLING_TIME); 

	}
	private void stopBalacePoll(){
		if(t!=null){
			t.cancel();
		}
	}
	private class TweetAsync extends AsyncTask<String, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(String... params) {
			if (mTwitter == null){
				mTwitter = getTwitter();
			}
			try {
				mTwitter.updateStatus("@"+mContext.getString(R.string.BotAccount)+" "+params[0]);
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		protected void onPostExecute(Boolean a){
			toastMsg("Done!");
			updateBalance(null);
		}

	}

	private class UpdateBalance extends AsyncTask<String, Integer, String>{

		@Override
		protected String doInBackground(String... params) {
			Pattern pattern = Pattern.compile(mContext.getString(R.string.updateregexp));
			if (mTwitter == null){
				mTwitter = getTwitter();
			}
			try {
				Iterator<DirectMessage> msg = mTwitter.getDirectMessages().iterator();
				while (msg.hasNext()){
					DirectMessage dmsg = msg.next();
					if (dmsg.getSenderScreenName().equals(mContext.getString(R.string.BotAccount))){
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
			if (result != null && Integer.valueOf(result) != coffeeleft){
				coffeeleft = Integer.valueOf(result);
				TextView counter = (TextView) findViewById(R.id.coffeCount);
				counter.setText(result);
				toastMsg(mContext.getString(R.string.countcoffe)+result);
			}
		}

	}

	private void toastMsg(String msg){
		TextView view =new TextView(mContext);
		view.setTextSize(40);
		view.setBackgroundColor(Color.BLACK);
		view.setText(msg);
		Toast toast =new Toast(mContext);
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.setView(view);
		toast.show();
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
		money = (EditText) findViewById(R.id.moneyInput);
		numcoffes = (EditText) findViewById(R.id.numInput);
		numcoffes.setOnFocusChangeListener(this);
		money.setOnFocusChangeListener(this);
		initAccessToken();

	}

	@Override
	public void onResume(){
		super.onResume();
		updateBalance(null);
		updateBalacePoll();
	}
	@Override
	public void onPause(){
		super.onPause();
		stopBalacePoll();
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


	public void drinkCoffee(View view){
		if (aToken == null){
			initAccessToken();
		}
		else{
			String numCof = toDrink.getText().toString();
			String question = String.format(mContext.getString(R.string.drink_question),numCof);
			String tweet = String.format(mContext.getString(R.string.drink_tweet),numCof);
			tweetWithConfirm(question,tweet);
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


	public void topUp(View view){
		if (aToken == null){
			initAccessToken();
		}
		else{
			String numCof = numcoffes.getText().toString();
			String question = String.format(mContext.getString(R.string.topup_question),numCof);
			String tweet = String.format(mContext.getString(R.string.topup_tweet),numCof);
			tweetWithConfirm(question,tweet);

		}

	}

	private void updateNum(){
		int coffee;
		String mon = money.getText().toString();
		if (mon.equals("") || mon.equals(" "))
			mon = "0";
		coffee = (int) (Float.parseFloat(mon)/COFFEE_PRICE);
		numcoffes.setText(String.valueOf(coffee));
	}

	private void updateMoney(){
		float mon;
		mon = Float.parseFloat(numcoffes.getText().toString())*COFFEE_PRICE;
		money.setText(String.valueOf(mon));
	}

	public void onFocusChange(View v, boolean hasFocus) {
		if (!hasFocus){
			if (v.getId() == R.id.moneyInput)
				updateNum();
			if (v.getId() == R.id.numInput)
				updateMoney();			
		}
	}
	private void tweetWithConfirm (String question,final String tweetmsg){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(question)
		       .setCancelable(false)
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       })
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		   			new TweetAsync().execute(tweetmsg);		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}
}
