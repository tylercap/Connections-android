package com.gmail.tylercap4.connections;

import java.util.LinkedList;
import java.util.List;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.TextView;

public class Connections extends Activity
{		
	public static final int ROWS = 9;
	public static final int COLUMNS = 8;
	public static final int PLAYER_CARDS = 6;

    private AdView adMobView;
    
	private TurnBasedMultiplayer multiplayer;
	private TurnBasedMatch match;
	private int owner = 1;
	
	private int [][] widget_ids;
	private int [] player_card_ids;
	
	private IndexedButton [][] 	widgets;
	private IndexedButton [] 	player_cards;
	
	private int [][] 			gameboard;
	private int [][] 			owner_vals;
	
	private LinkedList<Integer> deck;
	private int[]				owner1_cards;
	private int[]				owner2_cards;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connections);
		
		// Load the banner ad
        adMobView = (AdView) findViewById(R.id.adView);
        adMobView.loadAd(new AdRequest.Builder().build());
	}
    
    @Override
    protected void onPause(){
    	super.onPause();
        adMobView.pause();
    }
	
	@Override
	protected void onResume(){
		super.onResume();
        adMobView.resume();

		if( gameboard == null ){
			widget_ids = new int[ROWS][COLUMNS];
			player_card_ids = new int[PLAYER_CARDS];
			
			gameboard = new int[ROWS][COLUMNS];
			owner_vals = new int[ROWS][COLUMNS];
			
			widgets = new IndexedButton[ROWS][COLUMNS];
			player_cards = new IndexedButton[PLAYER_CARDS];
			
			owner1_cards = new int[PLAYER_CARDS];
			owner2_cards = new int[PLAYER_CARDS];
			
			initWidgetIds();
		}
		
		if( match == null || match.getData() == null ){
			doNewGame();
		}
		else{
			loadFromMatch();
		}
		
		addHowToText();
	}
	
	private void loadFromMatch(){
		//TODO: load
	}
	
	private void doNewGame(){
    	synchronized( this ){	    	
    		List<Integer> vals = new LinkedList<Integer>();
    		deck = new LinkedList<Integer>();
	    	for( int val = 0; val <= (ROWS * COLUMNS / 2); val++ ){
		    	for( int i = 0; i < 2; i++ ){
		    		deck.add(val);
		    		vals.add(val);
		    	}
	    	}
	    	
	    	// now fill the table from the list
	    	int remaining = ROWS * COLUMNS;
	    	for(int row = 0; row < ROWS; row++ ){    		
	    		for( int column = 0; column < COLUMNS; column++ ){
	    			int index = (int) (Math.random() * remaining);
	    			int val = vals.remove(index);
	    			remaining--;
	    			
	    			gameboard[row][column] = val;
	    			owner_vals[row][column] = 0;
	    			
	    			FrameLayout view = (FrameLayout)findViewById(widget_ids[row][column]);
	    			int owner = owner_vals[row][column];
	    			IndexedButton button = new IndexedButton(this, row, column, val, owner);
	    			
	    			setUpButton(button);
	            	
	    			if( view.getChildCount() > 0 )
	    				view.removeAllViews();
	    			
	            	view.addView(button);
	            	widgets[row][column] = button;
	    		}
	    	}
	    	
	    	for( int i = 0; i < PLAYER_CARDS; i++ ){    			
    			FrameLayout view = (FrameLayout)findViewById(player_card_ids[i]);
    			
    			int val = getNextPlayerOption( i, owner );
    			IndexedButton child = new IndexedButton(this, -1, i, val, this.owner);
            	setUpButton( child );
    			if( view.getChildCount() > 0 )
    				view.removeAllViews();
    			
            	view.addView(child);
            	player_cards[i] = child;
	    	}
    	}
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

    private void setUpButton( IndexedButton button ){
//    	button.setLayoutParams(new FrameLayout.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT, 1.0f));
		button.setBackgroundResource(R.drawable.custom_button);
		button.setOnTouchListener(new TileTouchListener());
		
		button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
		
		button.updateBackground();

		button.setText(getEmoji(button.getValue()));
    }
    
    protected LinkedList<IndexedButton> getEligibleDropTargets( int value ){
    	LinkedList<IndexedButton> eligible = new LinkedList<IndexedButton>();
    	
    	synchronized( Connections.this ){
    		for( int row = 0; row < ROWS; row++ ){
    			for( int col = 0; col < COLUMNS; col++ ){
    				if( value < 0 ){
    					//TODO: handle wild cards
    				}
    				else if( gameboard[row][col] == value ){
			    		if( owner_vals[row][col] == 0 ){
			    			eligible.add(widgets[row][col]);
			    		}
			    	}
    			}
    		}
    		return eligible;
    	}
    }
	
    private int getNextPlayerOption(int column, int owner){
        int remaining = this.deck.size();
        if( remaining == 0 ){
            return -3;
        }  
        
        int index = (int) (Math.random() * remaining);
        int value = this.deck.remove(index);

        if( owner == 2 ){
            owner2_cards[column] = (int)value;
        }
        else{
            owner1_cards[column] = (int)value;
        }

        return value;
    }
    
    private void highlightTileClicked( IndexedButton button ){
    	this.owner_vals[button.getRow()][button.getColumn()] = this.owner;
    	button.setOwner(this.owner);
    	
    	for( int i = 0; i < PLAYER_CARDS; i++ ){
			IndexedButton ib = this.player_cards[i];
			if( ib.isHighlighted() ){
				int value = getNextPlayerOption( i, this.owner );
				ib.setValue(value);
				setUpButton(ib);
			}
    	}
    	
    	boolean winner = checkForWinner( this.owner, button.getRow(), button.getColumn() );
    	
    	if( winner ){
    		new AlertDialog.Builder(this)
	    	    .setTitle("You Won!")
	    	    .setMessage("Would you like to start a rematch?")
	    	    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int which) { 
	    	            //TODO: continue with rematch
	    	        }
	    	     })
	    	    .setNegativeButton("No", new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int which) { 
	    	            // do nothing
	    	        }
	    	     })
	    	     .show();
    	}
    	
    	//TODO: submit move
    	if( this.owner == 1 ){
    		this.owner = 2;
    	}
    	else{
    		this.owner = 1;
    	}
	}
    
    private void removeAllHighlighting(){
    	for( int i = 0; i < ROWS; i++ ){
    		for( int j = 0; j < COLUMNS; j++ ){
    			IndexedButton ib = this.widgets[i][j];
    			ib.setHighlighted(false);
    		}
    	}
    	
    	for( int i = 0; i < PLAYER_CARDS; i++ ){
			IndexedButton ib = this.player_cards[i];
			ib.setHighlighted(false);
    	}
    }
    
    private boolean checkForWinner( int owner, int row, int column ){
    	// check horizontal
    	int connections = 0;
    	for( int i = 0; i < COLUMNS; i++ ){
    		if( this.owner_vals[row][i] == owner ){
    			connections++;
    		}
    		else{
    			connections = 0;
    		}

    		if( connections == 5 ){
    			return true;
    		}
    	}

    	// check vertical
    	connections = 0;
    	for( int i = 0; i < ROWS; i++ ){
    		if( this.owner_vals[i][column] == owner ){
    			connections++;
    		}
    		else{
    			connections = 0;
    		}

    		if( connections == 5 ){
    			return true;
    		}
    	}

    	// check diagonal
    	return this.checkDiagonal( owner, row, column );
    }
    
    private boolean checkDiagonal( int owner, int row, int column ){
    	return this.checkDiagonal1( owner, row, column ) || this.checkDiagonal2(owner, row, column);
    }
    
    private boolean checkDiagonal1( int owner, int row, int column ){
    	// top left to bottom right (row & column increase together)
    	int connections = 0;
    	if( column <= row ){
    		for( int i = 0; i < COLUMNS; i++ ){
    			if( (row - column) + i >= ROWS ){
    				break;
    			}
    			if( this.owner_vals[(row - column) + i][i] == owner ){
    				connections++;
    			}
    			else{
    				connections = 0;
    			}

    			if( connections == 5 ){
    				return true;
    			}
    		}
    	}
    	else{ //column > row
    		for( int i = 0; i < ROWS; i++ ){
    			if( (column - row) + i >= COLUMNS ){
    				break;
    			}
    			if( this.owner_vals[i][(column - row) + i] == owner ){
    				connections++;
    			}
    			else{
    				connections = 0;
    			}

    			if( connections == 5 ){
    				return true;
    			}
    		}
    	}

    	return false;
    }
    
    private boolean checkDiagonal2( int owner, int row, int column ){
    	// bottom left to top right (column increases while row decreases)
    	int connections = 0;
    	if( column < (ROWS - 1 - row) ){
    		// we will start at column 0 and move until row 0
    		int currCol = 0;
    		int currRow = row + column;

    		while( currRow >= 0 ){
    			if( this.owner_vals[currRow][currCol] == owner ){
    				connections++;
    			}
    			else{
    				connections = 0;
    			}

    			if( connections == 5 ){
    				return true;
    			}

    			currRow--;
    			currCol++;
    		}
    	}
    	else{ //column >= ([self getSections] - 1 - row)
    		// we will start at last row and end at last column
    		int currRow = ROWS - 1;
    		int currCol = column - (ROWS - 1 - row);

    		while( currCol < COLUMNS ){
    			if( this.owner_vals[currRow][currCol] == owner ){
    				connections++;
    			}
    			else{
    				connections = 0;
    			}

    			if( connections == 5 ){
    				return true;
    			}

    			currRow--;
    			currCol++;
    		}
    	}

    	return false;
    }

    private final class TileTouchListener implements OnTouchListener 
    {
        @Override
	    public boolean onTouch(View view, MotionEvent motionEvent) {
        	if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
        		synchronized( Connections.this ){				        
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
        		
		        return true;
		    } 
        	
        	return false;
        }
    }

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
