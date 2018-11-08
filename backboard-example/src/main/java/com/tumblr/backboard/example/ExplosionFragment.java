package com.tumblr.backboard.example;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;
import com.tumblr.backboard.performer.Performer;

import java.util.Random;

/**
 * Demonstrates creating and removing {@link android.view.View}s and
 * {@link com.facebook.rebound.Spring}s.
 * <p/>
 * Created by ericleong on 5/7/14.
 */
public class ExplosionFragment extends Fragment
{

    private static final int DIAMETER = 80;

    private RelativeLayout mRootView;

    private int colorIndex;
    private TypedArray mCircles;

    private static double[] mmp = {1.526379902550308,
            1.536379902550308, 1.546379902550308,
            1.426379902550308, 1.436379902550308,
            1.336379902550308, 1.456379902550308,
            1.636379902550308, 1.686379902550308,
            1.706379902550308, 1.746379902550308,};

    private Handler mHandler;
    private Runnable mRunnable;
    private boolean mTouching;

    private SpringSystem mSpringSystem;

    private SpringConfig mCoasting;
    private SpringConfig mGravity;

    static protected ViewGroup mViewGroup;

    /**
     * Destroys the attached {@link com.facebook.rebound.Spring}.
     */
    private static class Destroyer implements SpringListener
    {

        public int mMin, mMax;

        protected View mViewToRemove;
        int count;

        private Destroyer(ViewGroup viewGroup, View viewToRemove, int min,
                          int max)
        {
            mViewToRemove = viewToRemove;
            mMin = min;
            mMax = max;
        }

        public synchronized boolean shouldClean(Spring spring)
        {
            // these are arbitrary values to keep the view from disappearing before it is
            // fully off the screen
            //这些是任意值，以防止视图在它之前消失
            //完全离开屏幕
            count += Math.abs(spring.getCurrentValue());
            boolean tag = Math.abs(count) > mViewGroup.getHeight();
            count = 0;
            return tag;
        }

        public void clean(Spring spring)
        {
            if (mViewGroup != null && mViewToRemove != null)
            {
                mViewGroup.removeView(mViewToRemove);
            }
            if (spring != null)
            {
                spring.destroy();
            }
        }

        @Override
        public void onSpringUpdate(Spring spring)
        {
            if (shouldClean(spring))
            {
                clean(spring);
            }
        }

        @Override
        public void onSpringAtRest(Spring spring)
        {

        }

        @Override
        public void onSpringActivate(Spring spring)
        {

        }

        @Override
        public void onSpringEndStateChange(Spring spring)
        {

        }
    }

    private class CircleSpawn implements Runnable
    {
        @Override
        public void run()
        {
            if (mTouching)
            {
                colorIndex++;
                if (colorIndex >= mCircles.length())
                {
                    colorIndex = 0;
                }

                float diameter = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DIAMETER,
                        getResources().getDisplayMetrics());

                Drawable drawable = getResources().getDrawable(mCircles.getResourceId
                        (colorIndex, -1));

                createCircle(getActivity(), mRootView, mSpringSystem, mCoasting, mGravity,
                        (int) diameter, drawable);

                mHandler.postDelayed(this, 80);
            }
        }
    }

    static int index = 1;

    private synchronized static void createCircle(Context context, ViewGroup rootView,
                                                  SpringSystem springSystem,
                                                  SpringConfig coasting,
                                                  SpringConfig gravity,
                                                  int diameter,//直径
                                                  Drawable backgroundDrawable)
    {

        final Spring xSpring = springSystem.createSpring().setSpringConfig(coasting);
        final Spring ySpring = springSystem.createSpring().setSpringConfig(gravity);

        // create view
        View view = new View(context);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(diameter, diameter);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        view.setLayoutParams(params);
        view.setTag(index);
        index++;
        view.setBackgroundDrawable(backgroundDrawable);

        rootView.addView(view);

        // generate random direction and magnitude
        double magnitude = Math.random() * 1000 + 2000;//幅度

        double temp = Math.random();
        double angle = temp * Math.PI / 2 + Math.PI / 4;//方向

        int a = new Random().nextInt(mmp.length);
        double angle1 = mmp[a];

        xSpring.setVelocity(magnitude * Math.cos(angle1));
        //*设置弹簧上的速度，以每秒像素为单位 负值向上 正值向下
        ySpring.setVelocity(-(magnitude * Math.sin(angle1) - 500));

        int maxX = rootView.getMeasuredWidth() / 2;
        xSpring.addListener(new Destroyer(rootView, view, -maxX, maxX));

        int maxY = rootView.getMeasuredHeight();
        ySpring.addListener(new Destroyer(rootView, view, -maxY, maxY));

        xSpring.addListener(new Performer(view, View.TRANSLATION_X));
        ySpring.addListener(new Performer(view, View.TRANSLATION_Y));


        // set a different end value to cause the animation to play
        xSpring.setEndValue(0);
        ySpring.setEndValue(0);

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        //创建一个SpringSystem对象
        mSpringSystem = SpringSystem.create();
        // SpringConfig 用于存储弹簧配置的数据结构。
        //得到一个配置好的SpringConfig Tension表示张力 Friction表示摩擦力
        mCoasting = SpringConfig.fromOrigamiTensionAndFriction(0, 0);
        //设置张力为零
        mCoasting.tension = 0;
        // this is very much a hack, since the end value is set to 9001 to simulate constant
        // acceleration.
        mGravity = SpringConfig.fromOrigamiTensionAndFriction(0, 0);
        //设置张力为0
        mGravity.tension = 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mRootView = (RelativeLayout) inflater.inflate(R.layout.fragment_bloom, container, false);
        mCircles = getResources().obtainTypedArray(R.array.circles);
        mRunnable = new CircleSpawn();
        mViewGroup = mRootView;
        mRootView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:

                        // create circles as long as the user is holding down
                        mTouching = true;
                        mHandler.post(mRunnable);

                        break;
                    case MotionEvent.ACTION_UP:
                        mTouching = false;
                        mHandler.removeCallbacks(mRunnable);

                        break;
                }

                return true;
            }
        });

        return mRootView;
    }
}
