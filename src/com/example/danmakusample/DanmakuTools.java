package com.example.danmakusample;

import java.util.HashMap;

import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.android.DanmakuGlobalConfig;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuSurfaceView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.text.TextPaint;
import android.view.WindowManager;

public class DanmakuTools {
	public static void initDamakuViews(Context context, final DanmakuSurfaceView danmakuSurfaceView) {
		if (danmakuSurfaceView == null)
			return;

		danmakuSurfaceView.setBackgroundColor(context.getResources().getColor(
				android.R.color.transparent));

		danmakuSurfaceView.setZOrderOnTop(true);// 设置画布 背景透明
		danmakuSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);// 避免SurfaceView黑屏
		WindowManager.LayoutParams mLayout = new WindowManager.LayoutParams();
		mLayout.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
		
		// 设置最大显示行数
		HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
		// 滚动弹幕最大显示5行
		//maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 5);
		// 设置是否禁止重叠
		HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
		overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
		overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
		DanmakuGlobalConfig.DEFAULT
				.setDanmakuStyle(DanmakuGlobalConfig.DANMAKU_STYLE_STROKEN, 5)
				.setDuplicateMergingEnabled(false)
				.setScrollSpeedFactor(1.2f)
				.setScaleTextSize(1.2f)
				// .setCacheStuffer(new SpannedCacheStuffer()) //
				// 图文混排使用SpannedCacheStuffer
				.setCacheStuffer(new BackgroundCacheStuffer())
				// 绘制背景使用BackgroundCacheStuffer
				.setMaximumLines(maxLinesPair)
				.preventOverlapping(overlappingEnablePair)
				.setMaximumVisibleSizeInScreen(0);

		danmakuSurfaceView.setCallback(new Callback() {
			@Override
			public void updateTimer(DanmakuTimer timer) {
			}

			@Override
			public void prepared() {
				danmakuSurfaceView.start();
			}
		});

		// 从本地获取文件流 得到解析器parser
		danmakuSurfaceView.prepare(new BaseDanmakuParser() {
			@Override
			protected Danmakus parse() {
				return new Danmakus();
			}
		});

		danmakuSurfaceView.showFPS(false);
		danmakuSurfaceView.enableDanmakuDrawingCache(true);
		danmakuSurfaceView.show();

	}

	private static class BackgroundCacheStuffer extends SpannedCacheStuffer {
		// 通过扩展SimpleTextCacheStuffer或SpannedCacheStuffer个性化你的弹幕样式
		final Paint paint = new Paint();

		@Override
		public void measure(BaseDanmaku danmaku, TextPaint paint) {
			initPaint(paint);
			super.measure(danmaku, paint);
		}

		@Override
		public void drawBackground(BaseDanmaku danmaku, Canvas canvas,
				float left, float top) {
			super.drawBackground(danmaku, canvas, left, top);
		}

		@Override
		public void drawStroke(BaseDanmaku danmaku, String lineText,
				Canvas canvas, float left, float top, Paint paint) {
			initPaint(paint);
			super.drawStroke(danmaku, lineText, canvas, left, top, paint);
		}

		@Override
		public void drawText(BaseDanmaku danmaku, String lineText,
				Canvas canvas, float left, float top, Paint paint) {
			initPaint(paint);
			super.drawText(danmaku, lineText, canvas, left, top, paint);

		}

		private void initPaint(Paint paint) {
			// 设定阴影(柔边, X 轴位移, Y 轴位移, 阴影颜色)
			// paint.setShadowLayer(3, 2, 2, Color.BLACK);
			// paint.setStrokeWidth(0.8f);
			// paint.setColor(Color.BLACK);
			// paint.setStyle(Style.STROKE); //描边种类
			// paint.setFakeBoldText(true); // 外层text采用粗体
		}
	}
}
