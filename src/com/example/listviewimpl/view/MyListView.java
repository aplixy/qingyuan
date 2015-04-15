package com.example.listviewimpl.view;

import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.OverScroller;

public class MyListView extends AdapterView<Adapter> {

	static final int TOUCH_MODE_REST = -1;

	static final int TOUCH_MODE_DOWN = 0;

	static final int TOUCH_MODE_SCROLL = 1;

	static final int TOUCH_MODE_FLING = 2;

	static final int TOUCH_MODE_OVERSCROLL = 3;

	private static final int INVALID_POINTER = -1;

	private static final float SCROLL_RATIO = 0.35f;// 阻尼系数

	private final int LAYOUT_MODE_BELOW = -1;
	private final int LAYOUT_MODE_ABOVE = 0;

	private int mTouchSlop;
	private Adapter mAdapter;
	private float mVelocityScale = 1.0f;

	private int mLastItemPosition;
	private int mFirstItemPosition;

	private Motion mMotion = new Motion();
	private Runnable mLongPressRunnable;
	private int mTouchMode;
	private VelocityTracker mVelocityTracker;

	private int mMinimumVelocity;

	private int mMaximumVelocity;

	private int mActivePointerId;

	private FlingRunnable mFlingRunnable;

	private final LinkedList<View> mCachedItemViews = new LinkedList<View>();

	private int mPageBottomEdge;

	private boolean isInLayoutTop;
	private boolean isInlayoutBottom;
	public boolean mDataChanged;

	private DataSetObserver mDataSetObserver;

	final class Motion {
		float mY;
		float mX;
		float mPointY;
	}

	public MyListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MyListView(Context context) {
		super(context);
		init();
	}

	private void init() {
		ViewConfiguration configuration = ViewConfiguration.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}

	@Override
	public Adapter getAdapter() {
		return mAdapter;
	}

	@Override
	public void setAdapter(Adapter adapter) {
		if (mAdapter == adapter)
			return;

		if (mAdapter != null && mDataSetObserver != null) {
			mAdapter.unregisterDataSetObserver(mDataSetObserver);
		}

		mAdapter = adapter;
		if (mDataSetObserver == null) {
			mDataSetObserver = new AdapterDataSetObserver();
		}
		mAdapter.registerDataSetObserver(mDataSetObserver);

		removeAllViewsInLayout();
		mCachedItemViews.clear();
		if (mFlingRunnable != null)
			mFlingRunnable.endFling();
		requestLayout();
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
		if (getChildCount() == 0 && mAdapter != null) {
			initList(left);
		}

		if (mDataChanged) {
			refreshList(left);
		}
	}

	private boolean trackMotionScroll(final float scrolledDistance) {
		if (scrolledDistance == 0)
			return false;

		if ((scrolledDistance > 0 && isInLayoutTop) || (scrolledDistance < 0 && isInlayoutBottom))
			return true;

		int left = getLeft();
		if (scrolledDistance > 0) {
			removeBottomNonVisibleViews(scrolledDistance);
			fillListUp(getChildAt(0).getTop(), scrolledDistance, left);
			layoutPositionItemsDown(left, scrolledDistance);
		} else {
			removeTopNonVisibleViews(scrolledDistance);
			fillListDown(getChildAt(getChildCount() - 1).getBottom(), scrolledDistance, left);
			layoutPositionItemsUp(left, scrolledDistance);
		}
		invalidate();
		return false;
	}

	private void removeTopNonVisibleViews(float offset) {
		int t_lastPosition = mLastItemPosition;
		View removeChild = getChildAt(0);
		while (t_lastPosition < mAdapter.getCount() && removeChild != null && removeChild.getBottom() + offset < 0) {
			removeViewInLayout(removeChild);
			mCachedItemViews.addLast(removeChild);
			mFirstItemPosition++;
			t_lastPosition++;
			removeChild = getChildAt(0);
		}
	}

	private void removeBottomNonVisibleViews(float offset) {
		int t_firstPosition = mFirstItemPosition;
		View removeChild = getChildAt(getChildCount() - 1);
		while (t_firstPosition > 0 && removeChild != null && removeChild.getTop() + offset > getHeight()) {
			removeViewInLayout(removeChild);
			mCachedItemViews.addLast(removeChild);
			mLastItemPosition--;
			t_firstPosition--;
			removeChild = getChildAt(getChildCount() - 1);
		}
	}

	private void fillListUp(int topEdge, float offset, int parentLeft) {
		View cachedView;
		View newTopChild = null;
		while (topEdge + offset > 0 && mFirstItemPosition > 0) {
			mFirstItemPosition--;
			cachedView = getCachedView();
			newTopChild = mAdapter.getView(mFirstItemPosition, cachedView, this);
			addAndMeasureChild(newTopChild, LAYOUT_MODE_ABOVE, cachedView == newTopChild);
			topEdge -= newTopChild.getMeasuredHeight();
		}
		if (newTopChild != null) {
			int width = newTopChild.getMeasuredWidth();
			int height = newTopChild.getMeasuredHeight();
			newTopChild.layout(parentLeft, -height, parentLeft + width, 0);
		}
	}

	private void fillListDown(int bottomEdge, float offset, int parentLeft) {
		View cachedView;
		View newBottomChild = null;
		while (bottomEdge + offset < mPageBottomEdge && mLastItemPosition < mAdapter.getCount()) {
			cachedView = getCachedView();
			newBottomChild = mAdapter.getView(mLastItemPosition, cachedView, this);
			addAndMeasureChild(newBottomChild, LAYOUT_MODE_BELOW, cachedView == newBottomChild);
			mLastItemPosition++;
			bottomEdge += newBottomChild.getMeasuredHeight();
		}
		if (newBottomChild != null) {
			int width = newBottomChild.getMeasuredWidth();
			int height = newBottomChild.getMeasuredHeight();
			newBottomChild.layout(parentLeft, mPageBottomEdge, parentLeft + width, mPageBottomEdge + height);
		}
	}

	private View getCachedView() {
		if (mCachedItemViews.size() != 0) {
			return mCachedItemViews.removeFirst();
		}
		return null;
	}

	private void initList(int parentLeft) {
		mDataChanged = false;
		mLastItemPosition = 0;
		mFirstItemPosition = 0;
		isInLayoutTop = false;
		isInlayoutBottom = false;
		scrollTo(0, 0);
		int bottomEdge = 0;
		for (; bottomEdge < getHeight() && mLastItemPosition < mAdapter.getCount(); mLastItemPosition++) {
			View newBottomChild = mAdapter.getView(mLastItemPosition, null, this);
			addAndMeasureChild(newBottomChild, LAYOUT_MODE_BELOW, false);
			int width = newBottomChild.getMeasuredWidth();
			int height = newBottomChild.getMeasuredHeight();
			newBottomChild.layout(parentLeft, bottomEdge, parentLeft + width, bottomEdge + height);
			bottomEdge += height;
		}
		mPageBottomEdge = bottomEdge < getHeight() ? bottomEdge : getHeight();
	}

	private void refreshList(int parentLeft) {
		mDataChanged = false;
		int t_count = getChildCount();
		final View[] tem = new View[t_count];
		for (int i = 0; i < t_count; i++) {
			tem[i] = getChildAt(i);
		}
		int bottomEdge = tem[0].getTop();
		removeAllViewsInLayout();

		for (int t_Position = 0; t_Position < t_count; t_Position++) {
			View oldView = tem[t_Position];
			View newBottomChild = mAdapter.getView(mFirstItemPosition + t_Position, oldView, this);
			addAndMeasureChild(newBottomChild, LAYOUT_MODE_BELOW, oldView == newBottomChild);
			int width = newBottomChild.getMeasuredWidth();
			int height = newBottomChild.getMeasuredHeight();
			newBottomChild.layout(parentLeft, bottomEdge, parentLeft + width, bottomEdge + height);
			bottomEdge += height;
		}

		for (; bottomEdge < getHeight() && mLastItemPosition < mAdapter.getCount(); mLastItemPosition++) {
			View oldView = getCachedView();
			View newBottomChild = mAdapter.getView(mLastItemPosition, oldView, this);
			addAndMeasureChild(newBottomChild, LAYOUT_MODE_BELOW, oldView == newBottomChild);
			int width = newBottomChild.getMeasuredWidth();
			int height = newBottomChild.getMeasuredHeight();
			newBottomChild.layout(parentLeft, bottomEdge, parentLeft + width, bottomEdge + height);
			bottomEdge += height;
		}
		if (bottomEdge < getHeight()) {
			mPageBottomEdge = bottomEdge;
		} else {
			mPageBottomEdge = getHeight();
			if (isInlayoutBottom) {
				int scrolledDistance = -getScrollY();
				isInlayoutBottom = false;
				scrollTo(0, 0);
				trackMotionScroll(scrolledDistance);
			}
		}
	}

	private void addAndMeasureChild(View child, int layoutMode, boolean b) {
		LayoutParams params = child.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}
		addViewInLayout(child, layoutMode, params, true);
		if (!b)
			child.measure(MeasureSpec.EXACTLY | getWidth(), MeasureSpec.UNSPECIFIED);
	}

	private void layoutPositionItemsUp(int parentLeft, float scrolledDistance) {
		int top = (int) (getChildAt(0).getTop() + scrolledDistance);

		if (mLastItemPosition == mAdapter.getCount()) {
			float bottom = getChildAt(getChildCount() - 1).getBottom() + scrolledDistance;
			if (bottom < mPageBottomEdge) {
				layoutPositionItemsBottom(parentLeft);
				isInLayoutTop = false;
				isInlayoutBottom = true;
				return;
			}
		}
		isInLayoutTop = false;
		isInlayoutBottom = false;

		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			int width = child.getMeasuredWidth();
			int height = child.getMeasuredHeight();

			child.layout(parentLeft, top, parentLeft + width, top + height);
			top += height;
		}
	}

	private void layoutPositionItemsDown(int parentLeft, float scrolledDistance) {
		int tCount = getChildCount() - 1;
		int bottom = (int) (getChildAt(tCount).getBottom() + scrolledDistance);

		if (mFirstItemPosition == 0) {
			float top = getChildAt(0).getTop() + scrolledDistance;
			if (top > 0) {
				layoutPositionItemsTop(parentLeft);
				isInLayoutTop = true;
				isInlayoutBottom = false;
				return;
			}
		}
		isInLayoutTop = false;
		isInlayoutBottom = false;

		for (int i = tCount; i >= 0; i--) {
			View child = getChildAt(i);
			int width = child.getMeasuredWidth();
			int height = child.getMeasuredHeight();

			child.layout(parentLeft, bottom - height, parentLeft + width, bottom);
			bottom -= height;
		}
	}

	private void layoutPositionItemsTop(int parentLeft) {
		int top = 0;

		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			int width = child.getMeasuredWidth();
			int height = child.getMeasuredHeight();

			child.layout(parentLeft, top, parentLeft + width, top + height);
			top += height;
		}
	}

	private void layoutPositionItemsBottom(int parentLeft) {
		int tCount = getChildCount() - 1;
		int bottom = mPageBottomEdge;

		for (int i = tCount; i >= 0; i--) {
			View child = getChildAt(i);
			int width = child.getMeasuredWidth();
			int height = child.getMeasuredHeight();

			child.layout(parentLeft, bottom - height, parentLeft + width, bottom);
			bottom -= height;
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
			mActivePointerId = ev.getPointerId(0);
			startLongPressCheck();
			initOrResetVelocityTracker();
			mVelocityTracker.addMovement(ev);
			scrollTo(0, 0);
			break;

		case MotionEvent.ACTION_MOVE:
			int pointerIndex = ev.findPointerIndex(mActivePointerId);
			if (pointerIndex == -1) {
				pointerIndex = 0;
				mActivePointerId = ev.getPointerId(pointerIndex);
			}
			initVelocityTrackerIfNotExists();
			mVelocityTracker.addMovement(ev);
			return startScrollIfNeeded(ev.getX(), ev.getY());

		case MotionEvent.ACTION_UP:
			mActivePointerId = INVALID_POINTER;
			recycleVelocityTracker();
			break;

		default:
			removeCallbacks(mLongPressRunnable);
		}
		return false;
	}

	private void recycleVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	private boolean startScrollIfNeeded(float x, float y) {
		float deltaY = Math.abs(y - mMotion.mY);
		float deltaX = Math.abs(x - mMotion.mX);
		if (deltaY > mTouchSlop || deltaX > mTouchSlop) {
			removeCallbacks(mLongPressRunnable);
			if (deltaY > deltaX) {
				mTouchMode = TOUCH_MODE_SCROLL;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (getChildCount() == 0)
			return super.onTouchEvent(ev);

		initVelocityTrackerIfNotExists();
		mVelocityTracker.addMovement(ev);

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
		switch (mTouchMode) {
		case TOUCH_MODE_SCROLL:
			final int childCount = getChildCount();
			if (childCount > 0) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

				final int initialVelocity = (int) (velocityTracker.getYVelocity(mActivePointerId) * mVelocityScale);

				if (Math.abs(initialVelocity) > mMinimumVelocity) {
					if (mFlingRunnable == null) {
						mFlingRunnable = new FlingRunnable();
					}
					mFlingRunnable.start(-initialVelocity);
				} else {
					mTouchMode = TOUCH_MODE_REST;
					if (mFlingRunnable != null) {
						mFlingRunnable.endFling();
					}
				}

			} else {
				mTouchMode = TOUCH_MODE_REST;
			}
			mActivePointerId = INVALID_POINTER;
			break;
		case TOUCH_MODE_OVERSCROLL:
			int scrollY = getScrollY();
			if (scrollY != 0) {
				if (mFlingRunnable == null) {
					mFlingRunnable = new FlingRunnable();
				}
				mFlingRunnable.startScroll(0, scrollY, 0, -scrollY);
			}
			break;
		case TOUCH_MODE_DOWN:
			if (mTouchMode == TOUCH_MODE_DOWN) {
				clickChildAt((int) ev.getX(), (int) ev.getY());
			}
			break;
		}
		mActivePointerId = INVALID_POINTER;
		recycleVelocityTracker();
	}

	private void onTouchMove(MotionEvent ev) {
		int pointerIndex = ev.findPointerIndex(mActivePointerId);
		if (pointerIndex == -1) {
			pointerIndex = 0;
			mActivePointerId = ev.getPointerId(pointerIndex);
		}
		float scrolledDistance = ev.getY() - mMotion.mPointY;
		switch (mTouchMode) {
		case TOUCH_MODE_DOWN:
		case TOUCH_MODE_REST:
			startScrollIfNeeded(ev.getX(), ev.getY());
			break;
		case TOUCH_MODE_SCROLL:
			if (trackMotionScroll(scrolledDistance))
				mTouchMode = TOUCH_MODE_OVERSCROLL;
			break;
		case TOUCH_MODE_OVERSCROLL:
			if (scrolledDistance == 0)
				break;

			scrolledDistance = -scrolledDistance;

			float newScrollY = getScrollY() + scrolledDistance;
			if (isInLayoutTop) {
				if (newScrollY > 0) {
					scrollTo(0, 0);
					mTouchMode = TOUCH_MODE_SCROLL;
					break;
				}
				final int sY = (int) (scrolledDistance * (scrolledDistance > 0 ? 1 : SCROLL_RATIO));
				scrollBy(0, sY);
			} else if (isInlayoutBottom) {
				if (newScrollY < 0) {
					scrollTo(0, 0);
					mTouchMode = TOUCH_MODE_SCROLL;
					break;
				}
				final int sY = (int) (scrolledDistance * (scrolledDistance < 0 ? 1 : SCROLL_RATIO));
				scrollBy(0, sY);
			} else {
				mTouchMode = TOUCH_MODE_SCROLL;
			}
			break;
		}
		mMotion.mPointY = ev.getY();
	}

	private void onTouchDown(MotionEvent ev) {
		mActivePointerId = ev.getPointerId(0);
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
						int index = getContainingChildIndex((int) mMotion.mX, (int) mMotion.mY);
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
		Rect mRect = new Rect();
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

	private void initOrResetVelocityTracker() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		} else {
			mVelocityTracker.clear();
		}
	}

	private void initVelocityTrackerIfNotExists() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
	}

	private class FlingRunnable implements Runnable {
		private final OverScroller mScroller;

		private int mLastFlingY;

		private final Runnable mCheckFlywheel = new Runnable() {
			@Override
			public void run() {
			}
		};

		private static final int FLYWHEEL_TIMEOUT = 40; // milliseconds

		FlingRunnable() {
			mScroller = new OverScroller(getContext());
		}

		void start(int initialVelocity) {
			int initialY = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
			mLastFlingY = initialY;
			mScroller.fling(0, initialY, 0, initialVelocity, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
			mTouchMode = TOUCH_MODE_FLING;
			postOnAnimation(this);
		}

		void startScroll(int startX, int startY, int dx, int dy) {
			mScroller.startScroll(startX, startY, dx, dy, 500);
			postOnAnimation(this);
		}

		void endFling() {
			mTouchMode = TOUCH_MODE_REST;

			removeCallbacks(this);
			removeCallbacks(mCheckFlywheel);
		}

		void flywheelTouch() {
			postDelayed(mCheckFlywheel, FLYWHEEL_TIMEOUT);
		}

		@Override
		public void run() {
			switch (mTouchMode) {
			default:
				endFling();
				return;

			case TOUCH_MODE_SCROLL:
				if (mScroller.isFinished()) {
					return;
				}
				// Fall through
			case TOUCH_MODE_FLING:
				if (getChildCount() == 0) {
					endFling();
					return;
				}

				final OverScroller scroller = mScroller;
				boolean more = scroller.computeScrollOffset();
				final int y = scroller.getCurrY();

				int scrolledDistance = mLastFlingY - y;

				// Don't fling more than 1 screen
				scrolledDistance = scrolledDistance > 0 ? Math.min(getHeight(), scrolledDistance) : Math.max(-(getHeight()), scrolledDistance);

				boolean atEnd = trackMotionScroll(scrolledDistance);

				if (more && !atEnd) {
					mLastFlingY = y;
					postOnAnimation(this);
				} else {
					endFling();
				}
				break;
			case TOUCH_MODE_OVERSCROLL:
				if (mScroller.computeScrollOffset()) {
					scrollTo(0, mScroller.getCurrY());
					postOnAnimation(this);
				} else {
					endFling();
				}
				break;
			}
		}
	}

	class AdapterDataSetObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			mDataChanged = true;
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			// ignore
		}
	}

	/**
	 * 调用ItemClickListener提供当前点击位置
	 * 
	 * @param x
	 *            触摸点X轴值
	 * @param y
	 *            触摸点Y轴值
	 */
	private void clickChildAt(int x, int y) {
		// 触摸点在当前显示所有Item中哪一个
		final int itemIndex = getContainingChildIndex(x, y);

		if (itemIndex != INVALID_POSITION) {
			final View itemView = getChildAt(itemIndex);
			// 当前Item在ListView所有Item中的位置
			final int position = mFirstItemPosition + itemIndex;
			final long id = mAdapter.getItemId(position);

			// 调用父类方法，会触发ListView ItemClickListener
			performItemClick(itemView, position, id);
		}
	}
}
