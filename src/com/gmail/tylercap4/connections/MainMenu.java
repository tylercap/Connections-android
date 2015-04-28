package com.gmail.tylercap4.connections;

import java.util.LinkedList;
import java.util.List;

import com.gmail.tylercap4.connections.basegameutils.BaseGameUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainMenu extends Activity implements ConnectionCallbacks, OnConnectionFailedListener
{
	private static final String 	SIGNED_IN_KEY = "SIGNED_IN";
	private static int RC_SIGN_IN = 9001;
	
	private MenuItem sign_in;
	
	/* Client used to interact with Google APIs. */
	protected GoogleApiClient mGoogleApiClient;

	private boolean mResolvingConnectionFailure = false;
	private boolean mSignInClicked = false;
	
	private List<String> game_strings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
        
        mGoogleApiClient = new GoogleApiClient.Builder(this)
		        .addConnectionCallbacks(this)
		        .addOnConnectionFailedListener(this)
		        .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();	
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		updateSignInButton();
		
		updateGameList();
	}
	
	private void updateSignInButton(){
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			if( this.sign_in != null )
				this.sign_in.setTitle(R.string.sign_out);


			// show email for user signed in
			String username = Plus.AccountApi.getAccountName(mGoogleApiClient);
		}
		else{
			if( this.sign_in != null )
				this.sign_in.setTitle(R.string.sign_in);
		}
	}
	
	@Override
    protected void onPause(){
    	super.onPause();
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	SharedPreferences.Editor prefs_editor = prefs.edit();
    	
    	if (mGoogleApiClient.isConnected()) {
    		mGoogleApiClient.disconnect();
        	prefs_editor.putBoolean(SIGNED_IN_KEY, true);
    	}
    	else{
    		prefs_editor.putBoolean(SIGNED_IN_KEY, false);
    	}
    	prefs_editor.commit();
    }
    
    @Override
    protected void onStart(){
    	super.onStart();
    	
    	reloadSignIn();
    }
    
    private void reloadSignIn(){
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean signed_in = prefs.getBoolean(SIGNED_IN_KEY, false);
    	if( signed_in ){
    		mGoogleApiClient.connect();
    	}
    }

	@Override
    public void onConnected(Bundle connectionHint) {
    	mSignInClicked = false;
	      
    	updateSignInButton();
    }
    
    public void onConnectionSuspended(int cause) {
    	mGoogleApiClient.connect();
    }
    
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            // Already resolving
            return;
        }

        // If the sign in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked) {
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            // Attempt to resolve the connection failure using BaseGameUtils.
            // The R.string.signin_other_error value should reference a generic
            // error string in your strings.xml file, such as "There was
            // an issue with sign in, please try again later."
            if (!BaseGameUtils.resolveConnectionFailure(this,
                    mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.sign_in_failed))) 
            {
                mResolvingConnectionFailure = false;
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                // Bring up an error dialog to alert the user that sign-in
                // failed. The R.string.signin_failure should reference an error
                // string in your strings.xml file that tells the user they
                // could not be signed in, such as "Unable to sign in."
                BaseGameUtils.showActivityResultError(this,
                    requestCode, resultCode, R.string.sign_in_failed);
            }
        }
    }
	
	private void updateGameList(){
		
		ListView games_list = (ListView) findViewById(R.id.games_list);

		game_strings = new LinkedList<String>();
		game_strings.add("test 1");
		game_strings.add("test 2");
		
        // Set the adapter for the list view
        games_list.setAdapter (new ArrayAdapter<String>(this, R.layout.game_list_item,
                R.id.game_label, game_strings) );

        games_list.setOnItemClickListener(new GameClickListener());
	}
	
	private class GameClickListener implements OnItemClickListener
    {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);			
		}
    }
	
	private void selectItem(int position){
		String game = game_strings.get(position);
		
		Intent i = new Intent( this, Connections.class );
		startActivity(i);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		
		sign_in = menu.findItem(R.id.action_sign_in);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_sign_in) {
			if(mGoogleApiClient.isConnected()){
            		mSignInClicked = false;
                    Games.signOut(mGoogleApiClient);
                    mGoogleApiClient.clearDefaultAccountAndReconnect();
                    
                    item.setTitle(R.string.sign_in);
			}
			else if (!mGoogleApiClient.isConnecting()) {
            		mSignInClicked = true;
                    mGoogleApiClient.connect();
			}
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
