package com.example.voicedetector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
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
import java.nio.channels.FileChannel;
import java.util.Timer;
import java.util.TimerTask;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.callback.ILoadCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {
    private static final int VOICE_RECORD_PERMISSION_CODE = 100;
    private static final int STORAGE_READ_PERMISSION_CODE = 101;
    private static final int STORAGE_WRITE_PERMISSION_CODE = 102;
    private static final int PICK_FILE_RESULT_CODE = 1;
    Button actionButton, openAudioBtn;
    TextView textView, sensitivityTextview, recordingLengthTextview;
    File directory = new File(Environment.getExternalStorageDirectory() + File.separator + "Voice Detector");
    VoiceProcess voiceProcess1, voiceProcess2;
    Thread thread1, thread2;
    String path;
    SeekBar sensitivitySeekBar, recordingLengthSeekBar;
    IConvertCallback callback;
    int timeLength, sensitivity;
    private boolean isRecording = false, isBuilt = false;

    public static void copyFileOrDirectory(String srcDir, String dstDir) {

        try {
            File src = new File(srcDir);
            File dst = new File(dstDir, src.getName());

            if (src.isDirectory()) {

                String files[] = src.list();
                int filesLength = files.length;
                for (int i = 0; i < filesLength; i++) {
                    String src1 = (new File(src, files[i]).getPath());
                    String dst1 = dst.getPath();
                    copyFileOrDirectory(src1, dst1);

                }
            } else {
                copyFile(src, dst);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
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
        sensitivitySeekBar = findViewById(R.id.sensitivitySeekbar);
        recordingLengthTextview = findViewById(R.id.recordingLengthTextview);
        recordingLengthSeekBar = findViewById(R.id.recordingLengthSeekbar);
        sensitivityTextview = findViewById(R.id.sensitivityTextview);
        actionButton = findViewById(R.id.startButton);
        openAudioBtn = findViewById(R.id.OpenAudioBtn);
        textView = findViewById(R.id.textView);
        AndroidAudioConverter.load(this, new ILoadCallback() {
            @Override
            public void onSuccess() {
                Log.e("Converter", "library loaded");

                // Great!
            }

            @Override
            public void onFailure(Exception error) {
                // FFmpeg is not supported by device
                Log.e("Converter", "library didnt load");

            }
        });

         callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                Log.e("Converter", "convert done "+ convertedFile.getPath());
                if (convertedFile.exists()) {
                    Log.e("Converter", "converted file exist");}
                if (convertedFile!=null) {
                    Log.e("Converter", "convertedfile is null");
                }else {
                    Log.e("Converter", "converted file is not null");


                }

                }


            @Override
            public void onFailure(Exception error) {
                Log.e("Converter", "problem is converting error :  "+error.getMessage());
                // Oops! Something went wrong
            }
        };
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
                    Uri uri = data.getData();
//                    path = getRealPathFromURI(uri);
                    converter(data.getData().getPath(),getFileName(uri));


                    if (isBuilt) {
                        voiceProcess2.setPath(path);
                        voiceProcess1.setPath(path);
                    }


                }

        }

    }


    public void RecordBtn(View view) {

        if (!isRecording) {
            isRecording = true;
            actionButton.setText("Stop");
            sensitivityTextview = findViewById(R.id.sensitivityTextview);
            voiceProcess1 = new VoiceProcess(1, directory, this, path, sensitivityTextview, sensitivitySeekBar, recordingLengthTextview, timeLength, sensitivity);
            voiceProcess2 = new VoiceProcess(2, directory, this, path, sensitivityTextview, sensitivitySeekBar, recordingLengthTextview, timeLength, sensitivity);
            if (!isBuilt) {
                isBuilt = true;
            }
            VoiceThreads();

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

    private void converter(String pathString,String child) {


        File flacFile = new File(pathString,child);
        if (flacFile==null){
            Log.e("Converter", "flacfile is null");
            Log.e("Converter", "path = " + pathString );


        }
        else {
            Log.e("Converter", "flac file in not empty");
        }
        if (!flacFile.exists()){
            Log.e("Converter", "flac file doesnt exists");

        }

        AndroidAudioConverter.with(this)
                // Your current audio file
                .setFile(flacFile)

                // Your desired audio format
                .setFormat(AudioFormat.WAV)

                // An callback to know when conversion is finished
                .setCallback(callback)

                // Start conversion
                .convert();

    }

    ;
}
