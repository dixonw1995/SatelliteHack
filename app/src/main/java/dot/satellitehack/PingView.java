package dot.satellitehack;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.util.AttributeSet;

import java.util.Arrays;
import java.util.List;

public class PingView extends android.support.v7.widget.AppCompatImageView {
    public static final List<Integer> pingDrawables = Arrays.asList(
            R.drawable.ping_0,
            R.drawable.ping_1,
            R.drawable.ping_2,
            R.drawable.ping_3,
            R.drawable.ping_4,
            R.drawable.ping_5
    );
//    private ObjectAnimator vibrateAnim;
    private Integer ping;
//    private int warning;

    public PingView(Context context) {
        super(context);
        init();
    }

    public PingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        setPing(0);
//        vibrateAnim = ObjectAnimator.ofFloat(this, "TranslationX",
//                getTranslationX(), getTranslationX() + 40f,
//                getTranslationX() - 40f, getTranslationX())
//                .setDuration(500);
//        vibrateAnim.setInterpolator(new LinearInterpolator());
//        vibrateAnim.setRepeatMode(ValueAnimator.RESTART);
//        vibrateAnim.setRepeatCount(1);
//        vibrateAnim = new AnimatorSet();
//        vibrateAnim.playSequentially(
//                ObjectAnimator.ofFloat(this, "X", 50).setDuration(100),
//                ObjectAnimator.ofFloat(this, "X", -50).setDuration(200),
//                ObjectAnimator.ofFloat(this, "X", 0).setDuration(100)
//        );
//        vibrateAnim = new AnimationSet(true);
//        vibrateAnim.setRepeatMode(Animation.RESTART);
//        vibrateAnim.setRepeatCount(3);
//        Animation[] animations = new Animation[3];
//        animations[0] = new TranslateAnimation(0, 20, 0, 0);
//        animations[0].setDuration(100);
//        animations[1] = new TranslateAnimation(20, -20, 0, 0);
//        animations[1].setDuration(200);
//        animations[1].setStartOffset(100);
//        animations[2] = new TranslateAnimation(-20, 0, 0, 0);
//        animations[2].setDuration(100);
//        animations[2].setStartOffset(300);
//        vibrateAnim.addAnimation(animations[0]);
//        vibrateAnim.addAnimation(animations[1]);
//        vibrateAnim.addAnimation(animations[2]);
//        vibrateAnim.setTarget(this);
//        warning = 4;
    }

    public void decreasePing() {
        if (null == ping || ping == 0)
            setPing(pingDrawables.size() - 1);
        else setPing(ping - 1);
    }

    public void setPing(int ping) {
        this.ping = ping;
        if (ping >= 0 && ping < pingDrawables.size())
            setImageResource(pingDrawables.get(ping));
    }

    public Integer getPing() {
        if (null == ping && null != getDrawableId()) {
            ping = pingDrawables.indexOf(getDrawableId());
        }
        return ping;
    }

    public Integer getDrawableId() {
        return (Integer) getTag();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        super.setImageResource(resId);
        ping = pingDrawables.indexOf(resId);
//        if (ping == 4)
//            vibrate();
    }

//    public AnimationSet getVibrateAnim() {
//        return vibrateAnim;
//    }
//
//    public void setVibrateAnim(AnimationSet vibrateAnim) {
//        this.vibrateAnim = vibrateAnim;
//    }

//    public int getWarning() {
//        return warning;
//    }
//
//    public void setWarning(int warning) {
//        this.warning = warning;
//    }
//
//    public void vibrate() {
//        vibrateAnim.start();
//    }
}
