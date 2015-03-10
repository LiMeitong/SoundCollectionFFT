package com.course.soundcollection;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.IOException;

class countDown extends Thread {
    public boolean exit = false;

    @Override
    public void run() {
        int i = 0;
        while (!exit && (i < 100)) {
            i++;
            Message remain = MainActivity.cntHandler.obtainMessage();
            remain.obj = i;
            MainActivity.cntHandler.sendMessage(remain);

            try {
                Thread.sleep(1000);
                if (exit) {
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.d("err", "Thread sleep error");
            }
        }

    }
}

public class MainActivity extends ActionBarActivity {
    private Button ctrlRecord;
    private TextView hintCount;
    public static Handler cntHandler;
    public int recordLength;
    private static MediaRecorder inputRecord;
    private static MediaPlayer playRecord;
    private Button playButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctrlRecord = (Button) findViewById(R.id.ControlRecordBtn);
        ctrlRecord.setText("开始录音");
        hintCount = (TextView) findViewById(R.id.CntDown);
        hintCount.setText("没有录音");
        playButton = (Button) findViewById(R.id.PlayButton);
        playButton.setEnabled(false);

        cntHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int remain = (int) msg.obj;
                hintCount.setText(" 正在录音 ... " + remain + " seconds.");
                recordLength = remain;
            }
        };


        ctrlRecord.setOnClickListener(new View.OnClickListener() {
            countDown cntDown;

            @Override
            public void onClick(View view) {
                /*
                点击的是开始录音
                 */
                if (ctrlRecord.getText().toString().equals("开始录音")) {
                    cntDown = new countDown();
                    //begin counting down, count seconds immediately;
                    ctrlRecord.setText("停止录音");
                    cntDown.start();

                    //begin mediaRecorder to save file;
                    try {
                        inputRecord = new MediaRecorder();
                        inputRecord.setAudioSource(MediaRecorder.AudioSource.MIC);
                        inputRecord.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                        inputRecord.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        inputRecord.setOutputFile("/sdcard/audio.amr");
                        inputRecord.prepare();
                        inputRecord.start();
                    } catch (IOException e) {
                        Log.d("err","Start Record Error");
                    }
                    //begin audioRecorder to analyse audio, in thread TODO


                }

                /*
                点击的是停止录音
                 */
                else {
                    ctrlRecord.setText("开始录音");
                    cntDown.exit = true;
                    hintCount.setText("录音长度" + recordLength + "秒");
                    playButton.setEnabled(true);// after recording ended, can play.

                    if (inputRecord != null) {
                        inputRecord.stop();
                        inputRecord.release();
                        inputRecord = null;
                    }

                }
            }
        });

        //播放键，停止键
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //点击了：播放
                if (playButton.getText().toString().equals("播放")) {
                    playButton.setText("停止");

                    if (playRecord != null) {
                        playRecord.stop();
                        playRecord.release();
                        playRecord = null;
                    }
                    playRecord = new MediaPlayer();
                    try {
                        playRecord.setDataSource("/sdcard/audio.amr");
                        playRecord.prepare();
                        playRecord.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // play completed,
                    playRecord.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {

                            if (playRecord != null) {
                                playRecord.stop();
                                playRecord.release();
                                playRecord = null;
                            }
                            playButton.setText("播放");
                        }
                    });
                }
                // 点击了：停止
                else {
                    if (playRecord != null) {
                        playRecord.stop();
                        playButton.setText("播放");

                    } else {
                        Log.d("err", "stop error");
                    }
                }
            }
        });


    }

}
