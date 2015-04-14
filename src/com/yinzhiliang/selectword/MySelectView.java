package com.yinzhiliang.selectword;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * author yzliang
 */
public class MySelectView extends TextView {
	/** TAG */
	private static final String TAG = "WordView";
	/** 判断是否发生移动 */
	private static final float MIN_VALID_MOVE = 3f;
	/** 放大镜尺寸 */
	private static int MAGNIFIER_W = 400;
	/** 放大镜尺寸 */
	private static int MAGNIFIER_H = 200;
	/** WindowManager */
	private WindowManager mWindownManager;
	/** 放大镜LayoutParams */
	private LayoutParams mParams;
	/** 放大镜 */
	private ImageView mMagnifier;
	/** 放大镜显示 */
	private boolean mShown = false;
	/** 放大镜坐标 */
	private int mParamX;
	/** I放大镜坐标 */
	private int mParamY;
	/** 起始屏幕坐标 */
	private float mStartX = -1f;
	/** 起始屏幕坐标 */
	private float mStartY = -1f;
	/** 上次屏幕坐标 */
	private float mLastX = -1f;
	/** 上次屏幕坐标 */
	private float mLastY = -1f;
	/** 文本 */
	private CharSequence mText = "";
	/** 所有的词 */
	private List<Word> mWords;
	/** Context */
	private Context mContext;
	/** 选中的词 */
	private SpannableStringBuilder mSpan;
	/** 选中的词 */
	private Word mSelectWord;
	private Bitmap mBitmap;

	/**
	 * 构造函数
	 * 
	 * @param context
	 *            Context
	 */
	public MySelectView(Context context) {
		super(context);
		mContext = context;
		mText = getResources().getString(R.string.testText);
		init();
	}

	/**
	 * 初始化参数
	 */
	private void init() {
		setBackgroundColor(Color.WHITE);
		mSpan = new SpannableStringBuilder(mText);
		mWords = getWords(mText);
		setText(mSpan);
		setTextSize(24);
		setTextColor(Color.BLACK);
		setWillNotCacheDrawing(false);

		mWindownManager = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		mParams = new WindowManager.LayoutParams();

		mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		mParams.format = PixelFormat.RGBA_8888;
		mParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
				| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

		mParams.width = MAGNIFIER_W;
		mParams.height = MAGNIFIER_H;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		Layout layout = getLayout();
		int line = 0;
		if (mWords == null) {
			return true;
		}

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mStartX = event.getX();
			mStartY = event.getY();
			mParams.x = (int) (mStartX - MAGNIFIER_W);
			mParams.y = (int) (mStartY - MAGNIFIER_H * 4);
			mParamX = mParams.x;
			mParamY = mParams.y;
			break;
		case MotionEvent.ACTION_MOVE:
			Log.d(TAG, event.getY() + ", " + event.getX());
			if (Math.abs(event.getX() - mLastX) > MIN_VALID_MOVE
					|| Math.abs(event.getY() - mLastY) > MIN_VALID_MOVE) {
				mLastX = event.getX();
				mLastY = event.getY();
				if (!mShown) {
					showMagnifier();
				}
				line = layout.getLineForVertical(getScrollY()
						+ (int) event.getY());
				final int index = layout.getOffsetForHorizontal(line,
						(int) event.getX());

				mSelectWord = getWord(index);

				if (mSelectWord != null) {
					SpannableStringBuilder span = new SpannableStringBuilder(
							mText);
					span.setSpan(new ForegroundColorSpan(Color.BLUE),
							mSelectWord.mStart, mSelectWord.mEnd,
							Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
					setText(span);
				}

				moveMagnifier(mLastX, mLastY);
				getMagnifierContent(mLastX, mLastY);
				if (mBitmap != null && !mBitmap.isRecycled()) {
					mMagnifier.setImageBitmap(mBitmap);
				}
			}

			break;

		case MotionEvent.ACTION_UP:
			onSelectEnd();
			break;
		}

		return true;
	}

	/**
	 * 显示放大镜
	 */
	private void showMagnifier() {
		mMagnifier = new ImageView(mContext);
		mWindownManager.addView(mMagnifier, mParams);
		mShown = true;
	}

	/**
	 * 隐藏放大镜
	 */
	private void hideMagnifier() {
		if (mShown) {
			mWindownManager.removeView(mMagnifier);
			mShown = false;
		}
	}

	/**
	 * 移动放大镜
	 * 
	 * @param x
	 *            点击坐标
	 * @param y
	 *            点击坐标
	 */
	private void moveMagnifier(float x, float y) {
		int dx = (int) (x - mStartX);
		int dy = (int) (y - mStartY);
		mParams.x = mParamX + dx;
		mParams.y = mParamY + dy;

		mWindownManager.updateViewLayout(mMagnifier, mParams);
	}

	/**
	 * 获取放大镜内容
	 * 
	 * @param x
	 *            点击坐标
	 * @param y
	 *            点击坐标
	 * @return 放大镜内容
	 */
	private void getMagnifierContent(float x, float y) {
		if (mBitmap == null || mBitmap.isRecycled()) {
			mBitmap = Bitmap.createBitmap(MAGNIFIER_W, MAGNIFIER_H,
					Config.ARGB_8888);
		}

		buildDrawingCache();
		Bitmap drawingCache = getDrawingCache();
		if (drawingCache == null) {
			return;
		}

		float offsetX = x * 0.6f;
		float offsetY = y * 0.6f;
		Paint p = new Paint();
		Canvas c = new Canvas(mBitmap);
		c.scale(1.2f, 1.2f);

		c.drawBitmap(drawingCache, -offsetX, -offsetY, p);

		destroyDrawingCache();
	}

	/**
	 * 点击结束
	 */
	private void onSelectEnd() {
		mLastX = -1f;
		mLastY = -1f;
		hideMagnifier();
		setText(mSpan);
		if (mSelectWord != null) {
			Toast.makeText(
					mContext,
					mText.subSequence(mSelectWord.getStart(),
							mSelectWord.getEnd()), Toast.LENGTH_LONG).show();
			mSelectWord = null;
		}
		if (mBitmap != null && !mBitmap.isRecycled()) {
			mBitmap.recycle();
			mBitmap = null;
		}
	}

	/**
	 * 获取单词
	 * 
	 * @param index
	 *            点击处索引
	 * @return 命中的词
	 */
	private Word getWord(final int index) {
		if (mWords == null) {
			return null;
		}

		for (Word w : mWords) {
			if (w.isHit(index)) {
				return w;
			}
		}

		return null;
	}

	/**
	 * 从字符串中获取所有单词
	 * 
	 * @param s
	 *            字符串
	 * @return 单词list
	 */
	public List<Word> getWords(CharSequence s) {
		if (s == null) {
			return null;
		}

		List<Word> result = new ArrayList<Word>();

		int start = -1;

		int i = 0;

		for (; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c == ' ' || !Character.isLetter(c)) {
				if (start != -1) {
					result.add(new Word(start, i));// From ( 0, 4 )
				}
				start = -1;
			} else {
				if (start == -1) {
					start = i;
				}
			}

		}

		if (start != -1) {
			result.add(new Word(start, i));
		}

		Log.d(TAG, result.toString());

		return result;

	}

	private class Word {
		/** 起始位置 */
		private int mStart;
		/** 结束位置 */
		private int mEnd;

		/**
		 * 构造函数
		 * 
		 * @param start
		 *            起始位置
		 * @param end
		 *            结束位置
		 */
		public Word(final int start, final int end) {
			this.mStart = start;
			this.mEnd = end;
		}

		/**
		 * 获取起始位置
		 * 
		 * @return 起始位置
		 */
		public int getStart() {
			return this.mStart;
		}

		/**
		 * 获取结束位置
		 * 
		 * @return 结束位置
		 */
		public int getEnd() {
			return this.mEnd;
		}

		/**
		 * 是否命中
		 * 
		 * @param index
		 *            索引
		 * @return 是否命中
		 */
		public boolean isHit(final int index) {
			if (index >= getStart() && index <= getEnd()) {
				return true;
			}

			return false;
		}
	}
}
