package dot.satellitehack;

import android.content.Context;
import android.util.AttributeSet;

public class GalaxyView extends android.support.v7.widget.AppCompatImageView {
    public static final int[] galaxyDrawables = new int[]{
            R.drawable.scope_galaxy1,
            R.drawable.scope_galaxy2,
            R.drawable.scope_galaxy3,
            R.drawable.scope_galaxy4
    };

    public GalaxyView(Context context) {
        super(context);
    }

    public GalaxyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GalaxyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        if (null == getTag()) {
            setImageResource(galaxyDrawables[(MathTools.random.nextInt(
                    galaxyDrawables.length
            ))]);
        }
    }
}
