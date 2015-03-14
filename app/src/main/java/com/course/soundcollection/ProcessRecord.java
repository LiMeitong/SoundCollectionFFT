
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
//复数的加减乘运算

class Complex {
    public double real;
    public double image;

    //三个构造函数
    public Complex() {
        // TODO Auto-generated constructor stub
        this.real = 0;
        this.image = 0;
    }

    public Complex(double real, double image){
        this.real = real;
        this.image = image;
    }

    public Complex(int real, int image) {
        Integer integer = real;
        this.real = integer.floatValue();
        integer = image;
        this.image = integer.floatValue();
    }

    public Complex(double real) {
        this.real = real;
        this.image = 0;
    }
    //乘法
    public Complex cc(Complex complex) {
        Complex tmpComplex = new Complex();
        tmpComplex.real = this.real * complex.real - this.image * complex.image;
        tmpComplex.image = this.real * complex.image + this.image * complex.real;
        return tmpComplex;
    }
    //加法
    public Complex sum(Complex complex) {
        Complex tmpComplex = new Complex();
        tmpComplex.real = this.real + complex.real;
        tmpComplex.image = this.image + complex.image;
        return tmpComplex;
    }
    //减法
    public Complex cut(Complex complex) {
        Complex tmpComplex = new Complex();
        tmpComplex.real = this.real - complex.real;
        tmpComplex.image = this.image - complex.image;
        return tmpComplex;
    }
    //获得一个复数的值
    public int getIntValue(){
        int ret = 0;
        ret = (int) Math.round(Math.sqrt(this.real*this.real + this.image*this.image));
        return ret;
    }
}

public class ProcessRecord{

    public boolean isRecording;
    private SurfaceView sfvTime,sfvFreq;
    private SurfaceHolder timeHolder, freqHolder;
    private Canvas canvasTime, canvasFreq;
    public ArrayList<short[]> recordBuffer = new ArrayList<>();
    drawThread dThread = new drawThread();
    public static short[] musicData0;
    public static int[] musicData;
    public static int[] fftData;
    int rateX = 15;
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
            musicData0 = music;
            audioTrack.play();



            //make a sample at rateX , save it to musicData
            //do FFT at sampled array, save it to fftData
            sample(  );

            dThread.start();
            stopPlay();
        } catch (Throwable t) {
            Log.e("AudioTrack","Playback Failed");
        }
    }

    public void sample(){
        int sampled[] = new int[musicData0.length/rateX+2];
        for( int i=0;i<musicData0.length;i+=rateX ){
            sampled[i/rateX] = (int)musicData0[i];
        }
        musicData = sampled;
        FFT();
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
            drawUnitTime(musicData,sfvTime);
            //draw FFT data: Frequency field;

            drawUnitFreq(fftData,sfvFreq);


        }

        public int getRange( int[] num ){
            int min = 99999, max = -99999;
            for( int i=0;i<num.length;i++ ){
                if( min > num[i] ) min = num[i];
                if( max < num[i] ) max = num[i];
            }
            return max-min;
        }

        public void drawUnitTime( int[] piece, SurfaceView surfaceView ){
            int range = getRange( piece )/10;
            Canvas canvas = surfaceView.getHolder().lockCanvas(new Rect(0,0,piece.length,surfaceView.getHeight()));
            int paddingBottom = (int) (canvas.getHeight()/1.5);
            int paddingLeft = 10;
            Paint paint = new Paint();
            paint.setStrokeWidth(2);
            paint.setAntiAlias(true);
            paint.setColor(Color.GREEN);
            int canvasWid = canvas.getWidth();
            int canvasHeight = canvas.getHeight();

            float yScale = (float) canvas.getHeight()/range;
            float xScale = (float) 0.130;

            for (int i = 0; i < piece.length; i ++) {
                canvas.drawLine(paddingLeft + xScale * i * rateX/2, paddingBottom, paddingLeft + xScale * i * rateX/2, paddingBottom - piece[i] * yScale, paint);
            }
            surfaceView.getHolder().unlockCanvasAndPost(canvas);
        }

        public void drawUnitFreq( int[] piece, SurfaceView surfaceView ){
            int range = getRange( piece );
            Canvas canvas = surfaceView.getHolder().lockCanvas(new Rect(0,0,piece.length,surfaceView.getHeight()));
            int paddingBottom = (int) (canvas.getHeight()/1.3);
            int paddingLeft = 10;
            Paint paint = new Paint();
            paint.setStrokeWidth(2);
            paint.setAntiAlias(true);
            paint.setColor(Color.GREEN);
            int canvasWid = canvas.getWidth();
            int canvasHeight = canvas.getHeight();

            float yScale = (float) canvas.getHeight()/range;
            float xScale = (float) 0.130;

            for (int i = 0; i < piece.length; i ++) {
                canvas.drawLine(paddingLeft + xScale * i * rateX/2, paddingBottom, paddingLeft + xScale * i * rateX/2, paddingBottom - piece[i] * yScale, paint);
            }
            surfaceView.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    /**
     * do FFT for musicData, save into fftData
     */
    public void FFT(){
        int N = to2N( musicData.length );

        Complex xin[] = new Complex[N];
        float pi = (float) 3.1415926;
        int f,m,N2,nm,i,k,j,L;//L:运算级数
        float fftScale = (float) 0.05;
        float p;
        int e2,le,B,ip;
        Complex w = new Complex();
        Complex t ;
        N2 = N / 2;//每一级中蝶形的个数,同时也代表m位二进制数最高位的十进制权值
        f = N;//f是为了求流程的级数而设立的
        for(m = 1; (f = f / 2) != 1; m++);                             //得到流程图的共几级
        nm = N - 2;
        j = N2;

        for( i=0;i<N;i++ )
            xin[i] = new Complex((double)musicData[i]);

        //rearrange

        for(i = 1; i <= nm; i++)
        {
            if(i < j)//防止重复交换
            {
                t = xin[j];
                xin[j] = xin[i];
                xin[i] = t;
            }
            k = N2;
            while(j >= k)
            {
                j = j - k;
                k = k / 2;
            }
            j = j + k;
        }
        for(L=1; L<=m; L++)                                    //从第1级到第m级
        {
            e2 = (int) Math.pow(2, L);
            //e2=(int)2.pow(L);
            le=e2+1;
            B=e2/2;
            for(j=0;j<B;j++)                                    //j从0到2^(L-1)-1
            {
                p=2*pi/e2;
                w.real = Math.cos(p * j);
                //w.real=Math.cos((double)p*j);                                   //系数W
                w.image = Math.sin(p*j) * -1;
                //w.imag = -sin(p*j);
                for(i=j;i<N;i=i+e2)                                //计算具有相同系数的数据
                {
                    ip=i+B;                                           //对应蝶形的数据间隔为2^(L-1)
                    t=xin[ip].cc(w);
                    xin[ip] = xin[i].cut(t);
                    xin[i] = xin[i].sum(t);
                }
            }
        }
        fftData = new int[xin.length];
        for( i=0;i<xin.length-1;i++ ){
            xin[i].real *= fftScale;
            xin[i].image *= fftScale;

            try {
                fftData[i] = xin[i].getIntValue();
                Log.d("err",i+"");
            }
            catch (Exception e){
                Log.d("err",e.getMessage());
            }

        }
        Log.d("err",fftData.length+"");
    }
    /**
     * assist function :
     * upper2N
     */
    public int to2N( int n ){
        int i = 1;
        while( i<n ){
            i = i<<1;
        }
        i/=2;
        return i;
    }


}

