package com.gmail.tylercap4.connections;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;

public class Model 
{
	public static final Map<String,Model> ID_TO_MODEL_MAP = new HashMap<String,Model>();
	public static final String ID_TAG = "MATCH_ID";
	
	public static final int ROWS = 9;
	public static final int COLUMNS = 8;
	public static final int PLAYER_CARDS = 6;
    
	private final String myId;
	private TurnBasedMatch match;
	
	private int [][] 			gameboard;
	private int [][] 			owner_vals;
	
	private LinkedList<Integer> deck;
	private int[]				owner1_cards;
	private int[]				owner2_cards;
	
	private int owners_turn;

	public Model( TurnBasedMatch match, String myId ){
		this.myId = myId;
		if( match == null )
			return;
		
		ID_TO_MODEL_MAP.put(match.getMatchId(), this);
		this.match = match;
		
		if( match == null || match.getData() == null || match.getData().length == 0 ){			
			gameboard = new int[ROWS][COLUMNS];
			owner_vals = new int[ROWS][COLUMNS];
			
			owner1_cards = new int[PLAYER_CARDS];
			owner2_cards = new int[PLAYER_CARDS];
			
			doNewGame();
		}
		else{
			loadFromMatch();
		}
	}
	
	private void loadFromMatch(){
		//TODO: load the pertinent data from the byte array
		byte[] data = this.match.getData();
		
		try
        {
			String decoded = new String(data, "UTF-8");
			
			int start = decoded.indexOf('[');
			int end = decoded.indexOf(']', start) + 1;
			String string = decoded.substring(start, end);
			this.gameboard = decode2DArray(string);
			
			start = decoded.indexOf('[', end);
			end = decoded.indexOf(']', start) + 1;
			string = decoded.substring(start, end);
			this.owner_vals = decode2DArray(string);
		    
			start = decoded.indexOf('[', end);
			end = decoded.indexOf(']', start) + 1;
			string = decoded.substring(start, end);
			this.owner1_cards = decode1DArray(string);

			start = decoded.indexOf('[', end);
			end = decoded.indexOf(']', start) + 1;
			string = decoded.substring(start, end);
			this.owner2_cards = decode1DArray(string);
		    
			start = decoded.indexOf('[', end);
			end = decoded.indexOf(']', start) + 1;
			string = decoded.substring(start, end);
			this.deck = decodeArrayToList(string);
		    
			start = decoded.indexOf('(', end);
			end = decoded.indexOf(')', start);
			string = decoded.substring(start + 1, end);
			this.owners_turn = Integer.parseInt(string);
        } catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
		}
	}
	
	public byte[] storeToData(){
		//TODO: store all pertinent data properly to a byte array
		StringBuffer buffer = new StringBuffer();
		buffer.append(getGameboardString());
		buffer.append(getOwnersString());
		buffer.append(getP1CardsString());
		buffer.append(getP2CardsString());
		buffer.append(getDeckString());
		buffer.append("("+this.owners_turn+")");

		byte[] data = null;
		try {
			data = buffer.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
		}
		return data;
	}

	private int[][] decode2DArray(String string){
		// Will be ROWS x COLUMNS
		int[][] array = new int[ROWS][COLUMNS];
		
		int i = 0;
		int j = 0;
		int start = string.indexOf('[') + 1;
		int end = string.indexOf(',', start);
	    while( end > 0 ){
	    	String value = string.substring(start, end);
	    	array[i][j] = Integer.parseInt(value);
	    	string = string.substring(end + 1);
	    	j++;
	        
	        if( !string.contains(",") ){
	            end = -1;
	            break;
	        }
	        
	        start = 0;
	        end = string.indexOf(',');
	        
	        int semCol = string.indexOf(';');
	        
	        if( semCol > 0 && semCol < end ){
	        	value = string.substring(start, semCol);
		    	array[i][j] = Integer.parseInt(value);
		    	string = string.substring(semCol + 1);
	            
	            start = 0;
	            end = string.indexOf(',');
	            
	            i++;
	            j = 0;
	        }
	    }
	    
	    end = string.indexOf(']');
	    String value = string.substring(start, end);
    	array[i][j] = Integer.parseInt(value);
	    
	    return array;
	}
	
	private int[] decode1DArray(String string){
		// Will have a length of PLAYER_CARDS
		int[] array = new int[PLAYER_CARDS];
		
		int j = 0;
		int start = string.indexOf('[') + 1;
		int end = string.indexOf(',', start);
	    while( end > 0 ){
	    	String value = string.substring(start, end);
	    	array[j] = Integer.parseInt(value);
	    	string = string.substring(end + 1);
	    	j++;
	        
	        if( !string.contains(",") ){
	            end = -1;
	            break;
	        }
	        
	        start = 0;
	        end = string.indexOf(',');
	    }
	    
	    end = string.indexOf(']');
	    String value = string.substring(start, end);
    	array[j] = Integer.parseInt(value);
	    
	    return array;
	}
	
	private LinkedList<Integer> decodeArrayToList(String string){
		LinkedList<Integer> list = new LinkedList<Integer>();
		int start = string.indexOf('[') + 1;
		int end = string.indexOf(',', start);
	    while( end > 0 ){
	    	String value = string.substring(start, end);
	    	list.add( Integer.parseInt(value) );
	    	string = string.substring(end + 1);
	        
	        if( !string.contains(",") ){
	            end = -1;
	            break;
	        }
	        
	        start = 0;
	        end = string.indexOf(',');
	    }
	    
	    end = string.indexOf(']');
	    String value = string.substring(start, end);
	    list.add( Integer.parseInt(value) );
	    
	    return list;
	}
	
	private String getGameboardString(){
		StringBuffer buffer = new StringBuffer('[');
		for( int i=0; i<ROWS; i++ ){
	        for( int j=0; j<COLUMNS; j++ ){
	            int value = this.gameboard[i][j];
	            buffer.append(value);
	            if( j < COLUMNS - 1 ){
	            	buffer.append(',');
	            }
	        }
	        if( i < ROWS - 1 ){
	            buffer.append(';');
	        }
	    }
		buffer.append(']');
		
		return buffer.toString();
	}
	
	private String getOwnersString(){
		StringBuffer buffer = new StringBuffer('[');
		for( int i=0; i<ROWS; i++ ){
	        for( int j=0; j<COLUMNS; j++ ){
	            int value = this.owner_vals[i][j];
	            buffer.append(value);
	            if( j < COLUMNS - 1 ){
	            	buffer.append(',');
	            }
	        }
	        if( i < ROWS - 1 ){
	            buffer.append(';');
	        }
	    }
		buffer.append(']');
		
		return buffer.toString();
	}
	
	private String getP1CardsString(){
		StringBuffer buffer = new StringBuffer('[');
		for( int i=0; i<PLAYER_CARDS; i++ ){   
			int value = this.owner1_cards[i];
	        buffer.append(value);
	        if( i < PLAYER_CARDS - 1 ){
	        	buffer.append(',');
	        }
	    }
	         
		buffer.append(']');
		
		return buffer.toString();
	}
	
	private String getP2CardsString(){
		StringBuffer buffer = new StringBuffer('[');
		for( int i=0; i<PLAYER_CARDS; i++ ){   
			int value = this.owner2_cards[i];
	        buffer.append(value);
	        if( i < PLAYER_CARDS - 1 ){
	        	buffer.append(',');
	        }
	    }
	         
		buffer.append(']');
		
		return buffer.toString();
	}
	
	private String getDeckString(){
		StringBuffer buffer = new StringBuffer('[');
		int i = 1;
		for( Integer value:this.deck ){   
	        buffer.append(value);
	        if( i < this.deck.size() ){
	        	buffer.append(',');
	        }
	        i++;
	    }
	         
		buffer.append(']');
		
		return buffer.toString();
	}
	
	private void doNewGame(){
		if( match.getParticipants().get(0).equals(this.myId) ){
			this.owners_turn = 1;
		}
		else{
			this.owners_turn = 2;
		}
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
	    		}
	    	}
	    	
	    	for( int owner = 1; owner <= 2; owner++ ){
		    	for( int i = 0; i < PLAYER_CARDS; i++ ){    			
	    			getNextPlayerOption( i, owner );
		    	}
	    	}
    	}
    }
	
	public TurnBasedMatch getMatch(){
		return this.match;
	}
	
	public static Participant getOpponentFromMatch(TurnBasedMatch match){
		//TODO: get opponent
		return match.getParticipants().get(1);
	}
	
//	-(GPGTurnBasedParticipant*)getOpponentFromMatch:(GPGTurnBasedMatch*)match
//	{
//	    NSArray *participants = match.participants;
//	    GPGTurnBasedParticipant *opponent = [participants objectAtIndex:0];
//	    
//	    NSString *myId = match.localParticipantId;
//	    if([myId isEqualToString:opponent.participantId] && [participants count] > 1){
//	        opponent = [participants objectAtIndex:1];
//	    }
//	    
//	    return opponent;
//	}
	
    public int getNextPlayerOption(int column, int owner){
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
    
    public int getPlayerCardAt( int owner, int column ){
    	if( owner == 1 ){
    		return this.owner1_cards[column];
    	}
    	else{
    		return this.owner2_cards[column];
    	}
    }
    
    public void setOwnerAt( int owner, int row, int column ){
    	this.owner_vals[row][column] = owner;
    }
    
    public int getOwnerAt( int row, int column ){
    	return this.owner_vals[row][column];
    }
    
    public int getValueAt( int row, int column ){
    	return this.gameboard[row][column];
    }
    
    public boolean checkForWinner( int owner, int row, int column ){
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
}
