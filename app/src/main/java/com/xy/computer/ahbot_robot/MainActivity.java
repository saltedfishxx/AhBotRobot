package com.xy.computer.ahbot_robot;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.felipecsl.gifimageview.library.GifImageView;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.util.IOUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

import com.google.firebase.messaging.RemoteMessage;
import com.xy.computer.ahbot_robot.FireStore.FirestoreHelper;
import com.xy.computer.ahbot_robot.FireStore.NotificationFireStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Fulfillment;
import ai.api.model.Result;
import ai.kitt.snowboy.SnowboyDetect;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.app.AlarmManager.INTERVAL_HOUR;


public class MainActivity extends AppCompatActivity {
    private GifImageView gifImageView;
    private static final String TAG = "Error:";
    int PERMISSION_ALL = 1;
    FirestoreHelper db;
    NotificationFireStore ndb;
    List<Medicine> medicines = new ArrayList<>();
    Date date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        db = new FirestoreHelper(this);
        db.storeMedicine(this);
        ndb = new NotificationFireStore(this);

        gifImageView = (GifImageView) findViewById(R.id.splash);

        //Set GifImageView resource
        try{
            InputStream inputStream = getAssets().open("giphy.gif");
            byte[] bytes = IOUtils.toByteArray(inputStream);
            gifImageView.setBytes(bytes);
            gifImageView.startAnimation();
        }catch(IOException ex){

        }

        // TODO: Setup Components
        //setupXiaoBaiButton();
        setupAsr();
        setupTts();
        setupNlu();
        setupHotword();
        // TODO: Start Hotword
        startHotword();

    }

    // ASR Variables
    private SpeechRecognizer speechRecognizer; //initialise this in setupASR method

    // TTS Variables
    private TextToSpeech textToSpeech;

    // NLU Variables
    private AIDataService aiDataService;

    // Hotword Variables
    private boolean shouldDetect;
    private SnowboyDetect snowboyDetect;

    static {
        System.loadLibrary("snowboy-detect-android");
    }

//    private void setupXiaoBaiButton() {
//        String BUTTON_ACTION = "com.gowild.action.clickDown_action";
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BUTTON_ACTION);
//        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                // TODO: Add action to do after button press is detected
//                //startAsr();
//                shouldDetect = false; // prevent startAsr from running two times
//            }
//        };
//        registerReceiver(broadcastReceiver, intentFilter);
//    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void setupAsr() {
        // TODO: Setup ASR --> Change Voice into Text
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) { // called when SR is ready to listen to you --> in here put showing faces or playing sound

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) { //when speech volume changes

            }

            @Override
            public void onBufferReceived(byte[] buffer) { //when receiving buffer of audio data

            }

            @Override
            public void onEndOfSpeech() { //when you stop speaking call this

            }

            @Override
            public void onError(int error) { //
                Log.e("asr", "Error: " + Integer.toString(error));
                startHotword();
            }

            @Override
            public void onResults(Bundle results) { //get asr results as list of strings
                List<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (texts == null || texts.isEmpty()) {
                    startNlu("Please try again");
                } else {
                    String text = texts.get(0);
                    startNlu(text);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }

    private void startAsr() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // TODO: Set Language
                final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

                // Stop hotword detection in case it is still running
                shouldDetect = false;

                // TODO: Start ASR
                speechRecognizer.startListening(recognizerIntent);
            }
        };
        Threadings.runInMainThread(this, runnable);
    }

    private void setupTts() {
        // TODO: Setup TTS
        textToSpeech = new TextToSpeech(this, null);

    }

    private void startTts(String text) {
        // TODO: Start TTS
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);

        // TODO: Wait for end and start hotword
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (textToSpeech.isSpeaking()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e("tts", e.getMessage(), e);
                    }
                }

                startHotword();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void setupNlu() {
        // TODO: Change Client Access Token
        String clientAccessToken = "7900db35e2764da4b67fcae8e0adb09c";
        AIConfiguration aiConfiguration = new AIConfiguration(clientAccessToken,
                AIConfiguration.SupportedLanguages.English);
        aiDataService = new AIDataService(aiConfiguration);
    }

    private void startNlu(final String text) {
        // TODO: Start NLU
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                AIRequest aiRequest = new AIRequest();
                aiRequest.setQuery(text);


                try {
                    AIResponse aiResponse = aiDataService.request(aiRequest); // need exception because internet might not work --> Alt + Enter

                    Result result = aiResponse.getResult();
                    Fulfillment fulfillment = result.getFulfillment();
                    String speech = fulfillment.getSpeech();

                    String responseText;
                    if (speech.equalsIgnoreCase("weather_function")) {
                        responseText = getWeather();
                    } else if (speech.equalsIgnoreCase("medicine_function")) {
                        responseText = showMedicine();
                    } else if (speech.equalsIgnoreCase("okay")) {
                        responseText = speech;
                        Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                        startActivity(intent);
                    }else if (speech.equalsIgnoreCase("recipe_notification")){
                        responseText = sendRecipeNoti();
                    }else if (speech.equalsIgnoreCase("add_function")){
                        aiRequest.setQuery("my email is vic2@mail.com");
                        try{
                            aiResponse = aiDataService.request(aiRequest); // need exception because internet might not work --> Alt + Enter

                            result = aiResponse.getResult();
                            fulfillment = result.getFulfillment();
                            speech = fulfillment.getSpeech();
                            Log.d("email", speech);
                        }catch (AIServiceException e) {
                            e.printStackTrace();
                        }
                        responseText = speech;
                    } else {
                        responseText = speech;
                    }

                    startTts(responseText);
                } catch (AIServiceException e) {
                    e.printStackTrace();
                }


            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void setupHotword() {
        shouldDetect = false;
        com.xy.computer.ahbot_robot.SnowboyUtils.copyAssets(this);

        // TODO: Setup Model File
        File snowboyDirectory = com.xy.computer.ahbot_robot.SnowboyUtils.getSnowboyDirectory();
        File model = new File(snowboyDirectory, "alexa_02092017.pmdl");
        File common = new File(snowboyDirectory, "common.res");

        // TODO: Set Sensitivity
        snowboyDetect = new SnowboyDetect(common.getAbsolutePath(), model.getAbsolutePath());
        snowboyDetect.setSensitivity("0.60"); //Change to lower sensitivity --> False Positives
        snowboyDetect.applyFrontend(true);
    }


    private void startHotword() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                shouldDetect = true;
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                int bufferSize = 3200;
                byte[] audioBuffer = new byte[bufferSize];
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                );

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e("hotword", "audio record fail to initialize");
                    return;
                }

                audioRecord.startRecording();
                Log.d("hotword", "start listening to hotword");

                while (shouldDetect) {
                    audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    short[] shortArray = new short[audioBuffer.length / 2];
                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                    int result = snowboyDetect.runDetection(shortArray, shortArray.length);
                    if (result > 0) {
                        Log.d("hotword", "detected");
                        shouldDetect = false;
                    }
                }

                audioRecord.stop();
                audioRecord.release();
                Log.d("hotword", "stop listening to hotword");

                // TODO: Add action after hotword is detected
                startAsr();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }


    private String getWeather() {
        // TODO: (Optional) Get Weather Data via REST API

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.data.gov.sg/v1/environment/2-hour-weather-forecast")
                .addHeader("accept", "application/json")
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            String responseBody = response.body().string();

            JSONObject jsonObject = new JSONObject(responseBody);
            JSONArray forecasts = jsonObject.getJSONArray("items")
                    .getJSONObject(0)
                    .getJSONArray("forecasts");

            for (int i = 0; i < forecasts.length(); i++) {
                JSONObject forecastObject = forecasts.getJSONObject(i);

                String area = forecastObject.getString("area");

                if (area.equalsIgnoreCase("clementi")) {
                    String forecast = forecastObject.getString("forecast");
                    return "The weather in Clementi is" + forecast;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return "No weather info";


    }

    public String sendRecipeNoti(){
        ndb.saveData("Check your recipe here now!");

        return "I have send a signal to your phone. Open the application, to view the latest recipes.";
    }

    public void getMedicine(List<Medicine> medlist) {
        medicines = medlist;
    }

    public String showMedicine() {
        String s = "";
        if (!medicines.isEmpty()) {

            for (int i = 0; i < medicines.size(); i++) {
                Medicine medicine = medicines.get(i);
                // number 1: paracetamol tablet, with the dosage of 2 tablets 4 times per day
                s += "Number " + (i + 1) + ", " + medicine.getMedName() + ", with the dosage of " +
                        medicine.getMedAmount() + " tablets, " + medicine.getMedFrequency() + " times per day.";
            }
            return "The medicines from all the medical profiles are, " + s;
        } else {
            return "Sorry, you have not added any medicine";
        }
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    public void medicineReminder(){

        for (Medicine item : medicines){
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY,11);
            cal.set(Calendar.MINUTE,0);
            cal.set(Calendar.SECOND,0);
            cal.set(Calendar.AM_PM, Calendar.AM);

            date = cal.getTime();

            int frequency = Integer.parseInt(item.getMedFrequency());
            switch (frequency){
                case 1:
                    Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0,intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    AlarmManager am = (AlarmManager) MainActivity.this.getSystemService(MainActivity.this.ALARM_SERVICE);
                    if (am != null) {
                        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                    }
                case 2:
                    Intent intent1 = new Intent(MainActivity.this, AlarmReceiver.class);
                    PendingIntent pendingIntent1 = PendingIntent.getBroadcast(MainActivity.this, 0,intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                    AlarmManager am1 = (AlarmManager) MainActivity.this.getSystemService(MainActivity.this.ALARM_SERVICE);
                    if (am1 != null) {
                        am1.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_HALF_DAY, pendingIntent1);
                    }
                case 3:
                    Intent intent2 = new Intent(MainActivity.this, AlarmReceiver.class);
                    PendingIntent pendingIntent2 = PendingIntent.getBroadcast(MainActivity.this, 0,intent2, PendingIntent.FLAG_UPDATE_CURRENT);
                    AlarmManager am2 = (AlarmManager) MainActivity.this.getSystemService(MainActivity.this.ALARM_SERVICE);
                    if (am2 != null) {
                        am2.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 4*AlarmManager.INTERVAL_HOUR, pendingIntent2);
                    }
                case 4:
                    Intent intent3 = new Intent(MainActivity.this, AlarmReceiver.class);
                    PendingIntent pendingIntent3 = PendingIntent.getBroadcast(MainActivity.this, 0,intent3, PendingIntent.FLAG_UPDATE_CURRENT);
                    AlarmManager am3 = (AlarmManager) MainActivity.this.getSystemService(MainActivity.this.ALARM_SERVICE);
                    if (am3 != null) {
                        am3.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 3*AlarmManager.INTERVAL_HOUR, pendingIntent3);
                    }
            }
        }
    }


    @Override
    protected void onDestroy() {
        deleteCache(this);
        shouldDetect = false;
        //Close the Text to Speech Library
        if (textToSpeech != null) {

            textToSpeech.stop();
            textToSpeech.shutdown();
            Log.d("tts", "TTS Destroyed");
        }
        try {
            speechRecognizer.destroy();
        } catch (Exception e) {
            Log.e("nlu", "Exception:" + e.toString());
        }
        super.onDestroy();
    }

}


