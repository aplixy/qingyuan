package com.example.listviewimpl.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Adapter;
import android.widget.AdapterView;

public class Copy_2_of_MyListView extends AdapterView<Adapter> {

	static final int TOUCH_MODE_DOWN = 0;

	static final int TOUCH_MODE_SCROLL = 1;

	private final int LAYOUT_MODE_BELOW = -1;
	private final int LAYOUT_MODE_ABOVE = 0;

	private int mTouchSlop;
	private Adapter mAdapter;

	private int mLastItemPosition;
	private int mFirstItemPosition;

	private Motion mMotion = new Motion();
	private Runnable mLongPressRunnable;
	private int mTouchMode;
	private int mScrolledDistance;

	final class Motion {
		float mY;
		float mX;
		float mPointY;
	}

	public Copy_2_of_MyListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public Copy_2_of_MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public Copy_2_of_MyListView(Context context) {
		super(context);
		init();
	}

	private void init() {
		final ViewConfiguration configuration = ViewConfiguration.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
	}

	@Override
	public Adapter getAdapter() {
		return mAdapter;
	}

	@Override
	public void setAdapter(Adapter adapter) {
		mAdapter = adapter;
	}

	@Override
	public View getSelectedView() {
		return null;
	}

	@Override
	public void setSelection(int position) {
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (mAdapter == null)
			return;

		if (getChildCount() == 0) {
			mLastItemPosition = 0;
			fillListDown(0, 0);
		} else {
			removeNonVisibleViews(mScrolledDistance);
			fillList(mScrolledDistance);
		}
		positioinItems(left);
		invalidate();
	}

	private void removeNonVisibleViews(int offset) {
		if (mLastItemPosition < mAdapter.getCount()) {
			View removeChild = getChildAt(0);
			while (removeChild != null && removeChild.getBottom() + offset < 0) {
				removeViewInLayout(removeChild);
				mFirstItemPosition++;
				removeChild = getChildAt(0);
			}
		}
	}

	private void fillList(int offset) {
		fillListDown(getChildAt(getChildCount() - 1).getBottom(), offset);

		fillListUp(getChildAt(0).getTop(), offset);
	}

	private void fillListUp(int topEdge, int offset) {
		while (topEdge + offset > 0 && mFirstItemPosition > 0) {
			mFirstItemPosition--;
			View newTopChild = mAdapter.getView(mFirstItemPosition, null, this);
			addAndMeasureChild(newTopChild, LAYOUT_MODE_ABOVE);
			int childHeight = newTopChild.getMeasuredHeight();
			topEdge -= childHeight;
		}
	}

	private void fillListDown(int BottomEdge, int offset) {
		for (; BottomEdge + offset < getHeight() && mLastItemPosition < mAdapter.getCount(); mLastItemPosition++) {
			View newBottomChild = mAdapter.getView(mLastItemPosition, null, this);
			addAndMeasureChild(newBottomChild, LAYOUT_MODE_BELOW);
			BottomEdge += newBottomChild.getMeasuredHeight();
		}
	}

	private void addAndMeasureChild(View child, int layoutMode) {
		LayoutParams params = child.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}
		addViewInLayout(child, layoutMode, params, true);
		child.measure(MeasureSpec.EXACTLY | getWidth(), MeasureSpec.UNSPECIFIED);
	}

	private void positioinItems(int parentLeft) {
		int top = getChildAt(0).getTop() + mScrolledDistance;

		for (int i = 0; i < getChildCount(); i++) {
			final View child = getChildAt(i);
			final int width = child.getMeasuredWidth();
			final int height = child.getMeasuredHeight();

			child.layout(parentLeft, top, parentLeft + width, top + height);
			top += height;
		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (!isAttachedToWindow()) {
			return false;
		}
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mTouchMode = TOUCH_MODE_DOWN;
			mMotion.mX = ev.getX();
			mMotion.mY = ev.getY();
			mMotion.mPointY = ev.getY();
			startLongPressCheck();
			break;

		case MotionEvent.ACTION_MOVE:
			return startScrollIfNeeded(ev.getX(), ev.getY());

		default:
			removeCallbacks(mLongPressRunnable);
		}
		return false;
	}

	private boolean startScrollIfNeeded(float x, float y) {
		final float deltaY = Math.abs(y - mMotion.mY);
		final float deltaX = Math.abs(x - mMotion.mX);
		if (deltaY > mTouchSlop || deltaX > mTouchSlop) {
			removeCallbacks(mLongPressRunnable);
			if (deltaY > deltaX)
				return true;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (getChildCount() == 0)
			return super.onTouchEvent(ev);

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			onTouchDown(ev);
			break;

		case MotionEvent.ACTION_MOVE:
			onTouchMove(ev);
			break;

		case MotionEvent.ACTION_UP:
			onTouchUp(ev);
			break;
		}

		return true;
	}

	private void onTouchUp(MotionEvent ev) {
		removeCallbacks(mLongPressRunnable);
	}

	private void onTouchMove(MotionEvent ev) {
		if (startScrollIfNeeded(ev.getX(), ev.getY())) {
			mTouchMode = TOUCH_MODE_SCROLL;
			mScrolledDistance = (int) (ev.getY() - mMotion.mPointY);
			requestLayout();
		}
		mMotion.mPointY = ev.getY();
	}

	private void onTouchDown(MotionEvent ev) {
		mMotion.mPointY = ev.getY();
	}

	/**
	 * 开启异步线程，条件允许时调用LongClickListener
	 */
	private void startLongPressCheck() {
		// 创建子线程
		if (mLongPressRunnable == null) {
			mLongPressRunnable = new Runnable() {

				@Override
				public void run() {
					if (mTouchMode == TOUCH_MODE_DOWN) {
						final int index = getContainingChildIndex((int) mMotion.mX, (int) mMotion.mY);
						if (index != INVALID_POSITION) {
							longClickChild(index);
						}
					}
				}
			};
		}
		postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
	}

	private void longClickChild(int index) {
		View itemView = getChildAt(index);
		int position = mFirstItemPosition + index;
		long id = mAdapter.getItemId(position);
		OnItemLongClickListener listener = getOnItemLongClickListener();

		if (listener != null) {
			listener.onItemLongClick(this, itemView, position, id);
		}
	}

	private int getContainingChildIndex(int x, int y) {
		final Rect mRect = new Rect();
		// 遍历当前ListView所有Item
		for (int i = 0; i < getChildCount(); i++) {
			getChildAt(i).getHitRect(mRect);
			// x,y是否在当前视图区域内
			if (mRect.contains(x, y)) {
				return i;
			}
		}

		return INVALID_POSITION;
	}
}
