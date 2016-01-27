package com.example.danmakusample;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.ui.widget.DanmakuSurfaceView;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

public class MainActivity extends Activity {
	
	private static final String TAG = "MainActivity";

	private DanmakuSurfaceView mDanmakuView;

	private int textSize;
	
	private Handler mUiHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViews();
		init();
		setListener();
	}

	private void findViews() {
		mDanmakuView = (DanmakuSurfaceView) findViewById(R.id.danmaku_surfaceview);
	}

	private void init() {
		textSize = dip2px(this, 12);

		DanmakuTools.initDamakuViews(this, mDanmakuView);
		
		mUiHandler = new Handler();
		
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < 20; i++) {
					long currentViewTime = mDanmakuView.getCurrentTime();
					
					BaseDanmaku danmaku = getChatDanmaku(
							currentViewTime + (i * 10), 
							textSize, 
							"content " + i, 
							Color.BLUE);
					
					mDanmakuView.addDanmaku(danmaku);
				}
				
				mUiHandler.postDelayed(this, 3000);
			}
		};
		
		mUiHandler.postDelayed(runnable, 1000);
		
	}

	private void setListener() {

	}
	
	@Override
    public void onResume() {
        super.onResume();
        if (mDanmakuView != null && mDanmakuView.isPrepared()
                && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.pause();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDanmakuView != null) {
            // dont forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }
    }

	// =====================================================
	
	private void showDanmaku() {
		if (mDanmakuView == null) return;
		
		mDanmakuView.show();
	}
	
	private void hideDanmaku() {
		if (mDanmakuView == null) return;
		
		mDanmakuView.hide();
        mDanmakuView.clearDanmakusOnScreen();
	}
	
	private BaseDanmaku getChatDanmaku(long time, int textSize, CharSequence text, int textColor) {
		BaseDanmaku danmaku = DanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
		
		if (danmaku == null) {
			return null;
		}

		danmaku.priority = 1;
		danmaku.isLive = true;
		danmaku.time = time;
		danmaku.textSize = textSize;
		danmaku.padding = 3;
		danmaku.borderColor = Color.TRANSPARENT;

		//danmaku.index = index;
		//danmaku.userId = userId;
		//danmaku.name = name;
		
		danmaku.text = text;
		danmaku.textColor = textColor;

		danmaku.textShadowColor = Color.BLACK;

		// SpannableStringBuilder sb = new SpannableStringBuilder();
		// sb.append(EmoticonHelper.getTransformText(mActivity,
		// false,ToolUtils.dip2px(mActivity, 18f), ((ChatMsg)
		// object).getContent().getChatmsg()));

		return danmaku;
	}

	/**
	 * 重要：如果有图文混排，最好不要设置描边(设textShadowColor=0)，否则会进行两次复杂的绘制导致运行效率降低
	 * 
	 * @param time
	 * @param textSize
	 * @param index
	 * @param userId
	 * @param name
	 * @param text
	 * @param textColor
	 * @return
	 */
	private BaseDanmaku getRankBeanDanmaku(long time, int textSize, CharSequence text, int textColor) {
		BaseDanmaku danmaku = DanmakuFactory
				.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
		if (danmaku == null) {
			return null;
		}

		danmaku.priority = 1;
		danmaku.isLive = false;
		danmaku.time = time;
		danmaku.textSize = textSize;
		danmaku.padding = 3;
		danmaku.borderColor = Color.TRANSPARENT;

		//danmaku.index = index;
		//danmaku.name = name;
		//danmaku.userId = userId;
		
		danmaku.text = text;
		danmaku.textColor = textColor;

		danmaku.visibility = View.VISIBLE;
		
		danmaku.timeStr = System.currentTimeMillis();

		// danmaku.textShadowColor = 0;

		return danmaku;
	}
	
	private int dip2px(Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

}
