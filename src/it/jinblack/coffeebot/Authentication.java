package it.jinblack.coffeebot;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

public class Authentication extends Activity {
	Twitter twitter;
	RequestToken request;
	WebView webview;
	private class TweetAsync extends AsyncTask<String, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(String... params) {
			if (twitter == null){
				twitter = getTwitter();
			}
			try {
				request = twitter.getOAuthRequestToken();
				webview.loadUrl(request.getAuthorizationURL());
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	}
	private class SendPin extends AsyncTask<String, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(String... params) {
			AccessToken token = null;
			try {
				token = twitter.getOAuthAccessToken(request, params[0]);
			} catch (TwitterException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				if (token != null){
					FileOutputStream file = openFileOutput("token", MODE_PRIVATE);
					ObjectOutputStream objstrm = new ObjectOutputStream(file);
					objstrm.writeObject(token);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authentication);
		webview = (WebView)findViewById(R.id.webView);
		webview.setWebViewClient(new PinWebViewClient());
		new TweetAsync().execute("");

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_authentication, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private Twitter getTwitter(){
		Context mContext = getApplicationContext();
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthConsumerKey(mContext.getString(R.string.twitter_oauth_key));
		cb.setOAuthConsumerSecret(mContext.getString(R.string.twitter_oauth_secret));
		Twitter twitter = new TwitterFactory(cb.build()).getInstance();
		return twitter;

	}
	public void sendPin(View view){
		EditText pinText = (EditText)findViewById(R.id.insertPin);
		String pin = pinText.getText().toString().trim();
		if (pin != ""){
			new SendPin().execute(pin);
			setResult(RESULT_OK);
			finish();
		}
	}

	private class PinWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			/*  if (Uri.parse(url).getHost().equals("www.example.com")) {
                // This is my web site, so do not override; let my WebView load the page
                return false;
            }
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);*/
			return false;
		}
	}
}
