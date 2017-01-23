package dot.satellitehack;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class SatelliteHack extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SatelliteHack";

    private Camera camera;
    private int viewWidth, viewHeight;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView imageView;
    //private CameraManager cameraManager;
    //private Handler childHandler, mainHandler;
    //private String cameraID;
    //private ImageReader imageReader;
    //private CameraCaptureSession cameraCaptureSession;
    //private CameraDevice cameraDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_satellite_hack);
        initView();
    }

    private void initView() {
        imageView = (ImageView) findViewById(R.id.imageView);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.setOnClickListener(this);
        surfaceHolder = surfaceView.getHolder();
        //surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                initCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (null != camera) {
                    camera.stopPreview();
                    camera.release();
                }
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (null != surfaceView) {
            viewWidth = surfaceView.getWidth();
            viewHeight = surfaceView.getHeight();
        }
    }

    private void initCamera() {
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        if (null != camera) {
            try {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setPreviewSize(viewWidth, viewHeight);
                parameters.setPreviewFpsRange(4,10);
                parameters.setPictureFormat(ImageFormat.JPEG);
                parameters.set("jpeg-quality", 90);
                parameters.setPictureSize(viewWidth, viewHeight);
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (null == camera) return;
        camera.autoFocus(autoFocusCallback);
    }

    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                /*camera.takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {}
                }, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {}
                }, pictureCallback);*/
            }
        }
    };

    /*Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            final Bitmap resource = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (resource == null) {
                Toast.makeText(SatelliteHack.this, "Fail to photograph", Toast.LENGTH_SHORT).show();
            }
            final Matrix matrix = new Matrix();
            matrix.setRotate(90);
            final Bitmap bitmap = Bitmap.createBitmap(resource, 0, 0, resource.getWidth(), resource.getHeight(), matrix, true);
            if (bitmap != null && imageView != null && imageView.getVisibility() == View.GONE) {
                camera.stopPreview();
                imageView.setVisibility(View.VISIBLE);
                surfaceView.setVisibility(View.GONE);
                Toast.makeText(SatelliteHack.this, "Photographed", Toast.LENGTH_SHORT).show();
                imageView.setImageBitmap(bitmap);
            }
        }
    };*/
}
