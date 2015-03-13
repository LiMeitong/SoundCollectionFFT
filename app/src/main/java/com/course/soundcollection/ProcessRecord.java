
package com.course.soundcollection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Handler;

public class ProcessRecord{

    public boolean isRecording;
    private SurfaceView sfvTime,sfvFreq;
    private SurfaceHolder timeHolder, freqHolder;
    private Canvas canvasTime, canvasFreq;
    public ArrayList<short[]> recordBuffer = new ArrayList<>();
    drawThread dThread = new drawThread();
    public static short[] musicData;
    public static short[] fftData;
//initialization
    public void setSfv(SurfaceView _sfvTime,SurfaceView _sfvFreq){
        sfvTime = _sfvTime;
        sfvFreq = _sfvFreq;
    }

//record & play
    private AudioTrack audioTrack;

    public void play() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/reverseme.pcm");
        int musicLength = (int)(file.length()/2);
        short[] music = new short[musicLength];

        try {
            InputStream is = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);

            int i = 0;
            while (dis.available() > 0) {
                music[i] = dis.readShort();
                i++;
            }

            dis.close();
             audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    11025,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    musicLength*2,
                    AudioTrack.MODE_STREAM);
            //order workable
            audioTrack.write(music, 0, musicLength);
            musicData = music;
            FFT();//void FFT, transform musicData to fftData in N points;

            dThread.start();
            audioTrack.play();
            stopPlay();
        } catch (Throwable t) {
            Log.e("AudioTrack","Playback Failed");
        }
    }

    public void stopPlay(){
        audioTrack.stop() ;/*
        Message msg = MainActivity.stopHandler.obtainMessage();
        msg.what = -10;
        MainActivity.stopHandler.sendMessage(msg);*/
    }

    public void record() {
        int frequency = 11025;
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/reverseme.pcm");

        if (file.exists())
            file.delete();

        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create " + file.toString());
        }

        try {
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);

            int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration,  audioEncoding);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    frequency, channelConfiguration,
                    audioEncoding, bufferSize);

            short[] buffer = new short[bufferSize];


            audioRecord.startRecording();

            isRecording = true ;
//            dThread.start();
            while (isRecording) {
                int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                short[] tempBuf = new short[bufferReadResult];

                for (int i = 0; i < bufferReadResult; i++){
                    dos.writeShort(buffer[i]);
                    tempBuf[i] = buffer[i]; // copy buffer
                }
                synchronized (recordBuffer){
                    recordBuffer.add( tempBuf ); // all collect into a buffer, waiting to draw
                }
            }
            audioRecord.stop();
            dos.close();

        } catch (Throwable t) {
            Log.e("AudioRecord","Recording Failed");
        }
    }

    class drawThread extends Thread {
        @Override
        public void run() {
            //draw original data : Time field;
            drawUnit(musicData,sfvTime);
            //draw FFT data: Frequency field;

            drawUnit(fftData,sfvFreq);


        }

        public void drawUnit( short[] piece, SurfaceView surfaceView ){
            Canvas canvas = surfaceView.getHolder().lockCanvas(new Rect(0,0,piece.length,surfaceView.getHeight()));
            int paddingBottom = canvas.getHeight()/2;
            int paddingLeft = 10;
            Paint paint = new Paint();
            paint.setStrokeWidth(2);
            paint.setAntiAlias(true);
            paint.setColor(Color.GREEN);
            int canvasWid = canvas.getWidth();
            int canvasHeight = canvas.getHeight();
            int rateX = 20;

            float yScale = (float) 0.025;
            float xScale = (float) 0.030;

            for (int i = 0; i < piece.length; i += rateX) {
                canvas.drawLine(paddingLeft + xScale * i, paddingBottom, paddingLeft + xScale * i, paddingBottom - piece[i] * yScale, paint);
            }
            surfaceView.getHolder().unlockCanvasAndPost(canvas);
        }


    }

    /**
     * do FFT for musicData, save into fftData
     */
    public void FFT(){
        int N = musicData.length;

    }



}

