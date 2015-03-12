package com.gmail.tylercap4.connections;

import android.content.Context;
import android.widget.Button;

public class IndexedButton extends Button {
	private final int row;
	private final int column;

	public IndexedButton(Context context, int row, int column) {
		super(context);
		
		this.row = row;
		this.column = column;
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
