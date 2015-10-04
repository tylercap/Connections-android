package com.gmail.tylercap4.connections;

import android.content.Context;
import android.widget.Button;

public class IndexedButton extends Button {
	private final int row;
	private final int column;
	private int value;
	
	private int cardOwner;
	private int deviceOwner;
	private boolean highlighted;

	public IndexedButton(Context context, int row, int column, int value, int cardOwner, int deviceOwner) {
		super(context);
		
		this.row = row;
		this.column = column;
		this.value = value;
		this.cardOwner = cardOwner;
		this.deviceOwner = deviceOwner;
		this.highlighted = false;
	}
	
	public void updateBackground(){
		if( this.highlighted ){
			setBackgroundResource(R.drawable.highlighted);
		}
		else if( this.cardOwner == 1 || this.cardOwner == 2 ){
			if( this.cardOwner == this.deviceOwner ){
				setBackgroundResource(R.drawable.player1);				
			}
			else{
				setBackgroundResource(R.drawable.player2);
			}
		}
		else{
			setBackgroundResource(R.drawable.custom_button);
		}		
//		switch( owner ){
//			case 1:
//				button.setBackgroundResource(R.drawable.player1);
//				break;
//			case 2:
//				button.setBackgroundResource(R.drawable.player2);
//				break;
//			default:
//				button.setBackgroundResource(R.drawable.custom_button);
//				break;
//		}
	}
	
	public void setValue( int value ){
		this.value = value;
	}
	
	public void setCardOwner( int cardOwner ){
		this.cardOwner = cardOwner;
		this.updateBackground();
	}
	
	public void setHighlighted( boolean highlighted ){
		this.highlighted = highlighted;
		updateBackground();
	}
	
	public boolean isHighlighted(){
		return this.highlighted;
	}
	
	public int getValue() {
		return this.value;
	}

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}
	
	@Override
	public boolean performClick(){
		super.performClick();
		return true;
	}
}
