package com.gmail.tylercap4.connections;

import java.util.LinkedList;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TableRow;

public class Connections extends Activity implements ConnectionCallbacks, OnConnectionFailedListener
{
	private static final int ROWS = 9;
	private static final int COLUMNS = 8;
	private static final int PLAYER_CARDS = 6;
	
	private int [][] widget_ids;
	private int [] player_card_ids;
	
	private int [][] 			gameboard;
	private IndexedButton [][] 	widgets;
	
	/* Client used to interact with Google APIs. */
	private GoogleApiClient mGoogleApiClient;
    private AdView adMobView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connections);
		
		// Load the banner ad
        adMobView = (AdView) findViewById(R.id.adView);
        adMobView.loadAd(new AdRequest.Builder().build());
        
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
		adMobView.resume();

//		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//			findViewById(R.id.sign_in_button).setVisibility(View.GONE);
//
//			final View signOutLayout = findViewById(R.id.sign_out_layout);
//			final TextView user_view = (TextView) findViewById(R.id.current_user);
//
//			// show email for user signed in
//			String username = Plus.AccountApi.getAccountName(mGoogleApiClient);
//			if( username != null )
//				user_view.setText(username);
//
//			signOutLayout.setVisibility(View.VISIBLE);
//			findViewById(R.id.leaderboard_button).setVisibility(View.VISIBLE);
//		}
//		else{
//			findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
//			findViewById(R.id.sign_out_layout).setVisibility(View.GONE);
//			findViewById(R.id.leaderboard_button).setVisibility(View.GONE);
//		}

		if( gameboard == null ){
			widget_ids = new int[9][8];
			player_card_ids = new int[6];
			
			gameboard = new int[9][8];
			widgets = new IndexedButton[9][8];
			
			initWidgetIds();
			doNewGame();
		}
	}

    private void setUpButton( Button button, int value ){
    	button.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT, 1.0f));
		button.setBackgroundResource(R.drawable.custom_button);

		//TODO: increase font size
		
		button.setText(getEmoji(value));
    }

    
	private void doNewGame(){
    	synchronized( this ){
	    	LinkedList<Integer> list = new LinkedList<Integer>();
	    	
	    	for( int val = 0; val <= 36; val++ ){
		    	for( int i = 0; i < 2; i++ ){
		    		list.add(val);
		    	}
	    	}
	    	
	    	// now fill the table from the list
	    	int remaining = 72;
	    	for(int row = 0; row < ROWS; row++ ){    		
	    		for( int column = 0; column < COLUMNS; column++ ){
	    			int index = (int) (Math.random() * remaining);
	    			int val = list.remove(index);
	    			remaining--;
	    			
	    			gameboard[row][column] = val;
	    			
	    			FrameLayout view = (FrameLayout)findViewById(widget_ids[row][column]);
	    			IndexedButton button = new IndexedButton(this, row, column);
	    			setUpButton(button, val);
	            	
	    			if( view.getChildCount() > 0 )
	    				view.removeAllViews();
	    			
	            	view.addView(button);
	            	widgets[row][column] = button;
	    		}
	    	}
	    	
	    	for( int i = 0; i < PLAYER_CARDS; i++ ){    			
    			FrameLayout view = (FrameLayout)findViewById(player_card_ids[i]);
    			
    			Button child = new Button(this);
            	setUpButton( child, -2 + (i % 2) );
    			if( view.getChildCount() > 0 )
    				view.removeAllViews();
    			
            	view.addView(child);
	    	}
    	}
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.connections, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
    public void onConnected(Bundle connectionHint) {
//    	mSignInClicked = false;
//	      
//    	findViewById(R.id.sign_in_button).setVisibility(View.GONE);
//	    
//	    final View signOutLayout = findViewById(R.id.sign_out_layout);
//	    final TextView user_view = (TextView) findViewById(R.id.current_user);
//	    
//	    // show email for user signed in
//	    String username = Plus.AccountApi.getAccountName(mGoogleApiClient);
//	    if( username != null )
//	    	user_view.setText(username);
//	    
//	    signOutLayout.setVisibility(View.VISIBLE);
//	    findViewById(R.id.leaderboard_button).setVisibility(View.VISIBLE);
    }
    
    public void onConnectionSuspended(int cause) {
    	mGoogleApiClient.connect();
    }
    
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
//        if (mResolvingConnectionFailure) {
//            // Already resolving
//            return;
//        }
//
//        // If the sign in button was clicked or if auto sign-in is enabled,
//        // launch the sign-in flow
//        if (mSignInClicked) {
//            mSignInClicked = false;
//            mResolvingConnectionFailure = true;
//
//            // Attempt to resolve the connection failure using BaseGameUtils.
//            // The R.string.signin_other_error value should reference a generic
//            // error string in your strings.xml file, such as "There was
//            // an issue with sign in, please try again later."
//            if (!BaseGameUtils.resolveConnectionFailure(this,
//                    mGoogleApiClient, connectionResult,
//                    RC_SIGN_IN, getString(R.string.sign_in_failed))) 
//            {
//                mResolvingConnectionFailure = false;
//            }
//        }
    }
    


	private String getEmoji(int value){
		int unicode;
		
		switch( value ){
			case -1:
				unicode = 0x26A1; // lightning
				break;
			case -2:
				unicode = 0x1F340; // four leaf clover
				break;
			case 1:
				unicode = 0x1F3C8;
				break;
			case 2:
				unicode = 0x1F4A9;
				break;
			case 3:
				unicode = 0x1F431;
				break;
			case 4:
				unicode = 0x1F680;
				break;
			case 5:
				unicode = 0x1F3C0;
				break;
			case 6:
				unicode = 0x1F64A;
				break;
			case 7:
				unicode = 0x1F3AF;
				break;
			case 8:
				unicode = 0x1F385;
				break;
			case 9:
				unicode = 0x0231B;
				break;
			case 10:
				unicode = 0x023F0;
				break;
			case 11:
				unicode = 0x02600;
				break;
			case 12:
				unicode = 0x02614;
				break;
			case 13:
				unicode = 0x02615;
				break;
			case 14:
				unicode = 0x1F364;
				break;
			case 15:
				unicode = 0x026BD;
				break;
			case 16:
				unicode = 0x1F383;
				break;
			case 17:
				unicode = 0x026F3;
				break;
			case 18:
				unicode = 0x1F37B;
				break;
			case 19:
				unicode = 0x1F36A;
				break;
			case 20:
				unicode = 0x1F335;
				break;
			case 21:
				unicode = 0x1F339;
				break;
			case 22:
				unicode = 0x1F33B;
				break;
			case 23:
				unicode = 0x1F33D;
				break;
			case 24:
				unicode = 0x1F369;
				break;
			case 25:
				unicode = 0x1F344;
				break;
			case 26:
				unicode = 0x1F346;
				break;
			case 27:
				unicode = 0x1F347;
				break;
			case 28:
				unicode = 0x1F349;
				break;
			case 29:
				unicode = 0x1F34A;
				break;
			case 30:
				unicode = 0x1F34D;
				break;
			case 31:
				unicode = 0x1F352;
				break;
			case 32:
				unicode = 0x1F354;
				break;
			case 33:
				unicode = 0x1F355;
				break;
			case 34:
				unicode = 0x1F357;
				break;
			case 35:
				unicode = 0x1F35F;
				break;
			default:
				unicode = 0x1F603;
		}
		
	    return new String(Character.toChars(unicode));
	}
    
    private void initWidgetIds(){
    	player_card_ids [0] = R.id.playerCard0;
    	player_card_ids [1] = R.id.playerCard1;
    	player_card_ids [2] = R.id.playerCard2;
    	player_card_ids [3] = R.id.playerCard3;
    	player_card_ids [4] = R.id.playerCard4;
    	player_card_ids [5] = R.id.playerCard5;
    	
    	widget_ids[0][0] = R.id.widgetr0c0;
    	widget_ids[0][1] = R.id.widgetr0c1;
    	widget_ids[0][2] = R.id.widgetr0c2;
    	widget_ids[0][3] = R.id.widgetr0c3;
    	widget_ids[0][4] = R.id.widgetr0c4;
    	widget_ids[0][5] = R.id.widgetr0c5;
    	widget_ids[0][6] = R.id.widgetr0c6;
    	widget_ids[0][7] = R.id.widgetr0c7;

    	widget_ids[1][0] = R.id.widgetr1c0;
    	widget_ids[1][1] = R.id.widgetr1c1;
    	widget_ids[1][2] = R.id.widgetr1c2;
    	widget_ids[1][3] = R.id.widgetr1c3;
    	widget_ids[1][4] = R.id.widgetr1c4;
    	widget_ids[1][5] = R.id.widgetr1c5;
    	widget_ids[1][6] = R.id.widgetr1c6;
    	widget_ids[1][7] = R.id.widgetr1c7;
    	
    	widget_ids[2][0] = R.id.widgetr2c0;
    	widget_ids[2][1] = R.id.widgetr2c1;
    	widget_ids[2][2] = R.id.widgetr2c2;
    	widget_ids[2][3] = R.id.widgetr2c3;
    	widget_ids[2][4] = R.id.widgetr2c4;
    	widget_ids[2][5] = R.id.widgetr2c5;
    	widget_ids[2][6] = R.id.widgetr2c6;
    	widget_ids[2][7] = R.id.widgetr2c7;

    	widget_ids[3][0] = R.id.widgetr3c0;
    	widget_ids[3][1] = R.id.widgetr3c1;
    	widget_ids[3][2] = R.id.widgetr3c2;
    	widget_ids[3][3] = R.id.widgetr3c3;
    	widget_ids[3][4] = R.id.widgetr3c4;
    	widget_ids[3][5] = R.id.widgetr3c5;
    	widget_ids[3][6] = R.id.widgetr3c6;
    	widget_ids[3][7] = R.id.widgetr3c7;
    	
    	widget_ids[4][0] = R.id.widgetr4c0;
    	widget_ids[4][1] = R.id.widgetr4c1;
    	widget_ids[4][2] = R.id.widgetr4c2;
    	widget_ids[4][3] = R.id.widgetr4c3;
    	widget_ids[4][4] = R.id.widgetr4c4;
    	widget_ids[4][5] = R.id.widgetr4c5;
    	widget_ids[4][6] = R.id.widgetr4c6;
    	widget_ids[4][7] = R.id.widgetr4c7;

    	widget_ids[5][0] = R.id.widgetr5c0;
    	widget_ids[5][1] = R.id.widgetr5c1;
    	widget_ids[5][2] = R.id.widgetr5c2;
    	widget_ids[5][3] = R.id.widgetr5c3;
    	widget_ids[5][4] = R.id.widgetr5c4;
    	widget_ids[5][5] = R.id.widgetr5c5;
    	widget_ids[5][6] = R.id.widgetr5c6;
    	widget_ids[5][7] = R.id.widgetr5c7;
    	
    	widget_ids[6][0] = R.id.widgetr6c0;
    	widget_ids[6][1] = R.id.widgetr6c1;
    	widget_ids[6][2] = R.id.widgetr6c2;
    	widget_ids[6][3] = R.id.widgetr6c3;
    	widget_ids[6][4] = R.id.widgetr6c4;
    	widget_ids[6][5] = R.id.widgetr6c5;
    	widget_ids[6][6] = R.id.widgetr6c6;
    	widget_ids[6][7] = R.id.widgetr6c7;
    	
    	widget_ids[7][0] = R.id.widgetr7c0;
    	widget_ids[7][1] = R.id.widgetr7c1;
    	widget_ids[7][2] = R.id.widgetr7c2;
    	widget_ids[7][3] = R.id.widgetr7c3;
    	widget_ids[7][4] = R.id.widgetr7c4;
    	widget_ids[7][5] = R.id.widgetr7c5;
    	widget_ids[7][6] = R.id.widgetr7c6;
    	widget_ids[7][7] = R.id.widgetr7c7;
    	
    	widget_ids[8][0] = R.id.widgetr8c0;
    	widget_ids[8][1] = R.id.widgetr8c1;
    	widget_ids[8][2] = R.id.widgetr8c2;
    	widget_ids[8][3] = R.id.widgetr8c3;
    	widget_ids[8][4] = R.id.widgetr8c4;
    	widget_ids[8][5] = R.id.widgetr8c5;
    	widget_ids[8][6] = R.id.widgetr8c6;
    	widget_ids[8][7] = R.id.widgetr8c7;
    }
}
