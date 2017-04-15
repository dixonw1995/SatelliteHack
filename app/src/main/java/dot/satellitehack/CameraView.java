package dot.satellitehack;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class CameraView extends SurfaceView {
    private Camera camera;
    private static final String UI_TAG = "UI";

    public CameraView(Context context) {
        super(context);
        initCamera();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCamera();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCamera();
    }

    private void initCamera() {
        Log.i(UI_TAG, "Set surface view call back.");
//        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.camera);
        SurfaceHolder holder = this.getHolder();
        holder.setKeepScreenOn(true);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.i(UI_TAG, "Turn on camera.");
                camera = Camera.open();
                camera.setDisplayOrientation(90);
                if (null != camera) {
                    try {
                        camera.setPreviewDisplay(surfaceHolder);
                        camera.startPreview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (null != camera) {
                    Log.i(UI_TAG, "Turn off camera.");
                    camera.stopPreview();
                    camera.release();
                    surfaceHolder.getSurface().release();
                }
            }
        });
    }
}
