package com.xy.computer.ahbot_robot;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.location.LocationListener;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
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
import android.widget.Toast;

import com.felipecsl.gifimageview.library.GifImageView;
import com.google.android.gms.common.util.IOUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import com.google.firebase.messaging.RemoteMessage;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
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
import java.util.Random;
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
    static MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        db = new FirestoreHelper(this);
        db.storeMedicine(this);
        ndb = new NotificationFireStore(this);

        gifImageView = (GifImageView) findViewById(R.id.splash);

        //Set GifImageView resource
        try {
            InputStream inputStream = getAssets().open("giphy.gif");
            byte[] bytes = IOUtils.toByteArray(inputStream);
            gifImageView.setBytes(bytes);
            gifImageView.startAnimation();
        } catch (IOException ex) {

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
                    Log.d("Full Results ", texts.get(0));
                    startNlu(text);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

                List<String> texts = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.d("Partial Results ", texts.get(0));
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
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

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
                        responseText = scanQRcode();
                    } else if (speech.equalsIgnoreCase("playing music")) {
                        shouldDetect = false;
                        responseText = speech;
                        startService(new Intent(MainActivity.this, BackGroundMusic.class));
                    } else if (speech.equalsIgnoreCase("recipe_notification")) {
                        responseText = sendRecipeNoti();
                    } else if (speech.equalsIgnoreCase("add_function")) {
                        aiRequest.setQuery("my email is vic2@mail.com");
                        try {
                            aiResponse = aiDataService.request(aiRequest); // need exception because internet might not work --> Alt + Enter

                            result = aiResponse.getResult();
                            fulfillment = result.getFulfillment();
                            speech = fulfillment.getSpeech();
                            Log.d("email", speech);
                        } catch (AIServiceException e) {
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
                // MediaPlayer player = MediaPlayer.create(getApplicationContext(),Settings.System.DEFAULT_NOTIFICATION_URI);
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                    shouldDetect = false;

                } else if (mediaPlayer !=null && !mediaPlayer.isPlaying()) {
                    shouldDetect = true;
                    audioRecord.startRecording();
                }else{
                    audioRecord.startRecording();
                }

                Log.d("hotword", "start listening to hotword");
                while (shouldDetect) {
                    audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    short[] shortArray = new short[audioBuffer.length / 2];
                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);
                    int result = snowboyDetect.runDetection(shortArray, shortArray.length);
                    if (result > 0) {
                        Log.d("hotword", "detected");
                        shouldDetect = false;
                        //player.start();
                    }


                }

                audioRecord.stop();
                audioRecord.release();
                Log.d("hotword", "stop listening to hotword");

                // TODO: Add action after hotword is detected
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {

                } else{
                    shouldDetect = true;
                    startAsr();
                }


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

    public String scanQRcode() {
        final Activity activity = this;
        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
        return "Scanning!";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Log.d("note", "Scanning cancelled");
            } else {
                String content = result.getContents();
                String[] array = content.split("_");
                String id = "";
                Medicine medicine = new Medicine(id, array[0], array[1], array[2], array[3]);

                db.saveData(medicine);

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public String sendRecipeNoti() {
        ndb.saveData("Check your recipe here now!");

        return "I have send a signal to your phone. Open the application, to view the latest healthy recipes.";
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

    public static class BackGroundMusic extends Service {
        AudioManager audioManager;
        int Volume;

        public BackGroundMusic() {
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {

            Random random = new Random();
            int id = (random.nextInt(5) + 1);
            switch (id) {
                case 1:
                    mediaPlayer = MediaPlayer.create(this, R.raw.tune1);
                    break;
                case 2:
                    mediaPlayer = MediaPlayer.create(this, R.raw.tune2);
                    break;
                case 3:
                    mediaPlayer = MediaPlayer.create(this, R.raw.tune3);
                    break;
                case 4:
                    mediaPlayer = MediaPlayer.create(this, R.raw.tune4);
                    break;
                case 5:
                    mediaPlayer = MediaPlayer.create(this, R.raw.tune5);
                    break;
            }
            mediaPlayer.start();
            return super.onStartCommand(intent, flags, startId);
        }


        @Override
        public boolean stopService(Intent name) {

            mediaPlayer.stop();
            mediaPlayer.release();
            return super.stopService(name);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;

        }
    }
}


