package com.herd.therobot.app;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MoveRobot extends ActionBarActivity {

    private double previousLongitude = 0.0;
    private double previousLatitude = 0.0;

    double longitude = 0;
    double latitude = 0;
    boolean continueRunning;
    Timer timer;

    GPSTracker gps;
    boolean following = false;

    // JSON parser class
    JSONParser jsonParser = new JSONParser();
    private static final String MOVE_ROBOT_URL = "http://192.168.42.1/move_robot.php";
    private static final String GPS_URL = "http://192.168.42.1/gps.php";

    // JSON element ids from repsonse of php script:
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.move_robot);

        gps = new GPSTracker(MoveRobot.this);

        Button forward = (Button)findViewById(R.id.btnForward);
        Button left = (Button)findViewById(R.id.btnLeft);
        Button right = (Button)findViewById(R.id.btnRight);
        Button back = (Button)findViewById(R.id.btnBack);
        final Button followMe = (Button)findViewById(R.id.btnFollowMe);

        forward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (following) {
                    followMe.performClick();
                }
                new Move("turnOn", "1").execute();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                new Move("turnOff", "1").execute();
            }
            return true;
            }
        });

        left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (following) {
                        followMe.performClick();
                    }
                    new Move("turnOn", "2").execute();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    new Move("turnOff", "2").execute();
                }
                return true;
            }
        });

        right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (following) {
                        followMe.performClick();
                    }
                    new Move("turnOn", "3").execute();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    new Move("turnOff", "3").execute();
                }
                return true;
            }
        });

        back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (following) {
                        followMe.performClick();
                    }
                    new Move("turnOn", "4").execute();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    new Move("turnOff", "4").execute();
                }
                return true;
            }
        });

        followMe.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!following) {
                    timer = new Timer();

                    following = true;
                    new Move("turnOn", "5").execute();
                    continueRunning = true;
                    followMe.setText("Stop");

                    //final Follow follow = new Follow(longitude, latitude);
                    final Handler handler = new Handler();
                    TimerTask doAsynchronousTask = new TimerTask() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                public void run() {
                                    if (!continueRunning) {
                                        return; // stop when told to stop
                                    }
                                    try {
                                        gps = new GPSTracker(MoveRobot.this);
                                        if(gps.canGetLocation()){
                                            longitude = gps.getLongitude();
                                            latitude = gps.getLatitude();
                                        } else {
                                            gps.showSettingsAlert();
                                        }

                                        //TODO this part is causing data to only post once regardless of the user moving
                                        //System.out.println("previousLatitude: " + previousLatitude);
                                        //System.out.println("previousLongitude: " + previousLongitude);
                                        //System.out.println("latitude: " + latitude);
                                        //System.out.println("longitude: " + longitude);

                                        //String theToast = "latitude: " + latitude + "\nlongitude: " + longitude;

                                        //Toast.makeText(MoveRobot.this, theToast, Toast.LENGTH_SHORT).show();

                                        if (previousLatitude != latitude || previousLongitude != longitude)
                                        {
                                            previousLongitude = longitude;
                                            previousLatitude = latitude;
                                            Follow follow = new Follow(longitude, latitude);
                                            follow.execute();
                                        }
                                    } catch (Exception e) { }
                                }
                            });
                        }
                    };
                    timer.schedule(doAsynchronousTask, 0, 5000); //execute in every 5 seconds
                }
                else {
                    following = false;
                    followMe.setText("Follow");
                    continueRunning = false;
                    new Move("turnOff", "5").execute();
                    timer.cancel();
                }
            }
        });
    }

    class Follow extends AsyncTask<String, String, String> {

        private double longitude;
        private double latitude;
        private ProgressDialog pDialog;

        public Follow(double longitudePassed, double latitudePassed) {
            longitude = longitudePassed;
            latitude = latitudePassed;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MoveRobot.this);
            pDialog.setMessage("Attempting follow...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
            // Check for success tag
            int success;

            try {
                // Building Parameters
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
                params.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));

                Log.d("request!", "starting");
                // getting product details by making HTTP request
                JSONObject json = jsonParser.makeHttpRequest(GPS_URL, "POST",
                        params);

                // check your log for json response
                Log.d("Follow attempt", json.toString());

                // json success tag
                success = json.getInt(TAG_SUCCESS);
                if (success == 1) {
                    Log.d("Follow Successful!", json.toString());
                    return json.getString(TAG_MESSAGE);
                } else {
                    Log.d("Follow Failure!", json.getString(TAG_MESSAGE));
                    return json.getString(TAG_MESSAGE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String file_url) {
            // dismiss the dialog once product deleted
            pDialog.dismiss();
            if (file_url != null) {
                Toast.makeText(MoveRobot.this, file_url, Toast.LENGTH_LONG).show();
            }
        }
    }

    class Move extends AsyncTask<String, String, String> {

        private String action;
        private String pin;
        private ProgressDialog pDialog;

        public Move(String actionPassed, String pinPassed) {
            action = actionPassed;
            pin = pinPassed;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MoveRobot.this);
            pDialog.setMessage("Attempting move for pin " + pin);
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            //pDialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
            // Check for success tag
            int success;
            try {
                // Building Parameters
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("action", action));
                params.add(new BasicNameValuePair("pin", pin));

                Log.d("request!", "starting");
                // getting product details by making HTTP request
                JSONObject json = jsonParser.makeHttpRequest(MOVE_ROBOT_URL, "POST",
                        params);

                // check your log for json response
                Log.d("Move attempt", json.toString());

                // json success tag
                success = json.getInt(TAG_SUCCESS);
                if (success == 1) {
                    Log.d("Move Successful!", json.toString());
                    return json.getString(TAG_MESSAGE);
                } else {
                    Log.d("Move Failure!", json.getString(TAG_MESSAGE));
                    return json.getString(TAG_MESSAGE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String file_url) {
            // dismiss the dialog once product deleted
            pDialog.dismiss();
            if (file_url != null) {
                Toast.makeText(MoveRobot.this, file_url, Toast.LENGTH_LONG).show();
            }
        }
    }
}