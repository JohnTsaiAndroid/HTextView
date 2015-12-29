package com.hanks.htextview.animatetext;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.animation.BounceInterpolator;

import com.hanks.htextview.R;
import com.hanks.htextview.util.CharacterUtils;

import java.lang.reflect.Field;

/**
 * keynote 轰然坠落效果
 * Created by hanks on 15-12-14.
 */
public class AnvilText extends HText {

    /**
     * 知识点:
     * 1.自定义View画图主要是用Paint类在Canvas上画的
     * 2.Field通过变量名反射拿到值
     * 3.Paint的setAlpha[0,255]数值越小越透明
     * 4.类似小弹球自由落体的效果的插值器BounceInterpolator
     */

    private Paint bitmapPaint;
    private Bitmap[] smokes = new Bitmap[50];
    private float ANIMA_DURATION = 8000; // 每个字符动画时间 500ms
    private int mTextHeight = 0;
    private int mTextWidth;
    private float progress;

    private static final String TAG = "AnvilText";

    @Override
    protected void initVariables() {

        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapPaint.setColor(Color.WHITE);
        bitmapPaint.setStyle(Paint.Style.FILL);
        // load drawable
        try {
            R.drawable d = new R.drawable();
            for (int j = 0; j < 50; j++) {
                String drawable;
                if (j < 10) {
                    drawable = "wenzi000" + j;
                } else {
                    drawable = "wenzi00" + j;
                }
                Field fieldImgId = d.getClass().getDeclaredField(drawable);
                int imgId = (Integer) fieldImgId.get(d);//这个ID就是每个图片资源ID
                smokes[j] = BitmapFactory.decodeResource(mHTextView.getResources(), imgId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * dstHeight/dstWidth = smoke.getHeight/smoke.getWidth
     *
     * @param text
     */

    @Override
    protected void animateStart(CharSequence text) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1)
                .setDuration((long) ANIMA_DURATION);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (float) animation.getAnimatedValue();
                mHTextView.invalidate();
            }
        });
        valueAnimator.start();
        for (int i = 0; i < smokes.length; i++) {
            Bitmap smoke = smokes[i];
            int dstWidth = (int) (mTextWidth * 1.5f);
            if (dstWidth < 400) dstWidth = 400;
            int dstHeight = (int) (smoke.getHeight() * 1f / smoke.getWidth() * dstWidth);
            smokes[i] = Bitmap.createScaledBitmap(smoke, dstWidth, dstHeight, false);
            smoke.recycle();
        }
        System.gc();
    }

    @Override
    protected void animatePrepare(CharSequence text) {

        Rect bounds = new Rect();
        mPaint.getTextBounds(mText.toString(), 0, mText.length(), bounds);
        mTextHeight = bounds.height();
        mTextWidth = bounds.width();
    }


    /**
     *
     * 当ValueAnimator实例的addUpdateListener借口每次动画更新时
     * 会调用onDraw方法重绘界面
     * 最终会调用drawFraw方法
     * drawFraw方法实现了:
     * 1.draw old text(判断old text是否需要移动)
     * 2.draw new text(判断new text是否需要留在原地)
     * 3.show smoke
     * 三个功能
     * @param canvas
     */
    @Override
    protected void drawFrame(Canvas canvas) {
        float offset = startX;
        float oldOffset = oldStartX;

        int maxLength = Math.max(mText.length(), mOldText.length());

        float percent = progress; // 动画进行的百分比 0~1
        Log.d(TAG, "percent:" + percent);
        boolean showSmoke = false;
        for (int i = 0; i < maxLength; i++) {

            // draw old text
            if (i < mOldText.length()) {
                Log.d(TAG,"text:"+mOldText.charAt(i));
                mOldPaint.setTextSize(mTextSize);
                int move = CharacterUtils.needMove(i, differentList);
                if (move != -1) {
                    mOldPaint.setAlpha(255);
                    float p = percent * 2f;
                    p = p > 1 ? 1 : p;
                    float distX = CharacterUtils.getOffset(i, move, p, startX, oldStartX, gaps, oldGaps);
                    canvas.drawText(mOldText.charAt(i) + "", 0, 1, distX, startY, mOldPaint);
                } else {

                    float p = percent * 2f;
                    p = p > 1 ? 1 : p;
                    mOldPaint.setAlpha((int) ((1 - p) * 255));
                    canvas.drawText(mOldText.charAt(i) + "", 0, 1, oldOffset, startY, mOldPaint);
                }
                oldOffset += oldGaps[i];
            }

            // draw new text
            if (i < mText.length()) {

                if (!CharacterUtils.stayHere(i, differentList)) {

                    showSmoke = true;
                    //类似小弹球落地效果的插值器
                    float interpolation = new BounceInterpolator().getInterpolation(percent);

                    mPaint.setAlpha(255);
                    mPaint.setTextSize(mTextSize);

                    float y = startY - (1 - interpolation) * mTextHeight * 2;

                    float width = mPaint.measureText(mText.charAt(i) + "");
                    canvas.drawText(mText.charAt(i) + "", 0, 1, offset + (gaps[i] - width) / 2, y, mPaint);
                }

                offset += gaps[i];
            }
        }

        if (percent > 0.3 && percent < 1) {
            if (showSmoke) {
                drawSmokes(canvas, startX + (offset - startX) / 2f, startY - 50, percent);
            }
        }
    }

    /**
     * @param canvas 画布
     * @param x      中心点x坐标
     * @param y      中心点Y坐标
     */
    private void drawSmokes(Canvas canvas, float x, float y, float percent) {
        Bitmap b = smokes[0];
        try {
            int index = (int) (50 * percent);
            if (index < 0) index = 0;
            if (index >= 50) index = 49;
            b = smokes[index];
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (b != null) {
            int w = b.getWidth();
            int h = b.getHeight();
            canvas.drawBitmap(b, x - w / 2, y - h / 2, bitmapPaint);
        }
    }

}
