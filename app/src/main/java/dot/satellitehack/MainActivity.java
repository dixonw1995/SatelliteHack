package dot.satellitehack;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final int SAT_HACK_ID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void play(View view) {
        EditText countView = (EditText) findViewById(R.id.count);
        int level = Integer.parseInt(countView.getText().toString());
        Intent intent = new Intent(this, SatelliteHackActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("level", level);
        startActivityForResult(intent, SAT_HACK_ID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SAT_HACK_ID) {
            boolean success = false;
            int second = -1;
            if (null != data) {
                success = data.getBooleanExtra(SatelliteHackActivity.RESULT, success);
                second = data.getIntExtra(SatelliteHackActivity.TIME, second);
            }
            TextView result = (TextView) findViewById(R.id.result);
            TextView time = (TextView) findViewById(R.id.time);
            if (success) {
                result.setText(R.string.congratulation);
                if (second > -1)
                    time.setText(getString(R.string.time, second));
            }
            else {
                result.setText(R.string.good_luck);
                time.setText("");
            }
        }
    }

    public void gc(View view) {
        System.gc();
    }
}
