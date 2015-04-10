package com.example.listviewimpl.view;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Adapter;
import android.widget.AdapterView;

/**
 * 自定义ListView 1. 填充一屏幕Item 2. 添加滚动处理 3. 可以随意滚动，上下都可以自动填充 4. ItemClick 5.
 * LongClick 6. View复用
 * 
 */
public class CopyOfMyListView extends AdapterView<Adapter> {
	// ===========================================================
	// Constants
	// ===========================================================

	// 新添加的所有子视图在当前最当前最后一个子视图后添加的布局模型
	private static final int LAYOUT_MODE_BELOW = 0;
	// 与LAYOUT_MODE_BELOW相反方向添加的布局模型
	private static final int LAYOUT_MODE_ABOVE = 1;

	// 初始模式，用户还未接触ListView
	private static final int TOUCH_MODE_REST = -1;
	// 触摸Down事件模式
	private static final int TOUCH_MODE_DOWN = 0;
	// 滚动模式
	private static final int TOUCH_MODE_SCROLL = 1;

	private static final int INVALID_INDEX = -1;

	// ===========================================================
	// Fields
	// ===========================================================

	// 视图和数据适配
	private Adapter mAdapter;
	// 当前显示最后一个Item在Adapter中位置
	private int mLastItemPosition;
	// 当前显示第一个Item在Adapter中位置
	private int mFirstItemPosition;

	// 当前顶部第一个item
	private int mListTop;
	// 当前第一个显示的item与底部第一个item的顶部偏移量
	private int mListTopOffset;
	// 触摸Down事件时进行记录
	private int mListTopStart;

	// 记录ListView当前处于哪种模式
	private int mTouchMode = TOUCH_MODE_REST;

	// 记录上一次触摸X轴
	private int mTouchStartX;
	// 记录上一次触摸Y轴
	private int mTouchStartY;
	// 仅记录Down事件时Y轴值
	private int mMotionY;

	// 触发滚动的最小移动距离
	private int mTouchSlop;

	// 可反复使用的Rect
	private Rect mRect;

	// 用于检测是手长按动作
	private Runnable mLongPressRunnable;

	// View复用当前仅支持一种类型Item视图复用
	// 想更多了解ListView视图如何复用可以看AbsListView内部类RecycleBin
	private final LinkedList<View> mCachedItemViews = new LinkedList<View>();

	// ===========================================================
	// Constructors
	// ===========================================================

	public CopyOfMyListView(Context context) {
		super(context);
		initListView(context);
	}

	public CopyOfMyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initListView(context);
	}

	public CopyOfMyListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initListView(context);
	}

	// ===========================================================
	// Public Methods
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public Adapter getAdapter() {
		return mAdapter;
	}

	@Override
	public void setAdapter(Adapter adapter) {
		mAdapter = adapter;
		removeAllViewsInLayout();
		requestLayout();
	}

	@Override
	public View getSelectedView() {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public void setSelection(int position) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		
		if (mAdapter == null) {
			return;
		}

		if (getChildCount() == 0) {
			mLastItemPosition = 0;
			fillListDown(mListTop, 0);
		} else {
			final int offset = mListTop + mListTopOffset - getChildAt(0).getTop();
			// 移除可视区域的都干掉
			removeNonVisibleViews(offset);
			fillList(offset);
		}

		// layout，添加测量完后，获取视图摆放位置
		positioinItems(left);

		// draw， 上面子视图都添加完了，重绘布局把子视图绘制出来吧
		invalidate();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			startTouch(ev);
			return false;

		case MotionEvent.ACTION_HOVER_MOVE:
			return startScrollIfNeeded((int) ev.getY());

		default:
			endTouch();
			return false;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (getChildCount() == 0) {
			return false;
		}

		final int y = (int) event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			startTouch(event);
			break;

		case MotionEvent.ACTION_MOVE:
			if (mTouchMode == TOUCH_MODE_DOWN) {
				startScrollIfNeeded(y);
			} else if (mTouchMode == TOUCH_MODE_SCROLL) {
				scrollList(y);
			}
			break;

		case MotionEvent.ACTION_UP:
			// 如果当前触摸没有触发滚动，状态依然是DOWN
			// 说明是点击某一个Item
			if (mTouchMode == TOUCH_MODE_DOWN) {
				clickChildAt((int) event.getX(), y);
			}

			endTouch();
			break;

		default:
			endTouch();
			break;
		}

		return true;
	}

	// ===========================================================
	// Private Methods
	// ===========================================================

	/**
	 * ListView初始化
	 */
	private void initListView(Context context) {

		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = configuration.getScaledTouchSlop();
	}

	/**
	 * 向当前ListView添加子视图并负责Measure子视图操作
	 * 
	 * @param child
	 *            需要添加的ListView子视图(Item)
	 * @param layoutMode
	 *            在顶部添加上面添加还是在底部下面添加子视图 ， LAYOUT_MODE_ABOVE 或 LAYOUT_MODE_BELOW
	 */
	private void addAndMeasureChild(View child, int layoutMode) {
		LayoutParams params = child.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}

		final int index = layoutMode == LAYOUT_MODE_ABOVE ? 0 : -1;
		addViewInLayout(child, index, params, true);

		final int itemWidth = getWidth();
		child.measure(MeasureSpec.EXACTLY | itemWidth, MeasureSpec.UNSPECIFIED);
	}

	/**
	 * 对所有子视图进行layout操作，取得所有子视图正确的位置
	 * @param left 
	 */
	private void positioinItems(int left) {
		int top = mListTop + mListTopOffset;
		System.out.println(mListTop);
		System.out.println(mListTopOffset + "==");

		for (int i = 0; i < getChildCount(); i++) {
			final View child = getChildAt(i);
			final int width = child.getMeasuredWidth();
			final int height = child.getMeasuredHeight();

			child.layout(left, top, left + width, top + height);
			top += height;
		}
	}

	/**
	 * 初始化用于之后触摸事件判断处理的参数
	 * 
	 * @param event
	 */
	private void startTouch(MotionEvent event) {
		mTouchStartX = (int) event.getX();
		mMotionY = mTouchStartY = (int) event.getY();

		mListTopStart = getChildAt(0).getTop() - mListTopOffset;

		startLongPressCheck();

		mTouchMode = TOUCH_MODE_DOWN;
	}

	/**
	 * 是否满足滚动条件
	 * 
	 * @param y
	 *            当前触摸点Y轴的值
	 * @return true 可以滚动
	 */
	private boolean startScrollIfNeeded(int y) {
		// 不同，此处模拟AbsListView实现

		final int deltaY = y - mMotionY;
		final int distance = Math.abs(deltaY);

		// 只有移动一定距离之后才认为目的是想让ListView滚动
		if (distance > mTouchSlop) {

			// 记录当前处于滚动状态
			mTouchMode = TOUCH_MODE_SCROLL;
			return true;
		}

		return false;
	}

	/**
	 * 通过触摸点X,Y轴坐标获取是点击哪一个Item
	 * 
	 * @param x
	 *            触摸点X轴值
	 * @param y
	 *            触摸点Y轴值
	 * @return
	 */
	private int getContainingChildIndex(int x, int y) {
		if (mRect == null) {
			mRect = new Rect();
		}

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

	/**
	 * 控制ListView进行滚动
	 * 
	 * @param y
	 *            当前触摸点Y轴的值
	 */
	private void scrollList(int y) { // scrollIfNeeded
		// 当前手指坐在位置与刚触摸到屏幕之间的距离
		// 也就是当前手指在屏幕上Y轴总移动位置
		int scrolledDistance = y - mTouchStartY;
		// 改变当前记录的ListView顶部位置
		mListTop = mListTopStart + scrolledDistance;

		// 关键，要想使相面的计算生效必须重新请求布局
		// 会触发当前onLayout方法，指定Item位置与绘制先关还是在onLayout中
		requestLayout();
	}

	/**
	 * ListView向上或者向下移动后需要向顶部或者底部添加视图
	 * 
	 * @param offset
	 */
	private void fillList(final int offset) {
		// 最后一个item的下边界值就是当前ListView的下边界值
		final int bottomEdge = getChildAt(getChildCount() - 1).getBottom();
		fillListDown(bottomEdge, offset);

		// 第一个Item的上边界值就是ListVie的上边界值
		final int topEdge = getChildAt(0).getTop();
		fillListUp(topEdge, offset);
	}

	/**
	 * 与fillListDown相反方向添加
	 * 
	 * @param topEdge
	 *            当前第一个子视图顶部边界值
	 * @param offset
	 *            显示区域偏移量
	 */
	private void fillListUp(int topEdge, int offset) {
		while (topEdge + offset > 0 && mFirstItemPosition > 0) {
			// 现在添加的视图时当前子视图前面，所以位置-1
			mFirstItemPosition--;

			View newTopChild = mAdapter.getView(mFirstItemPosition, getCachedView(), this);
			addAndMeasureChild(newTopChild, LAYOUT_MODE_ABOVE);
			int childHeight = newTopChild.getMeasuredHeight();
			topEdge -= childHeight;

			// 在顶部添加视图后，更新顶部偏移
			mListTopOffset -= childHeight;
		}
	}

	/**
	 * 向当前最后一个子视图下面添加，填充到当前ListView底部无再可填充区域为止
	 * 
	 * @param bottomEdge
	 *            当前最后一个子视图底部边界值
	 * @param offset
	 *            显示区域偏移量
	 */
	private void fillListDown(int bottomEdge, int offset) {
		for (;bottomEdge + offset < getHeight() && mLastItemPosition < mAdapter.getCount();mLastItemPosition++) {
			View newBottomChild = mAdapter.getView(mLastItemPosition, getCachedView(), this);
			addAndMeasureChild(newBottomChild, LAYOUT_MODE_BELOW);
			bottomEdge += newBottomChild.getMeasuredHeight();
		}
	}

	/**
	 * 触摸屏幕结束，进行清理操作
	 */
	private void endTouch() {
		// 都结束了，无论是否执行了，干掉长按子线程
		removeCallbacks(mLongPressRunnable);

		mTouchMode = TOUCH_MODE_REST;
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

		if (itemIndex != INVALID_INDEX) {
			final View itemView = getChildAt(itemIndex);
			// 当前Item在ListView所有Item中的位置
			final int position = mFirstItemPosition + itemIndex;
			final long id = mAdapter.getItemId(position);

			// 调用父类方法，会触发ListView ItemClickListener
			performItemClick(itemView, position, id);
		}
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
						final int index = getContainingChildIndex(mTouchStartX, mTouchStartY);
						if (index != INVALID_INDEX) {
							longClickChild(index);
						}
					}
				}
			};
		}

		// ViewConfiguration.getLongPressTimeout() 获取系统配置的长按的时间间隔
		// 如果点击已经超过长按要求时间，才开始执行此线程
		postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
	}

	/**
	 * 调用ItemLongClickListener提供点击位置等信息
	 * 
	 * @param index
	 *            Item索引值
	 */
	private void longClickChild(final int index) {
		final View itemView = getChildAt(index);
		final int position = mFirstItemPosition + index;
		final long id = mAdapter.getItemId(position);
		// 从父类获取绑定的OnItemLongClickListener
		OnItemLongClickListener listener = getOnItemLongClickListener();

		if (listener != null) {
			listener.onItemLongClick(this, itemView, position, id);
		}
	}

	/**
	 * 删除当前已经移除可视范围的Item View
	 * 
	 * @param offset
	 *            可视区域偏移量
	 */
	private void removeNonVisibleViews(final int offset) {
		int childCount = getChildCount();

		/** ListView向上滚动，删除顶部移除可视区域的所有视图 **/

		// 不在ListView底部，子视图大于1
		if (mLastItemPosition != mAdapter.getCount() - 1 && childCount > 1) {
			View firstChild = getChildAt(0);
			// 通过第二条件判断当前最上面的视图是否被移除可是区域
			while (firstChild != null && firstChild.getBottom() + offset < 0) {
				// 既然顶部第一个视图已经移除可视区域从当前ViewGroup中删除掉
				removeViewInLayout(firstChild);
				// 用于下次判断，是否当前顶部还有需要移除的视图
				childCount--;
				// View对象回收，目的是为了复用
				mCachedItemViews.addLast(firstChild);
				// 既然最上面的视图被干掉了，当前ListView第一个显示视图也需要+1
				mFirstItemPosition++;
				// 同上更新
				mListTopOffset += firstChild.getMeasuredHeight();

				// 为下一次while遍历获取参数
				if (childCount > 1) {
					// 当前已经删除第一个，再接着去除删除后剩余的第一个
					firstChild = getChildAt(0);
				} else {
					// 没啦
					firstChild = null;
				}
			}
		}

		/** ListView向下滚动，删除底部移除可视区域的所有视图 **/
		// 与上面操作一样，只是方向相反一个顶部操作一个底部操作
		if (mFirstItemPosition != 0 && childCount > 1) {
			View lastChild = getChildAt(childCount - 1);
			while (lastChild != null && lastChild.getTop() + offset > getHeight()) {
				removeViewInLayout(lastChild);
				childCount--;
				mCachedItemViews.addLast(lastChild);
				mLastItemPosition--;

				if (childCount > 1) {
					lastChild = getChildAt(childCount - 1);
				} else {
					lastChild = null;
				}
			}
		}

	}

	/**
	 * 获取一个可以复用的Item View
	 * 
	 * @return view 可以复用的视图或者null
	 */
	private View getCachedView() {

		if (mCachedItemViews.size() != 0) {
			return mCachedItemViews.removeFirst();
		}

		return null;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
