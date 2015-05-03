package com.gmail.tylercap4.connections;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.LoadMatchesResult;
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
import android.widget.Toast;

public class MainMenu extends Activity implements OnTurnBasedMatchUpdateReceivedListener, OnInvitationReceivedListener, 
												  ConnectionCallbacks, OnConnectionFailedListener
{
	private static final String SIGNED_IN_KEY = "SIGNED_IN";
	private static final int 	RC_SIGN_IN = 9001;
	private static final int 	RC_SELECT_PLAYERS = 9002;
	
	private static final String sign_in_row = "Sign in to access your games";
	private static final String quick_match = "Quick Match";
	private static final String choose_opponent = "Choose Opponent";
//	private static final String invited = "You've been invited!";
//	private static final String your_turn = "It's your turn!";
//	private static final String match_ended = "Match has ended!";
	
	private MenuItem sign_in;
	
	/* Client used to interact with Google APIs. */
	protected GoogleApiClient mGoogleApiClient;

	private boolean mResolvingConnectionFailure = false;
	private boolean mSignInClicked = false;
	
	private List<String> game_strings;
	private List<TurnBasedMatch> matches;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
	}
	
	@Override
	protected void onResume(){
		super.onResume();		
        
        mGoogleApiClient = new GoogleApiClient.Builder(this)
		        .addConnectionCallbacks(this)
		        .addOnConnectionFailedListener(this)
		        .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();	
    	
    	reloadSignIn();
    	
		updateSignIn();
	}
	private void updateSignIn(){
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			updateSignIn( true );
		}
		else{
			updateSignIn( false );
		}
	}
	
	private void updateSignIn( boolean signed_in ){
		if( signed_in ){
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
	      
    	Games.Invitations.registerInvitationListener(mGoogleApiClient, this);
    	Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, this);
    	updateSignIn(true);
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
		game_strings = new LinkedList<String>();
		if( signed_in && this.mGoogleApiClient.isConnected() ){
			game_strings.add(quick_match);
			game_strings.add(choose_opponent);
			
			//TODO: add open matches
			matches = new LinkedList<TurnBasedMatch>();
			int[] statuses = new int[] { TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN, TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN, TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE };
			Games.TurnBasedMultiplayer.loadMatchesByStatus( mGoogleApiClient, statuses )
				.setResultCallback( new ResultCallback<LoadMatchesResult>()
				{
					@Override
					public void onResult( LoadMatchesResult result )
					{
						if( result.getStatus().getStatusCode() != GamesStatusCodes.STATUS_OK )
						{
							System.out.println(result.getStatus().getStatusMessage());
						}
						else
						{							
							for( int i = 0; i < result.getMatches().getCompletedMatches().getCount(); i++ ){
								TurnBasedMatch match = result.getMatches().getCompletedMatches().get( i ); 
								matches.add( match );
								String opponent = Model.getOpponentFromMatch(match).getDisplayName();
								game_strings.add(opponent);
							}
							
//							for( int i = 0; i < result.getMatches().getInvitations().getCount(); i++ ){
//								result.getMatches().getInvitations().get( i );
//							}
								
							for( int i = 0; i < result.getMatches().getMyTurnMatches().getCount(); i++ ){
								TurnBasedMatch match = result.getMatches().getMyTurnMatches().get( i ); 
								matches.add( match );
								String opponent = Model.getOpponentFromMatch(match).getDisplayName();
								game_strings.add(opponent + ": Your Turn");
							}
								
							for( int i = 0; i < result.getMatches().getTheirTurnMatches().getCount(); i++ ){
								TurnBasedMatch match = result.getMatches().getTheirTurnMatches().get( i );								 
								matches.add( match );
								String opponent = Model.getOpponentFromMatch(match).getDisplayName();
								game_strings.add(opponent + ": Their Turn");
							}
							
							reloadDisplay();
						}
					}
				});

		}
		else{
			game_strings.add(sign_in_row);
		}
		reloadDisplay();	
	}	
	
//	            case GPGTurnBasedUserMatchStatusMatchCompleted: //Completed match
//	                for (GPGTurnBasedParticipantResult *result in match.results)
//	                {
//	                    if( [result.participantId isEqualToString:[model getOpponent].participantId] ){
//	                        // opponent result
//	                        if( result.result == GPGTurnBasedParticipantResultStatusWin ){
//	                            resultStr = @"You Lost";
//	                        }
//	                        if( result.result == GPGTurnBasedParticipantResultStatusLoss ){
//	                            resultStr = @"You Won!";
//	                        }
//	                    }
//	                    else{
//	                        // my result
//	                        if( result.result == GPGTurnBasedParticipantResultStatusWin ){
//	                            resultStr = @"You Won!";
//	                        }
//	                        if( result.result == GPGTurnBasedParticipantResultStatusLoss ){
//	                            resultStr = @"You Lost";
//	                        }
//	                    }
//	                }		

	private void reloadDisplay(){
		ListView games_list = (ListView) findViewById(R.id.games_list);
        // Set the adapter for the list view
        games_list.setAdapter (new ArrayAdapter<String>(this, R.layout.game_list_item,
                R.id.game_label, game_strings) );

        games_list.setOnItemClickListener(new GameClickListener());
	}

	@Override
	public void onTurnBasedMatchReceived(TurnBasedMatch arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTurnBasedMatchRemoved(String matchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInvitationReceived(Invitation arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInvitationRemoved(String arg0) {
		// TODO Auto-generated method stub
		
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
		
		switch( game ){
			case sign_in_row:
				// do nothing
				return;
			case quick_match:
				startQuickMatch();
				return;
			case choose_opponent:
				chooseOpponent();
				return;
		}

		// open existing match
		TurnBasedMatch match = matches.get(position - 2);
		openMatch(match);		
	}
	
	private Model submitNewMatch(TurnBasedMatch match){
		// submit the match to gpg with initial data
		String myId = Games.Players.getCurrentPlayer(mGoogleApiClient).getPlayerId();
		Model model = new Model(match, myId);
		byte[] data = model.storeToData();
		Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(), data, myId);
		
		return model;
	}
	
	private void openMatch(TurnBasedMatch match){
		String myId = Games.Players.getCurrentPlayer(mGoogleApiClient).getPlayerId();
		Model model = new Model(match, myId);
		openMatch(match.getMatchId(), model);
	}
	
	private void openMatch(String id, Model model){
		Intent i = new Intent( this, Connections.class );
		i.putExtra(Model.ID_TAG, id);
		startActivity(i);
	}
	
	private void chooseOpponent(){
		Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 1, true);
		startActivityForResult( intent, RC_SELECT_PLAYERS );
	}
	
	private void startQuickMatch(){
        int minAutoMatchPlayers = 1;
        int maxAutoMatchPlayers = 1;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);

        TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                .setAutoMatchCriteria(autoMatchCriteria)
                .build();

        // Create and start the match.
        Games.TurnBasedMultiplayer
            .createMatch(mGoogleApiClient, tbmc)
            .setResultCallback(new MatchInitiatedCallback());
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
            if (response != Activity.RESULT_OK) {
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
            	TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                        .addInvitedPlayers(invitees)
                        .setAutoMatchCriteria(null)
                        .build();

                // Create and start the match.
                Games.TurnBasedMultiplayer
                    .createMatch(mGoogleApiClient, tbmc)
                    .setResultCallback(new MatchInitiatedCallback());
            }
        }
    }
	
//	- (void)submitRematch:(GPGTurnBasedMatch*)match
//	{
//	    [match rematchWithCompletionHandler:^(GPGTurnBasedMatch *rematch, NSError *error) {
//	        // submitNewMatch in MyTableViewController
//	        [self submitNewMatch:rematch];
//	    }];
//	}
//
//	- (void)submitNewMatch:(GPGTurnBasedMatch*)match
//	{
//	    GPGTurnBasedParticipant *me = match.localParticipant;
//	    if( me == nil ){
//	        [[[UIAlertView alloc] initWithTitle:@"Unable To Create New Game"
//	                                    message:@"Check you internet connection, or try again later."
//	                                   delegate:self
//	                          cancelButtonTitle:@"Okay"
//	                          otherButtonTitles:nil] show];
//	        
//	        [match dismissWithCompletionHandler:nil];
//	        [match cancelWithCompletionHandler:nil];
//	        return;
//	    }
//	    
//	    Model *model = [[Model alloc]init];
//	    [model loadNewGame:match localParticipant:me];
//	    
//	    NSData *data = [model storeToData];
//	    [match takeTurnWithNextParticipantId:me.participantId data:data results:match.results completionHandler:^(NSError *error)
//	     {
//	         if (error) {
//	             [[[UIAlertView alloc] initWithTitle:@"Unable To Create New Game"
//	                                         message:@"Check you internet connection, or try again later."
//	                                        delegate:self
//	                               cancelButtonTitle:@"Okay"
//	                               otherButtonTitles:nil] show];
//	             
//	             [match dismissWithCompletionHandler:nil];
//	             [match cancelWithCompletionHandler:nil];
//	             return;
//	         } else {
//	             [self performSegueWithIdentifier:openGame sender:match];
//	         }
//	     }];
//	    
//	}

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
                    
                    updateSignIn(false);
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
		    if (status.isSuccess()) {
		    	Toast.makeText(MainMenu.this, status.getStatusMessage(), Toast.LENGTH_SHORT).show();
		        return;
		    }
		
		    TurnBasedMatch match = result.getMatch();
		
		    openMatch(match.getMatchId(), submitNewMatch(match));
		}
	}
}
