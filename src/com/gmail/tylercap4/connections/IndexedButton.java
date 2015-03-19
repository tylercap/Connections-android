package com.gmail.tylercap4.connections;

import android.content.Context;
import android.widget.Button;

public class IndexedButton extends Button {
	private final int row;
	private final int column;
	private final int value;
	private boolean highlighted;

	public IndexedButton(Context context, int row, int column, int value) {
		super(context);
		
		this.row = row;
		this.column = column;
		this.value = value;
		this.highlighted = false;
	}
	
	public void setHighlighted(){
		this.highlighted = !this.highlighted;
		
		if( this.highlighted ){
			setBackgroundResource(R.drawable.highlighted);
		}
		else{
			setBackgroundResource(R.drawable.custom_button);
		}
	}
	
	public void isHighlighted(){
		
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
