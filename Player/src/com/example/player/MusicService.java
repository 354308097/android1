package com.example.player;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.Toast;

public class MusicService extends Service{
	//���ſ��������ʾ����
	public static final int COMMAND_UNKNOWN = -1;
	public static final int COMMAND_PLAY = 0;
	public static final int COMMAND_PAUSE = 1;
	public static final int COMMAND_STOP = 2;
	public static final int COMMAND_RESUME = 3;
	public static final int COMMAND_PREVIOUS = 4;
	public static final int COMMAND_NEXT = 5 ;
	public static final int COMMAND_CHECK_IS_PLAYING = 6;
	public static final int COMMAND_SEEK_TO = 7;
	//������״̬
	public static final int STATUS_PLAYING = 0;
	public static final int STATUS_PAUSED = 1;
	public static final int STATUS_STOPPED = 2;
	public static final int STATUS_COMPLETED = 3;
	//�㲥��ʶ
	public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
	public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS ="MusicService.ACTION_UPDATE";
	//�㲥������
	private CommandReceiver receiver;
	  /** �ڲ��࣬���չ㲥�����ִ�в���*/
	public void onCreate(){
		super.onCreate();
		//�󶨹㲥�����������Խ��ܹ㲥
		bindCommandReceiver();
		Toast.makeText(this, "MusicService.onCreate()", Toast.LENGTH_SHORT)
		.show();
	}
	public void onStart(Intent intent, int startId){
		super.onStart(intent,startId);	
		}
	public void onDestroy(){
		//�ͷŲ�������Դ
		if(player != null){
			player.release();
		}
		super.onDestroy();
	}
    class CommandReceiver extends BroadcastReceiver{    	
		@Override
		public void onReceive(Context context, Intent intent) {
			//�������
			int command = intent.getIntExtra("command", COMMAND_UNKNOWN);
			//ִ������
		    switch (command){
		    case COMMAND_SEEK_TO:
		    	seekTo(intent.getIntExtra("time", 0));
		    	break;
		    case COMMAND_PLAY:
		    case COMMAND_PREVIOUS:
		    case COMMAND_NEXT:
		    	int number = intent.getIntExtra("number", 1);
		    	Toast.makeText(MusicService.this, "���ڲ��ŵ�" + number + "��", 
		    			Toast.LENGTH_SHORT).show();
		    	play(number);
		    	break;
		    case COMMAND_PAUSE:
		    	pause();
		    	break;
		    case COMMAND_STOP:
		    	stop();
		    	break;
		    case COMMAND_RESUME:
		    	resume();
		    	break;
		    case COMMAND_CHECK_IS_PLAYING:
		    	if (player.isPlaying()){
		    		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
		    	}
		    	break;
		    case COMMAND_UNKNOWN:
		    	default:
		    		break;
		    }
		}
    }
    /** �󶨹㲥������*/
    private void bindCommandReceiver() {
    	receiver = new CommandReceiver();
    	IntentFilter filter = new IntentFilter(BROADCAST_MUSICSERVICE_CONTROL );
    	registerReceiver(receiver,filter);
    }
    private void sendBroadcastOnStatusChanged(int status){
    	Intent intent = new Intent(BROADCAST_MUSICSERVICE_UPDATE_STATUS);
    	intent.putExtra("status",status);
    	if(status == STATUS_PLAYING){
    		intent.putExtra("time", player.getCurrentPosition());
    		intent.putExtra("duration", player.getDuration());
    	}
    	sendBroadcast(intent);
    }
    private MediaPlayer player;
    /** ��ȡ�����ļ�*/
    private void load(int number){
		//֮ǰ����Դ���ã��ͷŵ�
    	if(player != null){
			player.release();
		}
		Uri musicUri = Uri.withAppendedPath(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + number);
	    //��ȡ�����ļ�������MediaPlayer����
		player = MediaPlayer.create(this, musicUri);
		//ע�������
		player.setOnCompletionListener(completionListener);
	}
	
    //���Ž���������
    OnCompletionListener completionListener = new OnCompletionListener(){
    	public void onCompletion(MediaPlayer player){
    		if(player.isLooping()){
    			replay();
    		}else{
    			sendBroadcastOnStatusChanged(MusicService.STATUS_COMPLETED);
    		}
    	}
    };
    
	private void play(int number){
		//ֹͣ��ǰ����
		if(player != null && player.isPlaying()){
			player.stop();
		}
		load(number);
		player.start();
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
	/**��ͣ����*/
	private void pause(){
		if (player.isPlaying()) {
			player.pause();
			sendBroadcastOnStatusChanged(MusicService.STATUS_PAUSED);
		}
	}
	/**ֹͣ����*/
	private void stop(){
		if(player != null){
			player.stop();
			sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
		}
	}
	/**�ָ����ţ�ֹ֮ͣ��*/
	private void resume(){
		player.start();
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
	/**���²��ţ��������֮��*/
	private void replay(){
		player.start();
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
	/** ��ת������λ��*/
	private void seekTo(int time){
		if(player != null){
			player.seekTo(time);
		}
	}
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
