package dot.satellitehack;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Created by dixon on 15/2/2017.
 */

class UITools {
    static Bitmap drawSight(int width, int height, float radius) {
        float[] cross = {width/2, height/2 - radius*1.5f, width/2, height/2-radius*0.5f,
                width/2 + radius*1.5f, height/2, width/2 + radius*0.5f, height/2,
                width/2, height/2 + radius*1.5f, width/2, height/2 + radius*0.5f,
                width/2 - radius*1.5f, height/2, width/2 - radius*0.5f, height/2};

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(Color.YELLOW);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bmp);
        canvas.drawCircle(width/2, height/2, radius, paint);
        canvas.drawLines(cross, paint);

        return bmp;
    }

    static Bitmap drawBullsEye(int width, int height, float radius) {
        return drawBullsEye(width, height, radius, false);
    }

    static Bitmap drawBullsEye(int width, int height, float radius, boolean fill) {
        Paint paint = new Paint();
        if (fill)
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
        else paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(Color.RED);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bmp);
        canvas.drawCircle(width/2, height/2, radius, paint);

        return bmp;
    }
}
