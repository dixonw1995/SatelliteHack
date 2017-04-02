package dot.satellitehack;

import android.content.Context;
import android.util.AttributeSet;

public class SatelliteView extends android.support.v7.widget.AppCompatImageView {
    public static final int[] satelliteDrawables = new int[]{
            R.drawable.satellite01,
            R.drawable.satellite02,
            R.drawable.satellite03,
            R.drawable.satellite04,
            R.drawable.satellite05,
            R.drawable.satellite06,
            R.drawable.satellite07
    };

    public SatelliteView(Context context) {
        super(context);
    }

    public SatelliteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SatelliteView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        if (null == getDrawable()) {
            setImageResource(satelliteDrawables[MathTools.random.nextInt(
                    satelliteDrawables.length
            )]);
        }
        setEnabled(false);
        setVisibility(GONE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            setVisibility(VISIBLE);
        }else {
            setVisibility(GONE);
        }
    }
}

