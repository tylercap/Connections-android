package com.gmail.tylercap4.connections;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.gmail.tylercap4.connections.basegameutils.BaseGameUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.LoadMatchesResult;
import com.google.android.gms.plus.Plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainMenu extends Activity implements OnTurnBasedMatchUpdateReceivedListener, OnInvitationReceivedListener, 
												  ConnectionCallbacks, OnConnectionFailedListener
{
	private static final String SIGNED_IN_KEY = "SIGNED_IN";
	private static final int 	RC_SIGN_IN = 9001;
	private static final int 	RC_SELECT_PLAYERS = 9002;
	private static final int 	RC_GAMES_INBOX = 9003;
	private static final String CONNECTED_SUCCESS_ACTION = "SUCCESSFULLY_CONNECTED";
	
	private static final String SIGN_IN_ROW = "Sign in to access your games";
//	private static final String QUICK_MATCH = "Quick Match";
//	private static final String CHOOSE_OPPONENT = "Choose Opponent";
	private static final String NEW_GAME = "New Game";
	private static final String GAMES_INBOX = "Games Inbox";
	private static final String INVITED = "You've been invited!";
	private static final String YOUR_TURN = "It's your turn!";
	private static final String MATCH_ENDED = "Match has ended!";
	
	private MenuItem sign_in;
	
	/* Client used to interact with Google APIs. */
	protected GoogleApiClient mGoogleApiClient;

	private boolean mResolvingConnectionFailure = false;
	private boolean mSignInClicked = false;
	
	private LoadMatchesResult previous_result;
	
	private List<TurnBasedMatch> matches;
	private List<Invitation> invitations;
	private List<String> game_strings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
	}
	
	@Override
	protected void onResume(){
		super.onResume();		
		
		MyReceiver receiver = new MyReceiver();

        // The filter's action is CONNECTED_SUCCESS_ACTION
        IntentFilter mStatusIntentFilter = new IntentFilter(CONNECTED_SUCCESS_ACTION);

        // Registers the receiver with the new filter
        LocalBroadcastManager.getInstance(this).registerReceiver(
                receiver,
                mStatusIntentFilter);
        
        mGoogleApiClient = new GoogleApiClient.Builder(this)
		        .addConnectionCallbacks(this)
		        .addOnConnectionFailedListener(this)
		        .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();	
    	
        matches = new LinkedList<TurnBasedMatch>();
        invitations = new LinkedList<Invitation>();
        
        reloadSignIn(); 
        updateSignIn();
		
		Timer timer = new Timer();
		TimerTask intervalUpdate = new TimerTask(){
			@Override
			public void run() {
				MainMenu.this.runOnUiThread(new Runnable() {
				    public void run() {
				    	updateSignIn();
				    }
				});
			}
		};
		timer.schedule(intervalUpdate, 5000, 60000);
	}
	
	private void updateSignIn(){
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			if( this.sign_in != null )
				this.sign_in.setTitle(R.string.sign_out);

			// show email for user signed in
//			String username = Plus.AccountApi.getAccountName(mGoogleApiClient);
		}
		else{
			if( this.sign_in != null )
				this.sign_in.setTitle(R.string.sign_in);
		}
		
		updateGameList(this.mGoogleApiClient.isConnected());
	}
	
	private void signedOut(){
		if( this.sign_in != null )
			this.sign_in.setTitle(R.string.sign_in);
	
		updateGameList(false);
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
	      
    	final Runnable r = new Runnable() {
    	    public void run() {
    	    	boolean try_again = false;
    	    	while( !mGoogleApiClient.isConnected() ){
    	    		if( try_again ){
    	    			try_again = false;
    	    			mGoogleApiClient.connect();
    	    		}
    	    		else{
    	    			try_again = true;
    	    		}
    	    		
    	    		try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// Auto-generated catch block
						e.printStackTrace();
					}
    	    	}
    	    	
    	    	//send a broadcast message for us to update our sign in
    	    	Intent localIntent = new Intent(CONNECTED_SUCCESS_ACTION);
                LocalBroadcastManager.getInstance(MainMenu.this).sendBroadcast(localIntent);
    	    }
    	};

    	new Thread(r).start();
    }
	
	public void onStop(){
		super.onStop();
		
		if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ){
			Games.Invitations.unregisterInvitationListener(mGoogleApiClient);
	    	Games.TurnBasedMultiplayer.unregisterMatchUpdateListener(mGoogleApiClient);

	    	mGoogleApiClient.disconnect();
		}
	}
	
	private void doOnConnected(){
		if( mGoogleApiClient == null || !mGoogleApiClient.isConnected() ){
			signedOut();
		}
		else{
	    	Games.Invitations.registerInvitationListener(mGoogleApiClient, this);
	    	Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, this);
	    	updateSignIn();
		}
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
	
	private void updateGameList(boolean signed_in){
		synchronized(this){
			if( signed_in && this.mGoogleApiClient.isConnected() ){				
				// add open matches
				int[] statuses = TurnBasedMatch.MATCH_TURN_STATUS_ALL;
				Games.TurnBasedMultiplayer.loadMatchesByStatus( mGoogleApiClient, statuses )
					.setResultCallback( new ResultCallback<LoadMatchesResult>()
					{
						@Override
						public void onResult( LoadMatchesResult result )
						{
							if( previous_result != null ){
								previous_result.release();
							}
							
							previous_result = result;
							if( result.getStatus().getStatusCode() != GamesStatusCodes.STATUS_OK )
							{
								System.out.println(result.getStatus().getStatusMessage());
							}
							else
							{
								try{
									LinkedList<String> my_turn_strings = new LinkedList<String>();
									LinkedList<TurnBasedMatch> my_turn_matches = new LinkedList<TurnBasedMatch>();
									LinkedList<String> their_turn_strings = new LinkedList<String>();
									LinkedList<TurnBasedMatch> their_turn_matches = new LinkedList<TurnBasedMatch>();
									LinkedList<String> completed_strings = new LinkedList<String>();
									LinkedList<TurnBasedMatch> completed_matches = new LinkedList<TurnBasedMatch>();
									game_strings = new LinkedList<String>();
									matches = new LinkedList<TurnBasedMatch>();
									invitations = new LinkedList<Invitation>();
	
									game_strings.add(GAMES_INBOX);
									game_strings.add(NEW_GAME);
									
									String myId = Games.Players.getCurrentPlayer(mGoogleApiClient).getPlayerId();
																	
									for( int i = 0; i < result.getMatches().getInvitations().getCount(); i++ ){
										Invitation invite = result.getMatches().getInvitations().get( i ); 
										invitations.add( invite );
										String opponent = invite.getInviter().getDisplayName();
										game_strings.add(opponent + ": Your Invited");
									}
										
									for( int i = 0; i < result.getMatches().getMyTurnMatches().getCount(); i++ ){
										TurnBasedMatch match = result.getMatches().getMyTurnMatches().get( i ); 
										Participant opponentPart = Model.getOpponentFromMatch(match, myId);
										String opponent = opponentPart.getDisplayName();
										ParticipantResult pr = opponentPart.getResult();
										
										if( pr == null ){
											my_turn_matches.add(match);
											my_turn_strings.add(opponent + ": Your Turn");
										}
										else if( pr.getResult() == ParticipantResult.MATCH_RESULT_WIN ){
											completed_matches.add(match);
											completed_strings.add(opponent + ": You Lost");
										}
										else if( pr.getResult() == ParticipantResult.MATCH_RESULT_LOSS ){
											completed_matches.add(match);
											completed_strings.add(opponent + ": You Won!");
										}
										else{
											my_turn_matches.add(match);
											my_turn_strings.add(opponent + ": Your Turn");
										}
									}
										
									for( int i = 0; i < result.getMatches().getTheirTurnMatches().getCount(); i++ ){
										TurnBasedMatch match = result.getMatches().getTheirTurnMatches().get( i );
										Participant opponentPart = Model.getOpponentFromMatch(match, myId);
										String opponent = opponentPart.getDisplayName();
										ParticipantResult pr = opponentPart.getResult();
										
										if( pr == null ){
											their_turn_matches.add(match);
											their_turn_strings.add(opponent + ": Their Turn");
										}
										else if( pr.getResult() == ParticipantResult.MATCH_RESULT_WIN ){
											completed_matches.add(match);
											completed_strings.add(opponent + ": You Lost");
										}
										else if( pr.getResult() == ParticipantResult.MATCH_RESULT_LOSS ){
											completed_matches.add(match);
											completed_strings.add(opponent + ": You Won!");
										}
										else{
											their_turn_matches.add(match);
											their_turn_strings.add(opponent + ": Their Turn");
										}
									}
									
									for( int i = 0; i < result.getMatches().getCompletedMatches().getCount(); i++ ){
										TurnBasedMatch match = result.getMatches().getCompletedMatches().get( i ); 
	//									String myPartId = match.getParticipantId(myId);
										Participant opponentPart = Model.getOpponentFromMatch(match, myId);
										String opponent = opponentPart.getDisplayName();
										ParticipantResult pr = opponentPart.getResult();
										
										if( pr == null ){
											completed_matches.add(match);
											completed_strings.add(opponent + ": Expired");
										}
										else if( pr.getResult() == ParticipantResult.MATCH_RESULT_WIN ){
											completed_matches.add(match);
											completed_strings.add(opponent + ": You Lost");
										}
										else if( pr.getResult() == ParticipantResult.MATCH_RESULT_LOSS ){
											completed_matches.add(match);
											completed_strings.add(opponent + ": You Won!");
										}
										else{
											completed_matches.add(match);
											completed_strings.add(opponent + ": Expired");
										}
									}
									
									matches.addAll(my_turn_matches);
									matches.addAll(their_turn_matches);
									matches.addAll(completed_matches);
									
									game_strings.addAll(my_turn_strings);
									game_strings.addAll(their_turn_strings);
									game_strings.addAll(completed_strings);
									
									reloadDisplay();
								}
								//						result.release();
								catch( IllegalStateException ex ){
									Model.showConnectionError(MainMenu.this, "Unable To Refresh Game List");
								}								
							}
						}
					});
	
			}
			else{
				game_strings = new LinkedList<String>();
				game_strings.add(SIGN_IN_ROW);
				reloadDisplay();	
			}
		}
	}			

	private void reloadDisplay(){
		ListView games_list = (ListView) findViewById(R.id.games_list);
        // Set the adapter for the list view
        games_list.setAdapter (new ArrayAdapter<String>(this, R.layout.game_list_item,
                R.id.game_label, game_strings) );

        games_list.setOnItemClickListener(new GameClickListener());
	}

	@Override
	public void onTurnBasedMatchReceived(final TurnBasedMatch match) {
		// display info for match event
		updateGameList(mGoogleApiClient!=null && mGoogleApiClient.isConnected());
		
		try{
			String myId = Games.Players.getCurrentPlayer(mGoogleApiClient).getPlayerId();
			String opponent = Model.getOpponentFromMatch(match, myId).getDisplayName();
			
			if( match.getStatus() == TurnBasedMatch.MATCH_STATUS_ACTIVE ){			
				new AlertDialog.Builder(MainMenu.this)
				    .setTitle(YOUR_TURN)
				    .setMessage(opponent + " just took their turn in a match. Would you like to jump to that game now?")
				    .setPositiveButton("Sure!", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				        	// go to this match
				        	openMatch(match);
				        }
				     })
				     .setNegativeButton("No", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				        	// do nothing
				        }
				     })
				     .show();
			}
			else{
				new AlertDialog.Builder(MainMenu.this)
				    .setTitle(MATCH_ENDED)
				    .setMessage(opponent + " just finished a match. Would you like to view the results now?")
				    .setPositiveButton("Sure!", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				        	// go to this match
				        	openMatch(match);
				        }
				     })
				     .setNegativeButton("No", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				        	// do nothing
				        }
				     })
				     .show();
			}
		}
		catch(IllegalStateException ex){
			Model.showConnectionError(MainMenu.this, "Unable To Go To Game");
		}
	}
	
	private void acceptInvitation(Invitation invitation){
		try{
        	Games.TurnBasedMultiplayer.acceptInvitation(mGoogleApiClient, invitation.getInvitationId())
        							  .setResultCallback(new MatchInitiatedCallback());
    	}
    	catch(IllegalStateException ex){
    		Model.showConnectionError(MainMenu.this, "Unable To Accept Invitation");
    	}
	}

	@Override
	public void onTurnBasedMatchRemoved(String matchId) {
		updateGameList(mGoogleApiClient!=null && mGoogleApiClient.isConnected());
	}

	@Override
	public void onInvitationReceived(Invitation invitation) {
		updateGameList(mGoogleApiClient!=null && mGoogleApiClient.isConnected());
		handleInvitation(invitation, false);
	}
	
	private void handleInvitation(final Invitation invitation, final boolean decline) {
		String opponent = invitation.getInviter().getDisplayName();
		
		String cancel = "Not Now";
		if( decline ){
			cancel = "Decline";
		}
		
		new AlertDialog.Builder(MainMenu.this)
		    .setTitle(INVITED)
		    .setMessage(opponent + " just invited you to a game. Would you like to play now?")
		    .setPositiveButton("Sure!", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) {
		        	// go to this match
		        	acceptInvitation(invitation);
		        }
		     })
		     .setNegativeButton(cancel, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) {
		        	if( decline ){
			        	// decline invite
		        		try{
				        	Games.TurnBasedMultiplayer.declineInvitation(mGoogleApiClient, invitation.getInvitationId());
				    		updateGameList(mGoogleApiClient!=null && mGoogleApiClient.isConnected());
		        		}
		        		catch(IllegalStateException ex){
		        			Model.showConnectionError(MainMenu.this, "Unable To Decline Invitation");
		        		}
		        	}
		        	else{
		        		// just save it for later
		        	}
		        }
		     })
		     .show();
	}

	@Override
	public void onInvitationRemoved(String matchId) {
		updateGameList(mGoogleApiClient!=null && mGoogleApiClient.isConnected());
	}
	
	private class GameClickListener implements OnItemClickListener
    {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);			
		}
    }
	
	private void selectItem(int position){
//		System.out.println("Selected Item");
		String game = game_strings.get(position);
		
		switch( game ){
			case SIGN_IN_ROW:
				// do nothing
				return;
			case GAMES_INBOX:
				loadInbox();
				return;
			case NEW_GAME:
				chooseOpponent();
				return;
		}
		
		if( position >= 2 && position < (invitations.size() + 2) ){
			// handle the invitation
			Invitation invite = invitations.get(position - 2);
			handleInvitation(invite, true);
		}
		else if( position >= (invitations.size() + 2) ){
			// open existing match
			TurnBasedMatch match = matches.get(position - (invitations.size() + 2));
			
			// for testing to delete invalid matches
//			Games.TurnBasedMultiplayer.cancelMatch(mGoogleApiClient, match.getMatchId());
//			Games.TurnBasedMultiplayer.dismissMatch(mGoogleApiClient, match.getMatchId());
			openMatch(match);		
		}
	}
	
	private void loadInbox(){
		Intent inbox = Games.TurnBasedMultiplayer.getInboxIntent(mGoogleApiClient);
		startActivityForResult( inbox, RC_SELECT_PLAYERS );
	}
	
	private void openMatch(TurnBasedMatch match){
		try{
			String myId = Games.Players.getCurrentPlayer(mGoogleApiClient).getPlayerId();
//			System.out.println("Create Model");
			Model model = new Model(match, myId);
			openMatch(match.getMatchId(), model);
		}
		catch(IllegalStateException ex){
			Model.showConnectionError(MainMenu.this, "Unable To Open Match");
		}
	}
	
	private void openMatch(String id, Model model){
//		System.out.println("Open Match");
		Intent i = new Intent( this, Connections.class );
		i.putExtra(Model.ID_TAG, id);
		startActivity(i);
	}
	
	private void chooseOpponent(){
		try{
			Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 1, true);
			startActivityForResult( intent, RC_SELECT_PLAYERS );
		}
		catch(IllegalStateException ex){
			Model.showConnectionError(MainMenu.this, "Unable To Load Player Chooser");
		}
	}
	
	private void startQuickMatch(){
        int minAutoMatchPlayers = 1;
        int maxAutoMatchPlayers = 1;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);

        TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                .setAutoMatchCriteria(autoMatchCriteria)
                .build();

        try{
	        // Create and start the match.
	        Games.TurnBasedMultiplayer
	            .createMatch(mGoogleApiClient, tbmc)
	            .setResultCallback(new MatchInitiatedCallback());
        }
        catch(IllegalStateException ex){
        	Model.showConnectionError(MainMenu.this, "Unable To Create Match");
        }
	}
	
	@Override
    public void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        
        if (request == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (response == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                // Bring up an error dialog to alert the user that sign-in
                // failed. The R.string.signin_failure should reference an error
                // string in your strings.xml file that tells the user they
                // could not be signed in, such as "Unable to sign in."
                BaseGameUtils.showActivityResultError(this,
                    request, response, R.string.sign_in_failed);
            }
        }
        else if (request == RC_SELECT_PLAYERS) {
            if (response != RESULT_OK) {
                // user canceled
                return;
            }

            // Get the invitee list.
            final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

            // Get auto-match criteria.
            int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            if (minAutoMatchPlayers > 0) {
            	startQuickMatch();
            }
            else{
            	final TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                        .addInvitedPlayers(invitees)
                        .setAutoMatchCriteria(null)
                        .build();

                // Create and start the match.
            	// wait until we reconnect to call this
            	final Runnable r = new Runnable() {
            	    public void run() {
            	    	while( !mGoogleApiClient.isConnected() ){
            	    		try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								// Auto-generated catch block
								e.printStackTrace();
							}
            	    	}
     
            	    	try{
	            	        Games.TurnBasedMultiplayer
		                         .createMatch(mGoogleApiClient, tbmc)
		                         .setResultCallback(new MatchInitiatedCallback());
            	    	}
            	    	catch(IllegalStateException ex){
            	    		Model.showConnectionError(MainMenu.this, "Unable To Create Match");
            	    	}
            	    }
            	};

            	new Thread(r).start();
            }
        }
        else if (request == RC_GAMES_INBOX) {
            if (response != RESULT_OK) {
                // user canceled
                return;
            }

            TurnBasedMatch match = data.getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);
            if( match != null ){
            	openMatch(match);
            }
            else{
            	Invitation invite = data.getParcelableExtra(Multiplayer.EXTRA_INVITATION);
            	
            	if( invite != null ){
            		
            	}
            }
        }
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
                    
                    signedOut();
                    updateGameList(false);
			}
			else if (!mGoogleApiClient.isConnecting()) {
            		mSignInClicked = true;
                    mGoogleApiClient.connect();
			}
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	public class MatchInitiatedCallback implements ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>
	{		
		@Override
		public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
		    // Check if the status code is not success.
		    Status status = result.getStatus();
		    if (!status.isSuccess()) {
		    	Model.showConnectionError(MainMenu.this, "Unable To Submit Move");
		    }
		    else{
			    TurnBasedMatch match = result.getMatch();
			
			    Model model = Model.submitNewMatch(match, mGoogleApiClient, MainMenu.this);
			    if( model != null ){
			    	openMatch(match.getMatchId(), model);
			    }
		    }
		}
	}
	
	public class MyReceiver extends BroadcastReceiver {
		  @Override
		  public void onReceive(Context context, Intent intent) {
			  String action = intent.getAction();

			  if( action.equals(CONNECTED_SUCCESS_ACTION) ) {
				  doOnConnected();
			  }
		  }
	} 
}
