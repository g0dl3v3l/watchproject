package com.example.sensordatacollectorwithfilter;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Locale;

import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoDatabase;
import okhttp3.OkHttpClient;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import com.google.gson.Gson;
public class MainActivity extends FragmentActivity
        implements AmbientModeSupport.AmbientCallbackProvider, SensorEventListener, SocketConnectionListener {
    public interface SocketConnectionListener {
        void startSensorUpdates();
    }


    private static final String TAG = "MainActivity";
    //String Appid = "application-0-csdly";
    private OkHttpClient okHttpClient;
    //App app1;
    private static final DecimalFormat decfor = new DecimalFormat("0.00000000");
    MongoClient mongoClient;
    MongoDatabase mongoDatabase;
    private SensorManager mSensorManager;
    private Sensor gSensor, accelerometer, mAccel, mGyro, mLina, mMagno, mRot;
    private boolean isLinaPresent = false;
    private boolean isGyroPresent  = false;
    private boolean isMagnoPresent = false;
    private boolean isAccPresent = false;
    private boolean isRotPresent = false;
    //    private static final long START_TIME_IN_MILLIS= 60000; //60s
//    private static final long START_TIME_IN_MILLIS= 600000; //10m
    ImageView image;
    private TextView tdate;
    private StringBuilder buffer = new StringBuilder();

    Button save, record;
    float x,y,z, x_gy, y_gy, z_gy, x_lin, y_lin, z_lin, x_magno, y_magno, z_magno;
    String x_rot,y_rot,z_rot,s_rot;
    double Mag_accel, Mag_gyro,Mag_lin,Mag_magnet;
    String activityInput;
    String DATA = "";
    String newline = "";
    String modified_DATA = "";
    String dateCurrent;
    String dateCurrentTemp = "";
    String x_val, y_val, z_val, xG_val, yG_val, zG_val, xL_val, yL_val, zL_val, xM_val, yM_val, zM_val, x_Mag, a_Mag, g_Mag, l_Mag, m_Mag;
    private FileWriter writer;
    private Socket socket;
    Context context = this;
    File gpxfile;

    int CounterForSave = 0;
    int timePeriod = 0;
    int SamplingRate;
    private boolean permission_to_record = false;



    private ScalarKalmanFilter mFiltersCascade[] = new ScalarKalmanFilter[3];

    private CountDownTimer mCountDownTimer;

    private EditText Activity;
    private TextView mTextViewCountDown;
    private Button mButtonStartPause;
    private Button mButtonReset;
    private boolean mTimerRunning;

    private static final long START_TIME_IN_MILLIS= 30000; //5m
    private long mTimeLeftInMillis = START_TIME_IN_MILLIS;
    Vibrator vibrator;
    int temp = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());

        setContentView(R.layout.activity_main);
        tdate = (TextView) findViewById(R.id.tdate) ;
        new ConnectToServerTask(this).execute();

        //OkHttpClient okHttpClient = new OkHttpClient();
        //okHttpClient = new OkHttpClient();
        //Request request = new Request.Builder().url("http://192.168.0.113:5000/").build();
        //Realm.init(this);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //App app = new App(new AppConfiguration.Builder(Appid).build());
        //app1 = app;

//        Credentials credentials = Credentials.anonymous();
        //Credentials credentials = Credentials.emailPassword("dheeraj.mahendiran@gmail.com" , "dheeraj");
//        app1.loginAsync(credentials, new App.Callback<User>() {
//            @Override
//            public void onResult(App.Result<User> result) {
//                if(result.isSuccess()) {
//
//                    Log.v("User", "Logged in anonymously");
//                }
//                else
//                {
//                    Log.v("User" , "Failed to login");
//                }
//            }
//        });
//       // app1.login(credentials);
//        try {
//            app.getEmailPassword().registerUserAsync("CS@CS.com", "dheeraj" ,it->{});
//        } catch(Exception e){
//
//        }
        AmbientModeSupport.attach(this);
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


            mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_FASTEST);
//        mLina = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        mRot = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

            accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        // Create socket to connect to server


        mFiltersCascade[0] = new ScalarKalmanFilter(1, 1, 0.01f, 0.0025f);
        mFiltersCascade[1] = new ScalarKalmanFilter(1, 1, 0.01f, 0.0025f);
        mFiltersCascade[2] = new ScalarKalmanFilter(1, 1, 0.01f, 0.0025f);

        mButtonStartPause = findViewById(R.id.Record);
        mButtonReset = findViewById(R.id.Save);
        save = (Button) findViewById(R.id.Save);
        record = (Button) findViewById(R.id.Record);
        //get the spinner from the xml.
        Spinner dropdown = findViewById(R.id.spinner1);
//        Spinner dropdown2 = findViewById(R.id.spinner2);
        Spinner dropdown3 = findViewById(R.id.activity);
        //create a list of items for the spinner.
        String[] items1 = new String[]{"20 dps", "25 dps", "30 dps"};
//        String[] items2 = new String[]{"1.5 min", "2 min", "3 min"};
        String[] items3 = new String[]{"breaststroke","backstroke","crawl","PassiveDrowning","ActiveDrowning","walk", "stand", "jump","fall"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items1);
//        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items2);
        ArrayAdapter<String> adapter3 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items3);

        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);
//        dropdown2.setAdapter(adapter2);
        dropdown3.setAdapter(adapter3);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        SamplingRate = 20;
                        break;
                    case 1:
                        SamplingRate = 25;
                        break;
                    case 2:
                        SamplingRate = 30;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
//        dropdown2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                switch (position){
//                    case 0:
//                        START_TIME_IN_MILLIS = 90000;
//                        break;
//                    case 1:
//                        START_TIME_IN_MILLIS = 120000;
//                        break;
//                    case 2:
//                        START_TIME_IN_MILLIS = 180000;
//                        break;
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });
        dropdown3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//
//                record.setEnabled(!activityInput.isEmpty());
                switch (position){

                    case 0:
                        activityInput = "breaststroke";
                        break;
                    case 1:
                        activityInput = "backstroke";
                        break;
                    case 2:
                        activityInput = "crawl";
                        break;
                    case 3:
                        activityInput = "PassiveDrowning";
                        break;
                    case 4:
                        activityInput = "ActiveDrowning";
                        break;
                    case 5:
                        activityInput = "walk";
                        break;
                    case 6:
                        activityInput = "stand";
                        break;
                    case 7:
                        activityInput = "jump";
                        break;
                    case 8:
                        activityInput = "fall";
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        updateCountDownText();

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permission_to_record = true;

                if (record.getText() != "pause") {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }

                vibrator.vibrate(500);
                if (mTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                    Toast.makeText(MainActivity.this, "Start Recording...", Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "Touch Screen Disabled", Toast.LENGTH_SHORT).show();
                    record.setBackgroundColor(Color.RED);
                }


            }

        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
                record.setBackgroundColor(Color.DKGRAY);

            }
        });
    }
//    private TextWatcher activityTextWatcher = new TextWatcher() {
//        @Override
//        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//        }
//
//        @Override
//        public void onTextChanged(CharSequence s, int start, int before, int count) {
//            activityInput = Activity.getText().toString();
//
//            record.setEnabled(!activityInput.isEmpty());
//        }
//
//        @Override
//        public void afterTextChanged(Editable s) {
//
//        }
//    };
    private Runnable mutiThread = new Runnable(){
        public void run(){
            long date2 = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH-mm-ss");
            String dateString = sdf.format(date2);

            String theFileName = "SmartWatch" + dateString + "_" + activityInput + ".csv";
            String thePath = "/storage";
            String theData = "DATE,TIME,ax,ay,az,gx,gy,gz,ma,mg,timePeriod,label\n" + modified_DATA;
            InputStream theInput = new ByteArrayInputStream(theData.getBytes());


            File folder = context.getFilesDir();
            gpxfile = new File(folder, theFileName);
            try {
                writer = new FileWriter(gpxfile);
                writer.write(theData);

                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "File Not Found!", Toast.LENGTH_SHORT).show();
            } catch (IOException e){
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error saving!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void startTimer() {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();

            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                permission_to_record = false;

                save.setBackgroundColor(Color.GREEN);
                record.setBackgroundColor(Color.DKGRAY);
                Toast.makeText(MainActivity.this, "File Created & Saved", Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "Touch screen enabled", Toast.LENGTH_SHORT).show();
                // File management
                timePeriod = 0;
                resetTimer();

                Thread thread = new Thread(mutiThread);
                thread.start();
                temp = 0;
                vibrator.vibrate(1500);
                // Activity.getText().clear();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                mButtonStartPause.setText("Record");
                mButtonStartPause.setVisibility(View.INVISIBLE);
                mButtonReset.setVisibility(View.VISIBLE);

            }
        }.start();
        mTimerRunning = true;
        mButtonStartPause.setText("pause");
        mButtonReset.setVisibility(View.INVISIBLE);
    }
    private void pauseTimer() {
        mCountDownTimer.cancel();
        onPause();
        mTimerRunning = false;
        mButtonStartPause.setText("Start");
        mButtonReset.setVisibility(View.VISIBLE);
    }

    private void resetTimer() {
        mTimeLeftInMillis = START_TIME_IN_MILLIS;
        updateCountDownText();
        mButtonReset.setVisibility(View.INVISIBLE);
        mButtonStartPause.setVisibility(View.VISIBLE);
        newline = "";
    }

    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        mTextViewCountDown.setText(timeLeftFormatted);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_STEM_1:
                Toast.makeText(this, "Touch screen enabled", Toast.LENGTH_SHORT).show();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                return true;


        }

        return super.onKeyDown(keyCode, event);

    }
    @Override
    protected void onResume() {
        super.onResume();

        if (accelerometer != null) {
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            isAccPresent = true;
        }
        if (mGyro != null) {
            mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_FASTEST);
            isGyroPresent = true;
        }
//        if (mRot != null) {
//            mSensorManager.registerListener(this, mRot, SensorManager.SENSOR_DELAY_NORMAL);
//            isRotPresent = true;
//        }
//        if (mLina != null) {
//            mSensorManager.registerListener(this, mLina, SensorManager.SENSOR_DELAY_NORMAL);
//            isLinaPresent = true;
//        }
    }
    @Override
    protected void onPause() {
        super.onPause();

//        if(isLinaPresent) {
//            mSensorManager.unregisterListener(this, mLina);
//        }
        if(isAccPresent) {
            mSensorManager.unregisterListener(this, accelerometer);
        }
        if(isGyroPresent) {
            mSensorManager.unregisterListener(this, mGyro);
        }
//        if(isRotPresent) {
//            mSensorManager.unregisterListener(this, mRot);
//        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Smoothes the signal from accelerometer
     */
    private float filter(float measurement){
        float f1 = mFiltersCascade[0].correct(measurement);
        float f2 = mFiltersCascade[1].correct(f1);
        float f3 = mFiltersCascade[2].correct(f2);
        return f3;
    }

    long sTime = System.currentTimeMillis();


    @Override
    public void onSensorChanged(SensorEvent event) {
        //User user = app1.currentUser();
//        mongoClient = user.getMongoClient("mongodb-atlas");
//        mongoDatabase = mongoClient.getDatabase("watchdata");
//        MongoCollection<org.bson.Document> mongoCollection = mongoDatabase.getCollection("test data");
//        RequestBody body = new FormBody.Builder()
//                .add("data", "dheeraj")
//                .build();
//        String json = "{\"id\":1,\"name\":\"John\"}";
//
//        RequestBody body = RequestBody.create(
//                MediaType.parse("application/json"), json);


        long date = System.currentTimeMillis();
        long timePeriod = date - sTime;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss");
        dateCurrent = sdf.format(date);
        tdate.setText(dateCurrent);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

//            x = filter(event.values[0]);
//            y = filter(event.values[1]);
//            z = filter(event.values[2]);
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            x_val = String.valueOf(decfor.format(x));
            y_val = String.valueOf(decfor.format(y));
            z_val = String.valueOf(decfor.format(z));

            Mag_accel = Math.sqrt(Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2));
            a_Mag = String.valueOf(decfor.format(Mag_accel));

        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

//            x_gy = filter(event.values[0]);
//            y_gy = filter(event.values[1]);
//            z_gy = filter(event.values[2]);

            x_gy = event.values[0];
            y_gy = event.values[1];
            z_gy = event.values[2];

            xG_val = String.valueOf(decfor.format(x_gy));
            yG_val = String.valueOf(decfor.format(y_gy));
            zG_val = String.valueOf(decfor.format(z_gy));

            Mag_gyro = Math.sqrt(Math.pow(x_gy,2)+Math.pow(y_gy,2)+Math.pow(z_gy,2));
            g_Mag = String.valueOf(decfor.format(Mag_gyro));

        }
//        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
//
//            x_lin = event.values[0];
//            y_lin = event.values[1];
//            z_lin = event.values[2];
//
//            xL_val = String.valueOf(event.values[0]);
//            yL_val = String.valueOf(event.values[1]);
//            zL_val = String.valueOf(event.values[2]);
////            Mag_lin = Math.sqrt(Math.pow(x_lin,2)+Math.pow(y_lin,2)+Math.pow(z_lin,2));
////            l_Mag = String.valueOf(Mag_lin);
//
//        }
//        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//
//            x_magno = event.values[0];
//            y_magno = event.values[1];
//            z_magno = event.values[2];
//            xM_val = String.valueOf(event.values[0]);
//            yM_val = String.valueOf(event.values[1]);
//            zM_val = String.valueOf(event.values[2]);
//            Mag_magnet = Math.sqrt(Math.pow(x_magno,2)+Math.pow(y_magno,2)+Math.pow(z_magno,2));
//            m_Mag = String.valueOf(Mag_magnet);
//        }
//        if(event.sensor.getType()==Sensor.TYPE_ROTATION_VECTOR){
//            x_rot = String.valueOf(event.values[0]);
//            y_rot = String.valueOf(event.values[1]);
//            z_rot = String.valueOf(event.values[2]);
//            s_rot = String.valueOf(event.values[3]);
//        }
        if (!dateCurrentTemp.equals(dateCurrent)){
            dateCurrentTemp = dateCurrent;
            CounterForSave = 0;

        }

        if (CounterForSave<SamplingRate & permission_to_record) {
            DATA = dateCurrent + "," + x_val+ "," + y_val + "," + z_val + "," + xG_val +","+yG_val+","+zG_val+","+ a_Mag +","+g_Mag+","+timePeriod+","+activityInput+"\n"; //"DATE,TIME,WALK,JUMP,STATIC,FALLDOWN\n"
//            List<org.bson.Document> sensorData = Arrays.asList(
//                    new org.bson.Document().append("data Current", dateCurrent).append("x_val", x_val).append("y_val", y_val).append("z_val", z_val).append("xG_val", xG_val).append("yG_val", yG_val).append("zG_val", zG_val).append("a_Mag", a_Mag).append("g_Mag", g_Mag));
            modified_DATA = newline + DATA;
            newline = modified_DATA;
            CounterForSave = CounterForSave +1;
            buffer.append(dateCurrent).append(",").append(x_val).append(",").append(y_val).append(",").append(z_val).append(",").append(xG_val).append(",").append(a_Mag).append(",").append(timePeriod).append(",").append("\n");


            //Packet packet = new Packet(DATA.length(), DATA);

            //sendSensorData(packet);
            JSONObject data1 = new JSONObject();
//            try {
//                data1.put("dateCurrent", dateCurrent);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
            try {
                data1.put("x_val", x_val);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                data1.put("y_val", y_val);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                data1.put("z_val", z_val);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                data1.put("xG_val", xG_val);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                data1.put("yG_val", yG_val);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                data1.put("zG_val", zG_val);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                data1.put("a_Mag", a_Mag);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                   data1.put("g_Mag", g_Mag);
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            if(temp < 1) {
//                try {
//                    Thread.sleep(5000);
//                    temp = temp + 1;
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            else {
             //sendSensorData(Integer.toString(((data1.toString()).length())));
             sendSensorData((data1.toString()));
            Log.d(TAG, (Integer.toString(((data1.toString()).length()))));
            Log.d(TAG , data1.toString());
   //         }

//            RequestBody body = new FormBody.Builder()
//                    .add("data",modified_DATA )
//                    .build();

//            Request request = new Request.Builder().url("http://172.20.10.2:5000")
//                    .post(body)
//                    .build();
//            okHttpClient.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(
//                        @NotNull Call call,
//                        @NotNull IOException e) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(), "server down", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                }

//                @Override
//                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
//                    if (response.body().string().equals("received")) {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(getApplicationContext(), "data received", Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }
//                }
            //});



        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No op.
    }

    @Override
    public void startSensorUpdates() {
        //sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        //sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);

        //mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_FASTEST);
//        mLina = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        mRot = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        //accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

    }


    private class SendDataAsyncTask extends AsyncTask<Void, Void, Void> {
        //Packet packet ;
        private String Data;

        //private float x, y, z;

        public SendDataAsyncTask( String Data ) {
            this.Data = Data;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
               // SimpleMessage simpleMessage = new SimpleMessage(Data);
                OutputStream outputStream = socket.getOutputStream();
                //ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                //JSONObject packet = new JSONObject();
//                packet.put("type", "message");
//                packet.put("payload", Data);
                //String data = "\n" +Data + "\n";
                //outputStream.write(data.getBytes());
                //String json = serialize(packet).toString();
                //String data = serialize(packet);
                int dataLength = Data.length();
                outputStream.write(ByteBuffer.allocate(4).putInt(dataLength).array());
                OutputStreamWriter osw = new OutputStreamWriter(outputStream);
                //outputStream.write(data.getBytes());
                //outputStream.write(simpleMessage.buffer, 0, simpleMessage.messageLength);
               // outputStream.flush();
                //oos.writeObject(data);
                osw.write(Data);
                osw.flush();

            } catch (IOException e) {
                e.printStackTrace();

            }
            return null;
        }


    }
    private void sendSensorData(String Data) {
        new SendDataAsyncTask(Data).execute();
    }

//    public class SimpleMessage {
//        public byte[] buffer = new byte[1024];
//        public int messageLength;
//
//        public SimpleMessage(String msg) {
//            // the total message length is the length of the original message plus the header size
//            this.messageLength = Integer.BYTES + msg.length();
//
//            // first copy the total length of the message into the buffer
//            ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).putInt(messageLength);
//
//            // then the message itself
//            System.arraycopy(msg.getBytes(), 0, buffer, Integer.BYTES, msg.length());
//        }
//    }
        private static class Packet implements Serializable {
            private int length;
            private long timestamp;
            private String values;
            public Packet(int length,  String values) {
                this.length = length;

                this.values = values;
            }
        }

    private static String serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toString();
    }
    private class ConnectToServerTask extends AsyncTask<Void, Void, Void> {
        private MainActivity mActivity;
        public ConnectToServerTask(MainActivity activity) {
            mActivity = activity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final long RETRY_INTERVAL_MS = 5000;
            try {
                socket = new Socket("172.20.10.2", 5000);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (socket != null) {
                // Connection successful, start sensor updates
                 startSensorUpdates();
            } else {
                // Connection failed, display error message
                Toast.makeText(MainActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
            }
        }


    }




    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    /** Customizes appearance for Ambient mode. (We don't do anything minus default.) */
    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        /** Prepares the UI for ambient mode. */
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            super.onEnterAmbient(ambientDetails);
        }

        /**
         * Updates the display in ambient mode on the standard interval. Since we're using a custom
         * refresh cycle, this method does NOT update the data in the display. Rather, this method
         * simply updates the positioning of the data in the screen to avoid burn-in, if the display
         * requires it.
         */
        @Override
        public void onUpdateAmbient() {
            super.onUpdateAmbient();
        }

        /** Restores the UI to active (non-ambient) mode. */
        @Override
        public void onExitAmbient() {
            super.onExitAmbient();
        }
    }
}