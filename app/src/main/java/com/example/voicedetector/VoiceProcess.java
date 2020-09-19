package com.example.voicedetector;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.github.squti.androidwaverecorder.WaveRecorder;
import com.musicg.fingerprint.FingerprintSimilarity;
import com.musicg.wave.Wave;

import java.io.File;

public class VoiceProcess {
    WaveRecorder waveRecorder;
    int number, timeLength;
    File directory;
    Handler handler = new Handler();
    Runnable runnable1, runnable2;
    TextView textView, textView2;
    SeekBar seekBar;
    String path;
    Button stopAlarm;
    Context context;
    int sensitivity;
    MainActivity activity;
    boolean isRecording;

    public VoiceProcess(int number, File directory, Context context, String path, TextView textView,
                        SeekBar seekBar, TextView textView2, int timeLength, int sensitivity, Button stopAlarm, MainActivity activity) {
        this.seekBar = seekBar;
        this.number = number;
        this.sensitivity = sensitivity;
        this.timeLength = timeLength;
        this.textView2 = textView2;
        this.stopAlarm = stopAlarm;
        this.path = path;
        this.activity = activity;
        this.textView = textView;
        this.context = context;
        this.directory = directory;
        waveRecorder = new WaveRecorder(this.directory.getPath() + "/voice" + number + ".wav");
        waveRecorder.getWaveConfig().setSampleRate(44100);
        waveRecorder.setNoiseSuppressorActive(true);


    }

    public void start() {

        runnable1 = new Runnable() {
            @Override
            public void run() {
                waveRecorder.startRecording();
                isRecording = true;
                handler.postDelayed(runnable2, timeLength * 1000);

            }
        };
        runnable2 = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {

                waveRecorder.stopRecording();
                isRecording = false;
                Wave wave = new Wave(directory + "/voice" + number + ".wav");
                Wave wave2 = new Wave(directory + "/targetvoice.wav");
                FingerprintSimilarity fingerprintSimilarity;
                fingerprintSimilarity = wave.getFingerprintSimilarity(wave2);
                Toast.makeText(context, "similarity = " + fingerprintSimilarity.getScore(), Toast.LENGTH_SHORT).show();

                //todo process

                if (fingerprintSimilarity.getScore() * 100 >= sensitivity) {
                    activity.startAlarm();
                    stop();

                } else {
                    handler.postDelayed(runnable1, (timeLength - 2) * 1000);
                }

            }

        };
        runnable1.run();


    }

    public void stop() {
        handler.removeCallbacks(runnable1);
        handler.removeCallbacks(runnable2);
        if (isRecording) {
            waveRecorder.stopRecording();
        }
    }

    public void setTimeLength(int timeLength) {
        this.timeLength = timeLength;
    }

    public void setSensitivity(int timeLength) {
        this.timeLength = timeLength;
    }


}
