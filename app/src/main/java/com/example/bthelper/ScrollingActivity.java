package com.example.bthelper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO;

public class ScrollingActivity extends AppCompatActivity implements View.OnClickListener {


    LinearLayout rootLayout;
    Button getMode;
    Button setCommButton;
    Button setNormal;


    Button getSpeaker;
    Button setSpeakerOn;
    Button setSpeakerOff;

    Button getScoOn;
    Button startSco;
    Button stopSco;
    Button setScoOn;
    Button setScoOff;
    Button playMusic;
    Button playVoiceCall;
    Button startRecord;
    Button stopRecord;

    Button getAllStatus;
    Button getHeadsetOn;
    Button getMode2;
    Button getSpeaker2;
    Button getScoOn2;

    TextView tvMode;
    TextView tvRecord;
    TextView tvSco;
    TextView tvSpeaker;
    TextView tvPlay;

    int defaultSampleRateInHz = 16000;
    int defaultAudioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    int defaultChannelConfigIn = AudioFormat.CHANNEL_IN_MONO;
    int defaultChannelConfigOut = AudioFormat.CHANNEL_OUT_MONO;
    int defaultAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int defaultMonoFrameLen = 320;
    AudioManager audioManager;
    final String TAG = "BTHelper";
    Context context;
    boolean isWorking = false;
    MediaPlayer mediaPlayer;
    AudioTrack audioTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        context = ScrollingActivity.this.getBaseContext();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msgMode = "";
                String msgIsSpeakerphoneOn = "";
                String msgIsBluetoothScoOn = "";

                int mode = audioManager.getMode();
                if (mode == AudioManager.MODE_IN_COMMUNICATION) msgMode = "MODE_IN_COMMUNICATION";
                else if (mode == AudioManager.MODE_NORMAL) msgMode = "MODE_NORMAL";
                tvMode.setText(msgMode);

                boolean isSpeakerphoneOn = audioManager.isSpeakerphoneOn();
                if (isSpeakerphoneOn) msgIsSpeakerphoneOn = "扬声器：On";
                else msgIsSpeakerphoneOn = "扬声器：Off";
                tvSpeaker.setText(msgIsSpeakerphoneOn);

                boolean isBluetoothScoOn = audioManager.isBluetoothScoOn();
                if (isBluetoothScoOn) msgIsBluetoothScoOn = "Sco：On";
                else msgIsBluetoothScoOn = "Sco：Off";
                tvSco.setText(msgIsBluetoothScoOn);

                Snackbar.make(rootLayout, "AudioManager Mode：" + msgMode + "\n"
                                + "isBluetoothScoOn：" + isBluetoothScoOn + "\n"
                                + "isSpeakerphoneOn：" + isSpeakerphoneOn + "\n"
                        , Snackbar.LENGTH_SHORT).show();

            }
        });

        fbi();
    }


    public void setMode(int viewId) {
        if (viewId == R.id.setCommButton) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else if (viewId == R.id.setNormal) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }

        updateStatus(R.id.getMode, true);
    }


    public void setSpeaker(int viewId) {
        if (viewId == R.id.setSpeakerOn) {
            audioManager.setSpeakerphoneOn(true);
        } else if (viewId == R.id.setSpeakerOff) {
            audioManager.setSpeakerphoneOn(false);
        }
        updateStatus(R.id.getSpeaker, true);
    }

    public void setScoOn(int viewId) {
        if (viewId == R.id.setScoOn) {
            audioManager.setBluetoothScoOn(true);
        } else if (viewId == R.id.setScoOff) {
            audioManager.setBluetoothScoOn(false);
        }
        updateStatus(R.id.getScoOn, true);
    }


    public void startStopSco(int viewId) {
        if (viewId == R.id.startSco) {
            audioManager.startBluetoothSco();
        } else if (viewId == R.id.stopSco) {
            audioManager.stopBluetoothSco();
        }
        new Timer().schedule(new TimerTask() {
            public void run() {
                runOnUiThread(() -> {
                    updateStatus(R.id.getScoOn, true);
                });
            }
        }, 500);

    }


    public void startStopRecord(int viewId) {
        if (viewId == R.id.startRecord) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                Snackbar.make(rootLayout, "请授予录音和存储权限！", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (isWorking) return;
            isWorking = true;
            new Thread(() -> {
                AudioRecord mAudioRecord;
                int bufferSize = AudioRecord.getMinBufferSize(defaultSampleRateInHz, defaultChannelConfigIn, defaultAudioFormat);
                int frames = bufferSize / defaultMonoFrameLen + 1;
                bufferSize = frames * defaultMonoFrameLen;
                byte[] srcBuffer = new byte[bufferSize];
                byte[] buffer = new byte[defaultMonoFrameLen];

                mAudioRecord = new AudioRecord(defaultAudioSource, defaultSampleRateInHz, defaultChannelConfigIn, defaultAudioFormat, bufferSize);
                for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS))
                    Log.e(TAG, "所有的音频输入设备：" + device.getProductName() + " type:" + device.getType() + " id:" + device.getId());
                for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                    if (device.getType() == TYPE_BLUETOOTH_SCO) {
                        Log.e(TAG, "设置SCO为首选音频输入设备");
                        if (mAudioRecord != null) {
                            boolean status = mAudioRecord.setPreferredDevice(device);
                            if (status) Log.e(TAG, "设置成功");
                            else Log.e(TAG, "设置失败");
                        }
                        break;
                    }
                }
                if (mAudioRecord != null) {
                    SaveManager.getInstance(context).open();
                    mAudioRecord.startRecording();
                }
                while (isWorking) {
                    int bufferReadResult = mAudioRecord.read(srcBuffer, 0, bufferSize);
                    if (bufferReadResult != bufferSize) {
                        Log.e(TAG, "HeadSet 录音失败!");
                        break;
                    }
                    for (int k = 0; k < frames; k++) {
                        System.arraycopy(srcBuffer, k * defaultMonoFrameLen, buffer, 0, defaultMonoFrameLen);
                        Log.e(TAG, "录音Buffer：" + Arrays.toString(buffer));
                        SaveManager.getInstance(context).write(buffer);
                    }
                }
                mAudioRecord.release();
                Log.e(TAG, "录音已停止");
            }).start();
            updateStatus(R.id.startRecord, true);
        } else if (viewId == R.id.stopRecord) {
            SaveManager.getInstance(context).close();
            isWorking = false;
            updateStatus(R.id.stopRecord, true);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.setCommButton:
            case R.id.setNormal:
                setMode(id);
                break;

            case R.id.setSpeakerOn:
            case R.id.setSpeakerOff:
                setSpeaker(id);
                break;

            case R.id.setScoOn:
            case R.id.setScoOff:
                setScoOn(id);
                break;

            case R.id.startSco:
            case R.id.stopSco:
                startStopSco(id);
                break;

            case R.id.startRecord:
            case R.id.stopRecord:
                startStopRecord(id);
                break;


            case R.id.playMusic:
            case R.id.playVoiceCall:
                startPlay(id);
                break;

            case R.id.getMode:
            case R.id.getMode2:
            case R.id.getSpeaker:
            case R.id.getSpeaker2:
            case R.id.getScoOn:
            case R.id.getScoOn2:
                updateStatus(id, true);
                break;
        }
    }


    private void startPlay(int id) {
        byte[] sound = SaveManager.getInstance(context).getByteData(context.getFilesDir() + "/record.pcm");
        if (sound == null) {
            Snackbar.make(rootLayout, "没有录音数据", Snackbar.LENGTH_SHORT).show();
            return;
        }
        int bufferSize = AudioTrack.getMinBufferSize(defaultSampleRateInHz, defaultChannelConfigOut, defaultAudioFormat);
        if (audioTrack != null) {
            Snackbar.make(rootLayout, "请等待播放完成", Snackbar.LENGTH_SHORT);
            return;
        }
        int streamType = id == R.id.playMusic ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_VOICE_CALL;
        audioTrack = new AudioTrack(streamType, defaultSampleRateInHz, defaultChannelConfigOut, defaultAudioFormat,
                bufferSize, AudioTrack.MODE_STREAM);
        int audioLen = sound.length / 2;
        Log.e(TAG, "AudioTrack 开始播放");
        tvPlay.setText("正在播放");
        audioTrack.setNotificationMarkerPosition(audioLen);
        audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
                Log.e(TAG, "AudioTrack 播放完成");
                tvPlay.setText("播放完成");
                audioTrack = null;
            }

            @Override
            public void onPeriodicNotification(AudioTrack audioTrack) {

            }
        });
        audioTrack.play();
        new Thread(() -> audioTrack.write(sound, 0, sound.length)).start();
    }

    private void startPlay2() {
        new Thread(() -> {
            try {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    Snackbar.make(rootLayout, "请等待播放完成", Snackbar.LENGTH_SHORT);
                    return;
                }
                Log.e(TAG, "开始播放");
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(context.getFilesDir() + "/record.wav");
                mediaPlayer.prepare();
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (mediaPlayer == null)
                            return;
                        Log.e(TAG, "播放完成");
                    }
                });
            } catch (Exception e) {
            }
        }).start();
    }


    public void updateStatus(int id, boolean showSnackBar) {
        String msg = "";
        if (id == R.id.getMode || id == R.id.getMode2) {
            int mode = audioManager.getMode();
            if (mode == AudioManager.MODE_IN_COMMUNICATION) msg = "MODE_IN_COMMUNICATION";
            else if (mode == AudioManager.MODE_NORMAL) msg = "MODE_NORMAL";
            tvMode.setText(msg);
        } else if (id == R.id.getSpeaker || id == R.id.getSpeaker2) {
            boolean mode = audioManager.isSpeakerphoneOn();
            if (mode) msg = "扬声器：On";
            else msg = "扬声器：Off";
            tvSpeaker.setText(msg);
        } else if (id == R.id.getScoOn || id == R.id.getScoOn2) {
            boolean mode = audioManager.isBluetoothScoOn();
            if (mode) msg = "Sco：On";
            else msg = "Sco：Off";
            tvSco.setText(msg);
        } else if (id == R.id.startRecord || id == R.id.stopRecord) {
            if (isWorking) msg = "正在录音";
            else msg = "录音已停止";
            tvRecord.setText(msg);
        }
        if (showSnackBar)
            Snackbar.make(rootLayout, msg, Snackbar.LENGTH_SHORT);
    }

    public void fbi() {
        rootLayout = findViewById(R.id.linearLayout);

        getMode = findViewById(R.id.getMode);
        setCommButton = findViewById(R.id.setCommButton);
        setNormal = findViewById(R.id.setNormal);

        getSpeaker = findViewById(R.id.getSpeaker);
        setSpeakerOn = findViewById(R.id.setSpeakerOn);
        setSpeakerOff = findViewById(R.id.setSpeakerOff);

        getScoOn = findViewById(R.id.getScoOn);
        startSco = findViewById(R.id.startSco);
        stopSco = findViewById(R.id.stopSco);
        setScoOn = findViewById(R.id.setScoOn);
        setScoOff = findViewById(R.id.setScoOff);
        playMusic = findViewById(R.id.playMusic);
        playVoiceCall = findViewById(R.id.playVoiceCall);
        startRecord = findViewById(R.id.startRecord);
        stopRecord = findViewById(R.id.stopRecord);
        getAllStatus = findViewById(R.id.getAllStatus);
        getHeadsetOn = findViewById(R.id.getHeadsetOn);
        getMode2 = findViewById(R.id.getMode2);
        getSpeaker2 = findViewById(R.id.getSpeaker2);
        getScoOn2 = findViewById(R.id.getScoOn2);
        tvMode = findViewById(R.id.tvMode);
        tvRecord = findViewById(R.id.tvRecord);
        tvSco = findViewById(R.id.tvSco);
        tvSpeaker = findViewById(R.id.tvSpeaker);
        tvPlay = findViewById(R.id.tvPlay);

        getMode.setOnClickListener(this);
        setCommButton.setOnClickListener(this);
        setNormal.setOnClickListener(this);
        getSpeaker.setOnClickListener(this);
        setSpeakerOn.setOnClickListener(this);
        setSpeakerOff.setOnClickListener(this);
        getScoOn.setOnClickListener(this);
        startSco.setOnClickListener(this);
        stopSco.setOnClickListener(this);
        setScoOn.setOnClickListener(this);
        setScoOff.setOnClickListener(this);
        playMusic.setOnClickListener(this);
        playVoiceCall.setOnClickListener(this);
        startRecord.setOnClickListener(this);
        stopRecord.setOnClickListener(this);
        getAllStatus.setOnClickListener(this);
        getHeadsetOn.setOnClickListener(this);
        getMode2.setOnClickListener(this);
        getSpeaker2.setOnClickListener(this);
        getScoOn2.setOnClickListener(this);
    }

}