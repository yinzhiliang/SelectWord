package com.yinzhiliang.selectword;

import android.app.Activity;
import android.os.Bundle;

/**
 * @author yzliang
 */
public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(new MySelectView(this));
	}
}
