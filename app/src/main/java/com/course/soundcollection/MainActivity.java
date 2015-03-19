package com.course.soundcollection;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


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
    public static Handler stopHandler;
    public int recordLength;
    private Button playButton;
    private SurfaceView timeSurface;
    private SurfaceView freqSruface;
    ProcessRecord processRecord;

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
        timeSurface = (SurfaceView) findViewById(R.id.surfaceTime);
        freqSruface = (SurfaceView) findViewById(R.id.surfaceFreq);
        processRecord = new ProcessRecord();
        processRecord.setSfv(timeSurface,freqSruface);

/*
        stopHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
           //     if( msg.what == -10 )
           //     playButton.setText("播放");
            }
        };*/

        cntHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int remain = (int) msg.obj;
                hintCount.setText(" 正在录音 ... " + remain + " seconds.");
                recordLength = remain;
            }
        };

        /*
        click on record button
         */
        ctrlRecord.setOnClickListener(new View.OnClickListener() {
            countDown cntDown;

            @Override
            public void onClick(View view) {
                /*
                点击开始录音
                 */
                if (ctrlRecord.getText().toString().equals("开始录音")) {

                    processRecord = new ProcessRecord();
                    processRecord.setSfv(timeSurface,freqSruface);

                    cntDown = new countDown();
                    //begin counting down, count seconds immediately;
                    ctrlRecord.setText("停止录音");
                    cntDown.start();
                    //begin mediaRecorder to save file;
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            processRecord.record();
                        }
                    });
                    thread.start();
                }

                /*
                点击停止录音
                 */
                else {
                    ctrlRecord.setText("开始录音");
                    cntDown.exit = true;
                    hintCount.setText("录音长度" + recordLength + "秒");
                    playButton.setEnabled(true);// after recording ended, can play.
                    processRecord.isRecording=false;

                }
            }
        });

        /*
        click on play button
         */
        //播放键，停止键
        playButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                    processRecord.play();
            }
        });


    }


}
