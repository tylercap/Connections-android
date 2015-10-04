package com.gmail.tylercap4.connections;

import java.util.LinkedList;

import com.flurry.android.*;
import com.flurry.android.ads.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.InitiateMatchResult;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.LoadMatchResult;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.UpdateMatchResult;
import com.google.android.gms.plus.Plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Connections extends Activity implements ConnectionCallbacks, OnConnectionFailedListener
{		
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String MATCH_ID_TAG = "MatchID";
    private static final String FLURRY_API_KEY = "8Q8WG6WNS4DYWPSWW7T4";
    
    private FlurryAdInterstitial mFlurryAdInterstitial = null;
    private String intAdName = "CONNECT_ANDROID_INTERSTITIAL";
    
    private RelativeLayout mBanner;
    private FlurryAdBanner mFlurryAdBanner = null;
    private String bannerAdName = "CONNECT_ANDROID_BANNER";
    
	/* Client used to interact with Google APIs. */
	protected GoogleApiClient mGoogleApiClient;
    
	private Model model;
	private boolean my_turn;
	private int owner;
	
	private int [][] widget_ids;
	private int [] player_card_ids;
	
	private IndexedButton [][] 	widgets;
	private IndexedButton [] 	player_cards;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connections);
		
		// configure Flurry
        FlurryAgent.setLogEnabled(false);

        // init Flurry
        FlurryAgent.init(this, FLURRY_API_KEY);
		
		// Load the banner ad
//        adMobView = (AdView) findViewById(R.id.adView);
//        adMobView.loadAd(new AdRequest.Builder().build());
	}

	@Override
    protected void onDestroy() {
		super.onDestroy();
        mFlurryAdBanner.destroy();
        mFlurryAdInterstitial.destroy();
    }
	
	@Override
	protected void onStart(){
		super.onStart();
		FlurryAgent.onStartSession(this);
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		FlurryAgent.onEndSession(this);
	}
    
    @Override
    protected void onPause(){
    	super.onPause();
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        try{
	        editor.putString(MATCH_ID_TAG, this.model.getMatchId());
	        editor.commit();
        }
        catch(Exception ex){
        	
        }
    }
	
	@Override
	protected void onResume(){
		super.onResume();
		mBanner = (RelativeLayout)findViewById(R.id.banner);
        mFlurryAdBanner = new FlurryAdBanner(this, mBanner, bannerAdName);
 
        // optional allow us to get callbacks for ad events, 
        mFlurryAdBanner.setListener(bannerAdListener);
        
        mFlurryAdInterstitial = new FlurryAdInterstitial(this, intAdName);

        // allow us to get callbacks for ad events
        mFlurryAdInterstitial.setListener(interstitialAdListener);

        mFlurryAdBanner.fetchAndDisplayAd();
        
        mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
        .addApi(Games.API).addScope(Games.SCOPE_GAMES)
        .build();	
        mGoogleApiClient.connect();
        
		addHowToText();

		if( widgets == null ){
			widget_ids = new int[Model.ROWS][Model.COLUMNS];
			player_card_ids = new int[Model.PLAYER_CARDS];
			
			widgets = new IndexedButton[Model.ROWS][Model.COLUMNS];
			player_cards = new IndexedButton[Model.PLAYER_CARDS];
			
			initWidgetIds();
		}
		
		loadModel();

    	setUpResignButton();
	}
	
	private void setUpResignButton(){
		Button resign_button = (Button)findViewById(R.id.resignButton);
    	if( resign_button != null ){
    		// add resign action
    		resign_button.setEnabled(this.my_turn);
    		
    		if(this.my_turn){
    			resign_button.setBackgroundResource(R.drawable.resign_button);
    		}
    		else{
    			resign_button.setBackgroundResource(R.drawable.resign_disabled);
    		}
    		
    		resign_button.setOnClickListener(new OnClickListener(){
    			
				@Override
			    public void onClick(View view) {
					doResign();
				}
			});				
    	}
	}
	
	@Override
    public void onConnected(Bundle connectionHint) {
		// Don't need to do anything
    }
    
    public void onConnectionSuspended(int cause) {
    	mGoogleApiClient.connect();
    }
    
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
//        super.onBackPressed();
    	this.finish();
    }
	
	private void addHowToText()
	{
	    String line1= "Get 5 tiles in a row vertically, horizontally, or diagonally to win.";
	    
	    String lightning = getEmoji(-2);
	    String clover = getEmoji(-1);
	    
	    String line2 = clover + " is a wild that can be placed on any open space.";
	    String line3 = lightning + "can open up any space that your opponent occupies.";
	    
	    TextView desc = (TextView)findViewById(R.id.description);
	    desc.setText(line1 + "\n" + line2 + "\n" + line3);
	}
	
	private void assignModelFromId( String id ){
		this.model = Model.getModelFromId(id);
		
		final ProgressDialog progress = ProgressDialog.show(this, "Loading Game",
			    "Please wait while your game loads", true);
		
		Games.TurnBasedMultiplayer.loadMatch(mGoogleApiClient, id)
			.setResultCallback(new ResultCallback<TurnBasedMultiplayer.LoadMatchResult>(){
				@Override
				public void onResult(LoadMatchResult result) {
					if( result.getStatus().isSuccess() ){
						Connections.this.model.setMatch(result.getMatch());
						finishLoadModel();						
						progress.dismiss();
					}
					else{
						Connections.this.finish();
					}
				}
			});
	}
	
	private void loadModel(){
		if( this.model == null ){
			Intent i = getIntent();
			if( i != null ){
				String id = i.getStringExtra(Model.ID_TAG);
				if( id != null ){
					assignModelFromId(id);
				}
			}
		}
		// model could still be null
		if( this.model == null ){			
			SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			String id = settings.getString(MATCH_ID_TAG, "");
			if( id != null && !id.isEmpty() ){
				assignModelFromId(id);
			}
		}
		
		if( this.model == null ){
			finish();
			return;
		}
	}
	
	private void finishLoadModel(){		
		// set which owner we are
		this.owner = this.model.getOwnerForMe(this);
		if( this.owner < 1 || this.owner > 2 ){
			this.finish();
		}
		// enable/disable activity based on myTurn
		this.my_turn = this.model.isMyTurn();
		
		for(int row = 0; row < Model.ROWS; row++ ){    		
    		for( int column = 0; column < Model.COLUMNS; column++ ){
    			FrameLayout view = (FrameLayout)findViewById(widget_ids[row][column]);
    			
    			int card_owner = model.getOwnerAt(row, column);
    			int val = model.getValueAt(row, column);
    			
    			IndexedButton button = new IndexedButton(this, row, column, val, card_owner, this.owner);
    			
    			setUpButton(button);
            	
    			if( view.getChildCount() > 0 )
    				view.removeAllViews();
    			
            	view.addView(button);
            	widgets[row][column] = button;
    		}
    	}
    	
    	for( int i = 0; i < Model.PLAYER_CARDS; i++ ){    			
			FrameLayout view = (FrameLayout)findViewById(player_card_ids[i]);
			
			int val = model.getPlayerCardAt( this.owner, i );
		
			IndexedButton child = new IndexedButton(this, -1, i, val, this.owner, this.owner);
        	setUpButton( child );
			if( view.getChildCount() > 0 )
				view.removeAllViews();
			
        	view.addView(child);
        	player_cards[i] = child;
    	}
    	this.setUpResignButton();
	}

    private void setUpButton( IndexedButton button ){
//    	button.setLayoutParams(new FrameLayout.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT, 1.0f));
		button.setBackgroundResource(R.drawable.custom_button);
		button.setOnClickListener(new TileClickListener());
		
		button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
		
		button.updateBackground();

		button.setText(getEmoji(button.getValue()));
    }
    
    protected LinkedList<IndexedButton> getEligibleDropTargets( int value ){
    	LinkedList<IndexedButton> eligible = new LinkedList<IndexedButton>();
    	
    	synchronized( Connections.this ){
    		for( int row = 0; row < Model.ROWS; row++ ){
    			for( int col = 0; col < Model.COLUMNS; col++ ){
    				if( value < 0 ){
    					int tileOwner = model.getOwnerAt(row, col);
    					if( value == -2 ){
    						// can remove any of the opponents cards
    						if( tileOwner != 0 && tileOwner != this.owner ){
    			    			eligible.add(widgets[row][col]);
    			    		}
    					}
    					else{
    						// can play on any space
    						if( tileOwner == 0 ){
    			    			eligible.add(widgets[row][col]);
    			    		}
    					}
    				}
    				else if( model.getValueAt(row, col) == value ){
			    		if( model.getOwnerAt(row, col) == 0 ){
			    			eligible.add(widgets[row][col]);
			    		}
			    	}
    			}
    		}
    		return eligible;
    	}
    }
    
    private void highlightTileClicked( IndexedButton button ){
    	boolean lightning = false;
    	for( IndexedButton player_button:this.player_cards ){
    		if( player_button.isHighlighted() && player_button.getValue() == -2 ){
    			lightning = true;
    			break;
    		}
    	}
    	if( lightning ){
			// remove card
    		model.setOwnerAt( 0, button.getRow(), button.getColumn() );
	    	button.setCardOwner(0);
		}
    	else{
	    	model.setOwnerAt( this.owner, button.getRow(), button.getColumn() );
	    	button.setCardOwner(this.owner);
    	}
    	
    	for( int i = 0; i < Model.PLAYER_CARDS; i++ ){
			IndexedButton ib = this.player_cards[i];
			if( ib.isHighlighted() ){
				int value = model.getNextPlayerOption( i, this.owner );
				ib.setValue(value);
				setUpButton(ib);
			}
    	}
    	
    	boolean winner = model.checkForWinner( this.owner, button.getRow(), button.getColumn() );
    	
    	if( winner ){
    		// submit winner
    		byte[] data = model.storeToData();
    		Participant opponent = model.getOpponent();
    		
    		LinkedList<ParticipantResult> results = new LinkedList<ParticipantResult>();
    		ParticipantResult myResult = new ParticipantResult(model.getMyPartId(), ParticipantResult.MATCH_RESULT_WIN, 1);
    		results.add(myResult);
    		
    		ParticipantResult oppResult = new ParticipantResult(opponent.getParticipantId(), ParticipantResult.MATCH_RESULT_LOSS, 2);
    		results.add(oppResult);
    		
    		try{
	    		Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, model.getMatchId(), data, results)
	    								  .setResultCallback(new ResultCallback<UpdateMatchResult>(){
	    									  @Override
	    									  public void onResult(UpdateMatchResult updateMatchResult) {
	    										  if( !updateMatchResult.getStatus().isSuccess() ){
	    											  String message = updateMatchResult.getStatus().getStatusMessage();
	    											  System.out.println(message);
	    											  Toast.makeText(Connections.this, "Check you internet connection, or try again later.", Toast.LENGTH_LONG).show();
	    									    	        	
	    									    	  // back to menu
	    									    	  Connections.this.finish();
	    										  }
	    										  else{
	    											  new AlertDialog.Builder(Connections.this)
	    									    	    .setTitle("You Won!")
	    									    	    .setMessage("Would you like to challenge your opponent to a rematch?")
	    									    	    .setPositiveButton("Rematch", new DialogInterface.OnClickListener() {
	    									    	        public void onClick(DialogInterface dialog, int which) { 
	    									    	            doRematch();
	    									    	        }
	    									    	     })
	    									    	    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
	    									    	        public void onClick(DialogInterface dialog, int which) { 
	    									    	            // do nothing
	    									    	        }
	    									    	     })
	    									    	     .show();
	    										  }
	    									  }
	    								  });

    			this.my_turn = false;
	    		
    		}
    		catch(Exception ex){
    			Model.showConnectionError(this, "Unable to Submit Move");
    			this.finish();
    		}
    	}
    	else{
        	// submit move
    		this.my_turn = false;
    		model.flipTurn();
    		byte[] data = model.storeToData();
    		Participant opponent = model.getOpponent();
    		try{
	    		Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, model.getMatchId(), data, opponent.getParticipantId())
	    								  .setResultCallback(new ResultCallback<UpdateMatchResult>(){
	    									  @Override
	    									  public void onResult(UpdateMatchResult updateMatchResult) {
	    										  if( !updateMatchResult.getStatus().isSuccess() ){
	    											  String message = updateMatchResult.getStatus().getStatusMessage();
	    											  System.out.println(message);
	    											  
	    											  Toast.makeText(Connections.this, "Check you internet connection, or try again later.", Toast.LENGTH_LONG).show();
	    									    	  model.flipTurn();
	    									    	        	
	    									    	  // back to menu
	    									    	  Connections.this.finish();
	    										  }
	    									  }
	    								  });
    		}
    		catch(IllegalStateException ex){
    			Model.showConnectionError(this, "Unable to Submit Move");
    			this.finish();
    		}
    	}
    	this.setUpResignButton();
    	
    	int rand = (int) (Math.random() * 10);
    	if( rand < 3 && this.mFlurryAdInterstitial != null ){
    		this.mFlurryAdInterstitial.fetchAd();
    	}
	}
    
    private void doRematch(){
    	// continue with rematch
    	try{
	    	Games.TurnBasedMultiplayer.rematch(mGoogleApiClient, this.model.getMatchId())
	    							  .setResultCallback(new ResultCallback<InitiateMatchResult>(){
				  @Override
				  public void onResult(InitiateMatchResult initiateMatchResult) {
					  if( !initiateMatchResult.getStatus().isSuccess() ){
						  Toast.makeText(Connections.this, "Check you internet connection, or try again later.", Toast.LENGTH_LONG).show();
				    	        	
				    	  // back to menu
				    	  Connections.this.finish();
					  }
					  else{
						  TurnBasedMatch match = initiateMatchResult.getMatch();
						  Model model = Model.submitNewMatch(match, mGoogleApiClient, Connections.this);
							
						  if( model != null ){
							  Intent i = new Intent( Connections.this, Connections.class );
							  i.putExtra(Model.ID_TAG, match.getMatchId());
							  Connections.this.startActivity(i);
						  }
					  }
				  }
			  });
    	}
    	catch(IllegalStateException ex){
    		Model.showConnectionError(this, "Unable to Start a Rematch");
    		this.finish();
    	}
    }

    private void doResign(){
    	new AlertDialog.Builder(Connections.this)
	    .setTitle("Resign")
	    .setMessage("Are you sure you would like to resign?")
	    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            doResignConfirmed();
	        }
	     })
	    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            // do nothing
	        }
	     })
	     .show();
    }
    
    private void doResignConfirmed(){
    	byte[] data = model.storeToData();
		Participant opponent = model.getOpponent();
		
		LinkedList<ParticipantResult> results = new LinkedList<ParticipantResult>();
		ParticipantResult myResult = new ParticipantResult(model.getMyPartId(), ParticipantResult.MATCH_RESULT_LOSS, 2);
		results.add(myResult);
		
		ParticipantResult oppResult = new ParticipantResult(opponent.getParticipantId(), ParticipantResult.MATCH_RESULT_WIN, 1);
		results.add(oppResult);
		
		this.my_turn = false;
		try{
			Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, model.getMatchId(), data, results)
									  .setResultCallback(new ResultCallback<UpdateMatchResult>(){
										  @Override
										  public void onResult(UpdateMatchResult updateMatchResult) {
											  if( !updateMatchResult.getStatus().isSuccess() ){
												  String message = updateMatchResult.getStatus().getStatusMessage();
    											  System.out.println(message);
    											  
    											  Toast.makeText(Connections.this, "Check you internet connection, or try again later.", Toast.LENGTH_LONG).show();

    											  Connections.this.finish();
											  }
											  else{
													new AlertDialog.Builder(Connections.this)
											    	    .setTitle("You Resigned")
											    	    .setMessage("Would you like to challenge your opponent to a rematch?")
											    	    .setPositiveButton("Rematch", new DialogInterface.OnClickListener() {
											    	        public void onClick(DialogInterface dialog, int which) { 
											    	            doRematch();
											    	        }
											    	     })
											    	    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
											    	        public void onClick(DialogInterface dialog, int which) { 
											    	            // do nothing
											    	        }
											    	     })
											    	     .show();
											  }
										  }
									  });
		}
		catch(IllegalStateException ex){
			Model.showConnectionError(this, "Unable to Resign");
			this.finish();
		}

    	this.my_turn = false;
    	setUpResignButton();
    }
    
    private void removeAllHighlighting(){
    	for( int i = 0; i < Model.ROWS; i++ ){
    		for( int j = 0; j < Model.COLUMNS; j++ ){
    			IndexedButton ib = this.widgets[i][j];
    			ib.setHighlighted(false);
    		}
    	}
    	
    	for( int i = 0; i < Model.PLAYER_CARDS; i++ ){
			IndexedButton ib = this.player_cards[i];
			ib.setHighlighted(false);
    	}
    }

    private final class TileClickListener implements OnClickListener 
    {
        @Override
	    public void onClick(View view) {
        	synchronized( Connections.this ){	
    			if( Connections.this.my_turn ){
			        IndexedButton button = (IndexedButton)view;
			        if( button.getRow() < 0 ){
			        	boolean wasHighlighted = button.isHighlighted();
			        	// remove all highlighting
			        	removeAllHighlighting();
			        	
			        	if( !wasHighlighted ){
			        		button.setHighlighted(true);
					        for( IndexedButton eligible:getEligibleDropTargets(button.getValue()) ){
					        	if( eligible != null )
					        		eligible.setHighlighted(true);
					        }	
			        	}
			        }
			        else{
			        	if( button.isHighlighted() ){
			        		// play the card on this spot
			        		highlightTileClicked( button );
			        		
				        	// remove all highlighting
				        	removeAllHighlighting();		        		
			        	}
			        }
    			}
    		}
        }
    }
    
    FlurryAdInterstitialListener interstitialAdListener = new FlurryAdInterstitialListener() {

        @Override
        public void onFetched(FlurryAdInterstitial adInterstitial) {
            adInterstitial.displayAd();
        }

        @Override
        public void onError(FlurryAdInterstitial adInterstitial, FlurryAdErrorType adErrorType, int errorCode) {
            adInterstitial.destroy();
        }
        //..
        //the remainder of listener callbacks 

		@Override
		public void onAppExit(FlurryAdInterstitial arg0) {
			// Auto-generated method stub
			
		}

		@Override
		public void onClicked(FlurryAdInterstitial arg0) {
			// Auto-generated method stub
			
		}

		@Override
		public void onClose(FlurryAdInterstitial arg0) {
			// Auto-generated method stub
			
		}

		@Override
		public void onDisplay(FlurryAdInterstitial arg0) {
			// Auto-generated method stub
			
		}

		@Override
		public void onRendered(FlurryAdInterstitial arg0) {
			// Auto-generated method stub
			
		}

		@Override
		public void onVideoCompleted(FlurryAdInterstitial arg0) {
			// Auto-generated method stub
			
		}
    };
    
    FlurryAdBannerListener bannerAdListener = new FlurryAdBannerListener() {
        
        @Override
        public void onFetched(FlurryAdBanner adBanner) {
               adBanner.displayAd();
        }

        @Override
        public void onError(FlurryAdBanner adBanner, FlurryAdErrorType adErrorType, int errorCode) {
             adBanner.destroy();
        }
       //..
       //the remainder of the listener callback methods

		@Override
		public void onAppExit(FlurryAdBanner arg0) {
			// Auto-generated method stub
			
		}

		@Override
		public void onClicked(FlurryAdBanner arg0) {
			// Auto-generated method stub
			
		}

		@Override
		public void onCloseFullscreen(FlurryAdBanner arg0) {
			// Auto-generated method stub
			
		}

		@Override
		public void onRendered(FlurryAdBanner arg0) {
			// Auto-generated method stub
			
		}

		@Override
		public void onShowFullscreen(FlurryAdBanner arg0) {
			// Auto-generated method stub
			
		}

		@Override
		public void onVideoCompleted(FlurryAdBanner arg0) {
			// Auto-generated method stub
			
		}
    };

	private String getEmoji(int value){
		int unicode;
		
		switch( value ){
			case -2:
				unicode = 0x26A1; // lightning
				break;
			case -1:
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
