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

    //used for keeping track of previous coordinates (we don't want to store duplicate data if the player is standing still)
    private double previousLongitude = 0.0;
    private double previousLatitude = 0.0;

    //used for current coordinates
    double longitude = 0;
    double latitude = 0;

    //keep the loop running
    boolean continueRunning;
    Timer timer;

    //class which gives the users coordinates
    GPSTracker gps;

    //determines if the following button has been pressed
    boolean following = false;

    // JSON parser class
    JSONParser jsonParser = new JSONParser();
    private static final String MOVE_ROBOT_URL = "http://192.168.42.1/move_robot.php";
    private static final String GPS_URL = "http://192.168.42.1/gps.php";

    // JSON element ids from repsonse of php script:
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";

    //"run" method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.move_robot);

        //create links to buttons on page
        Button forward = (Button)findViewById(R.id.btnForward);
        Button left = (Button)findViewById(R.id.btnLeft);
        Button right = (Button)findViewById(R.id.btnRight);
        Button back = (Button)findViewById(R.id.btnBack);
        final Button followMe = (Button)findViewById(R.id.btnFollowMe);

        //create listener for the forward button
        forward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //the user is pressing the forward button
                //if currently following, we need to stop following and then continue with the manual move
                if (following) {
                    //stop following
                    followMe.performClick();
                }
                //turn on move forward
                new Move("turnOn", "1").execute();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //the user released the forward button
                //turn off move forward
                new Move("turnOff", "1").execute();
            }
            return true;
            }
        });

        left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //the user is pressing the left button
                    //if currently following, we need to stop following and then continue with the manual move
                    if (following) {
                        //stop following
                        followMe.performClick();
                    }
                    //turn on move left
                    new Move("turnOn", "2").execute();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    //the user released the left button
                    //turn off move left
                    new Move("turnOff", "2").execute();
                }
                return true;
            }
        });

        right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //the user is pressing the right button
                    //if currently following, we need to stop following and then continue with the manual move
                    if (following) {
                        //stop following
                        followMe.performClick();
                    }
                    //turn on move right
                    new Move("turnOn", "3").execute();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    //the user released the right button
                    //turn off move right
                    new Move("turnOff", "3").execute();
                }
                return true;
            }
        });

        back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //the user is pressing the back button
                    //if currently following, we need to stop following and then continue with the manual move
                    if (following) {
                        //stop following
                        followMe.performClick();
                    }
                    //turn on move back
                    new Move("turnOn", "4").execute();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    //the user released the back button
                    //turn off move back
                    new Move("turnOff", "4").execute();
                }
                return true;
            }
        });

        followMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //if the user just clicked follow
                if (!following) {
                    //timer to determine how often to run the thread
                    timer = new Timer();

                    //turn on following
                    following = true;
                    new Move("turnOn", "5").execute();
                    continueRunning = true;

                    //change the button text
                    followMe.setText("Stop");

                    final Handler handler = new Handler();

                    //start new thread
                    TimerTask doAsynchronousTask = new TimerTask() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                public void run() {
                                    if (!continueRunning) {
                                        return; // stop when told to stop
                                    }
                                    try {
                                        //create the new gps item
                                        gps = new GPSTracker(MoveRobot.this);
                                        if(gps.canGetLocation()){
                                            //get the current coordinates
                                            longitude = gps.getLongitude();
                                            latitude = gps.getLatitude();
                                        } else {
                                            gps.showSettingsAlert();
                                        }

                                        //TODO this part is causing data to only post once regardless of the user moving
                                        //This needs to be tested with the pi turned on

                                        //compare current coordinates with previous coordinates
                                        if (previousLatitude != latitude || previousLongitude != longitude)
                                        {
                                            //if in a new position,
                                            //store current location and update previous location to current lovation
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
                    timer.schedule(doAsynchronousTask, 0, 5000); //execute every 5 seconds
                }
                else {
                    //user pressed stop or one of the manual control buttons performed the button stop button click
                    following = false;

                    //change the button text
                    followMe.setText("Follow");

                    //turn off follow
                    continueRunning = false;
                    new Move("turnOff", "5").execute();

                    //terminate the thread
                    timer.cancel();
                }
            }
        });
    }

    //This class posts the gps coordinates
    class Follow extends AsyncTask<String, String, String> {
        private double longitude;
        private double latitude;
        private ProgressDialog pDialog;

        //constructor
        public Follow(double longitudePassed, double latitudePassed) {
            longitude = longitudePassed;
            latitude = latitudePassed;
        }

        //set up the progress dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MoveRobot.this);
            pDialog.setMessage("Attempting follow...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        //execute the post
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

        //remove progress dialog
        protected void onPostExecute(String file_url) {
            // dismiss the dialog once product deleted
            pDialog.dismiss();
            if (file_url != null) {
                Toast.makeText(MoveRobot.this, file_url, Toast.LENGTH_LONG).show();
            }
        }
    }

    //this class posts the manual move action
    class Move extends AsyncTask<String, String, String> {
        private String action;
        private String pin;
        private ProgressDialog pDialog;

        //constructor
        public Move(String actionPassed, String pinPassed) {
            action = actionPassed;
            pin = pinPassed;
        }

        //display the progress dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MoveRobot.this);
            pDialog.setMessage("Attempting move for pin " + pin);
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            //pDialog.show();
        }

        //execute move
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

        //remove the progress dialog
        protected void onPostExecute(String file_url) {
            // dismiss the dialog once product deleted
            pDialog.dismiss();
            if (file_url != null) {
                Toast.makeText(MoveRobot.this, file_url, Toast.LENGTH_LONG).show();
            }
        }
    }
}