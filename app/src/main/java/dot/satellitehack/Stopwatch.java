package dot.satellitehack;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import java.util.Locale;

public class Stopwatch extends android.support.v7.widget.AppCompatTextView {
    private String format = "%d";
    private long second;
//    private AsyncTask<Long, Long, Void> timer;
    private TimerThread timer;
    private Listener listener;
    
    public Stopwatch(Context context) {
        super(context);
    }

    public Stopwatch(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Stopwatch(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getSecond() {
        return second;
    }

    public void setSecond(final long second) {
        this.post(new Runnable() {
            @Override
            public void run() {
                setText(String.format(Locale.getDefault(), format, second));
            }
        });
        this.second = second;
    }

    public Stopwatch start(final long time) {
        stop();
//        timer = new AsyncTask<Long, Long, Void>() {
//            @Override
//            protected Void doInBackground(Long[] time) {
//                while (time[0] > 0) {
//                    publishProgress(time);
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    time[0] -= 100;
//                }
//                return null;
//            }
//
//            @Override
//            protected void onProgressUpdate(Long... times) {
//                for (long time : times) {
//                    setSecond(time / 1000);
//                    if (null != listener)
//                        listener.onEvery100ms(time);
//                }
//            }
//
//            @Override
//            protected void onPostExecute(Void aVoid) {
//                setSecond(0);
//                if (null != listener)
//                    listener.onTimesUp();
//            }
//        };
//        timer.execute(time);
        timer = new TimerThread(time);
        timer.start();
        return this;
    }

    public void stop() {
        if (null != timer && timer.isAlive()) {
            timer.setStop(true);
            timer.interrupt();
            try {
                timer.join();
            } catch (InterruptedException ignored) {}
            timer = null;
        }
    }

    public Stopwatch setListener(Listener listener) {
        this.listener = listener;
        return this;
    }

    public interface Listener {
        void onTimesUp();
        void onEvery100ms(long time);
    }

    private class TimerThread extends Thread {
        private long time;
        private boolean stop;

        TimerThread(long time) {
            this.time = time;
            stop = false;
        }

        public long getTime() {
            return time;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }

        private void tick(long remaining) {
            setSecond(time / 1000);
            if (null != listener)
                listener.onEvery100ms(remaining);
        }

        @Override
        public void run() {
            while (time > 0) {
                tick(time);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (stop) return;
                time -= 100;
            }
            setSecond(0);
            if (null != listener)
                listener.onTimesUp();
        }
    }
}