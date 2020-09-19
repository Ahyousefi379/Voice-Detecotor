package com.example.voicedetector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.loader.content.CursorLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {
    private static final int VOICE_RECORD_PERMISSION_CODE = 100;
    private static final int STORAGE_READ_PERMISSION_CODE = 101;
    private static final int STORAGE_WRITE_PERMISSION_CODE = 102;
    private static final int WAKE_LOCK_PERMISSION_CODE = 103;

    private static final int PICK_FILE_RESULT_CODE = 1;
    Button actionButton, openAudioBtn, stopButton;
    TextView textView, sensitivityTextview, recordingLengthTextview;
    File directory = new File(Environment.getExternalStorageDirectory() + File.separator + "Voice Detector");
    VoiceProcess voiceProcess1, voiceProcess2;
    Thread thread1, thread2;
    String path;
    SeekBar sensitivitySeekBar, recordingLengthSeekBar;
    int timeLength, sensitivity;
    MediaPlayer mediaPlayer;

    private boolean isRecording = false, isBuilt = false;

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Permissions check
        checkPermission(Manifest.permission.RECORD_AUDIO, VOICE_RECORD_PERMISSION_CODE);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_READ_PERMISSION_CODE);
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_WRITE_PERMISSION_CODE);
        checkPermission(Manifest.permission.WAKE_LOCK, WAKE_LOCK_PERMISSION_CODE);
        sensitivitySeekBar = findViewById(R.id.sensitivitySeekbar);
        recordingLengthTextview = findViewById(R.id.recordingLengthTextview);
        recordingLengthSeekBar = findViewById(R.id.recordingLengthSeekbar);
        sensitivityTextview = findViewById(R.id.sensitivityTextview);
        actionButton = findViewById(R.id.startButton);
        openAudioBtn = findViewById(R.id.OpenAudioBtn);
        stopButton = findViewById(R.id.stopAlarmBtn);
        textView = findViewById(R.id.textView);
        mediaPlayer = MediaPlayer.create(this, R.raw.alarmsound);


        // todo use room and save sensitivity and time length
        sensitivity = 1;
        timeLength = 3;

        if (!directory.exists()) {
            directory.mkdir();
        }

        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivity = progress;
                sensitivityTextview.setText("sensitivity = " + sensitivity + " %");
                if (isBuilt) {
                    voiceProcess1.setSensitivity(sensitivity);
                    voiceProcess2.setSensitivity(sensitivity);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        recordingLengthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timeLength = progress;
                recordingLengthTextview.setText("Recording length = " + timeLength + "s");


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBuilt) {
                    voiceProcess1.setTimeLength(timeLength);
                    voiceProcess2.setTimeLength(timeLength);
                }

            }
        });


        //open first audio
        openAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(intent, PICK_FILE_RESULT_CODE);

            }

        });

    }

    public void checkPermission(String permission, int requestCode) {

        // Checking if permission is not granted
        if (ActivityCompat.checkSelfPermission(
                MainActivity.this,
                permission)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat
                    .requestPermissions(
                            MainActivity.this,
                            new String[]{permission},
                            requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_FILE_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    String type = getFileName(data.getData());

                    if (type.contains(".mp3")) {
                        try {
                            copy(new File(getRealPathFromURI(data.getData())), new File(directory + "/" + getFileName(data.getData())));
                            new File(directory + "/" + getFileName(data.getData())).renameTo(new File(directory + "/targetvoice.mp3"));
                            new Converter().convert(directory + "/targetvoice.mp3", directory + "/targetvoice.wav");

                        } catch (IOException | JavaLayerException e) {
                            textView.setText(e.getMessage());
                        }
                    } else {
                        if (!type.contains(".wav")) {
                            Toast.makeText(this, "Please select a WAV or MP3 target audio", Toast.LENGTH_SHORT).show();


                        } else {
                            if (type.contains(".wav")) {
                                if (!type.equals("voice1.wav") && !type.equals("voice2.wav")) {
                                    try {
                                        copy(new File(getRealPathFromURI(data.getData())), new File(directory + "/" + getFileName(data.getData())));
                                        new File(directory + "/" + getFileName(data.getData())).renameTo(new File(directory + "/targetvoice.wav"));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                } else {
                                    new File(directory + "/" + getFileName(data.getData())).renameTo(new File(directory + "/targetvoice.wav"));
                                }
                            }


                        }


                    }

                }

        }

    }

    public void RecordBtn(View view) {

        if (!isRecording) {
            if (new File(directory + "/targetvoice.wav").exists()) {


                isRecording = true;
                actionButton.setText("Stop");
                sensitivityTextview = findViewById(R.id.sensitivityTextview);
                voiceProcess1 = new VoiceProcess(1, directory, this, path, sensitivityTextview, sensitivitySeekBar,
                        recordingLengthTextview, timeLength, sensitivity, stopButton, this);
                voiceProcess2 = new VoiceProcess(2, directory, this, path, sensitivityTextview, sensitivitySeekBar,
                        recordingLengthTextview, timeLength, sensitivity, stopButton, this);
                if (!isBuilt) {
                    isBuilt = true;
                }
                VoiceThreads();
            } else {
                Toast.makeText(this, "Please select a WAV or MP3 target audio", Toast.LENGTH_LONG).show();
            }

        } else {
            if (isRecording) {
                isRecording = false;
                actionButton.setText("Start");
                voiceProcess1.stop();
                voiceProcess2.stop();
                thread1.interrupt();
                thread2.interrupt();
            }
        }
    }

    public void VoiceThreads() {
        thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                voiceProcess1.start();
            }
        });
        thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                voiceProcess2.start();
            }
        });

        thread1.start();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                thread2.start();

            }
        }, (timeLength - 1) * 1000);

    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Audio.Media.DATA};
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }


    public void startAlarm() {
        stopButton.setVisibility(View.VISIBLE);
        openAudioBtn.setVisibility(View.INVISIBLE);
        actionButton.setVisibility(View.INVISIBLE);
        isRecording = false;
        actionButton.setText("Start");
        voiceProcess1.stop();
        voiceProcess2.stop();
        thread1.interrupt();
        thread2.interrupt();
        mediaPlayer.setLooping(true);
        mediaPlayer.start();


    }


    public void stopAlarm(View view) throws IOException {
        mediaPlayer.stop();
        mediaPlayer.prepare();
        openAudioBtn.setVisibility(View.VISIBLE);
        actionButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.GONE);

    }
}
