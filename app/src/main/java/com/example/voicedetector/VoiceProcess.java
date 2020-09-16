package com.example.voicedetector;

import android.content.Context;
import android.os.Handler;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
    SeekBar seekBar, seekBar2;
    String path;
    Context context;
    int sensitivity;
    boolean isRecording;

    public VoiceProcess(int number, File directory, Context context, String path, TextView textView, SeekBar seekBar, TextView textView2, int timeLength, int sensitivity) {
        this.seekBar = seekBar;
        this.number = number;
        this.sensitivity = sensitivity;
        this.timeLength = timeLength;
        this.textView2 = textView2;
        this.path = path;
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
            @Override
            public void run() {

                waveRecorder.stopRecording();
                isRecording = false;
                Wave wave = new Wave(path);
//                Wave wave = new Wave(Environment.getExternalStorageDirectory() + "/Folan/voice1.wav");
                Wave wave2 = new Wave(directory.getPath() + "/voice" + number + ".wav");
                FingerprintSimilarity fingerprintSimilarity = wave.getFingerprintSimilarity(wave2);
                Toast.makeText(context, "similarity = " + fingerprintSimilarity.getScore(), Toast.LENGTH_SHORT).show();

                //todo process

                if (fingerprintSimilarity.getScore() >= sensitivity) {
                    Toast.makeText(context, "similarity is more than sensitivity = " + fingerprintSimilarity.getScore(), Toast.LENGTH_SHORT).show();


                }
                handler.postDelayed(runnable1, (timeLength - 2) * 1000);


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

    public void setPath(String path) {
        this.path = path;
    }
}
