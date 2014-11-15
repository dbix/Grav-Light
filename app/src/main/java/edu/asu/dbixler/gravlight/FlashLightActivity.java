package edu.asu.dbixler.gravlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;


public class FlashLightActivity extends Activity implements SensorEventListener {

    // Camera stuff
    private static boolean hasFlash = false;
    private static boolean flashIsOn = false;
    private static boolean buttonOn = false;
    private static boolean flashWasOnDuringPause;
    private static Camera.Parameters params;
    private static Camera camera;

    // Sensor stuff
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;

    // Used for calculations in onSensorChanged
    // private static final float OFF_THRESHOLD = 9;
    private static float last_x, last_y, last_z;
    private static long current_time, last_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_light);

        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);
        relativeLayout.setBackgroundColor(Color.BLACK);

        // Check if the device has flash support.
        hasFlash = getApplicationContext()
                    .getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        // Close the application if device does not support flash.
        if (!hasFlash) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Unsupported Device")
                    .setMessage("Sorry, your device is not supported :(")
                    .setCancelable(false)
                    .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }

        startCamera();

        // Set up the button.
        ImageButton imageButtonOnOff = (ImageButton) findViewById(R.id.imageButtonOnOff);
        imageButtonOnOff.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonOn = !buttonOn;
                if (flashIsOn) {
                    turnFlashOff();
                    sensorManager.unregisterListener(FlashLightActivity.this);
                } else {
                    turnFlashOn();
                    // Restart the sensor listener.
                    sensorManager.registerListener(
                            FlashLightActivity.this,
                            sensorAccelerometer,
                            SensorManager.SENSOR_DELAY_NORMAL
                    );
                }
            }
        });

        // Set up the sensors.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Turn the camera light on.
     */
    public void turnFlashOn() {
        if (!flashIsOn) {
            if (camera == null || params == null) return;
            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
            flashIsOn = true;
        }
    }

    /**
     * Turn the camera light off.
     */
    private void turnFlashOff() {
        if (flashIsOn) {
            if (camera == null || params == null) return;
            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();
            flashIsOn = false;
        }
    }

    /**
     * Start the camera so we can use the light.
     */
    private void startCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
                params = camera.getParameters();
            } catch (RuntimeException e) {
                Log.e("Camera Error. " +
                        "Failed to Open. " +
                        "Error: ", e.getMessage());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if
        // it is present.
        getMenuInflater().inflate(R.menu.menu_flash_light, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action
        // bar will automatically handle clicks on the
        // Home/Up button, so long as you specify a
        // parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (flashIsOn) {
            turnFlashOff();
            flashWasOnDuringPause = true;
        } else {
            flashWasOnDuringPause = false;
        }
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Turn flash back on and re-register the
        // sensor listener if the light was on when
        // the application was paused.
        if (flashWasOnDuringPause) {
            turnFlashOn();
            sensorManager.registerListener(this,
                    sensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    /**
     * Turn the light off if the top of the phone is facing downwards
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float y = event.values[1];
            float dy = last_y - y;

            if (y < -4 && Math.abs(dy) < 9.8) turnFlashOff();
            else turnFlashOn();

            last_y = y;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // I don't know what to do here yet.
    }
}
