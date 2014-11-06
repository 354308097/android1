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
	//播放控制命令，表示操作
	public static final int COMMAND_UNKNOWN = -1;
	public static final int COMMAND_PLAY = 0;
	public static final int COMMAND_PAUSE = 1;
	public static final int COMMAND_STOP = 2;
	public static final int COMMAND_RESUME = 3;
	public static final int COMMAND_PREVIOUS = 4;
	public static final int COMMAND_NEXT = 5 ;
	public static final int COMMAND_CHECK_IS_PLAYING = 6;
	public static final int COMMAND_SEEK_TO = 7;
	//播放器状态
	public static final int STATUS_PLAYING = 0;
	public static final int STATUS_PAUSED = 1;
	public static final int STATUS_STOPPED = 2;
	public static final int STATUS_COMPLETED = 3;
	//广播标识
	public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
	public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS ="MusicService.ACTION_UPDATE";
	//广播接收器
	private CommandReceiver receiver;
	  /** 内部类，接收广播命令，并执行操作*/
	public void onCreate(){
		super.onCreate();
		//绑定广播接收器，可以接受广播
		bindCommandReceiver();
		Toast.makeText(this, "MusicService.onCreate()", Toast.LENGTH_SHORT)
		.show();
	}
	public void onStart(Intent intent, int startId){
		super.onStart(intent,startId);	
		}
	public void onDestroy(){
		//释放播放器资源
		if(player != null){
			player.release();
		}
		super.onDestroy();
	}
    class CommandReceiver extends BroadcastReceiver{    	
		@Override
		public void onReceive(Context context, Intent intent) {
			//获得命令
			int command = intent.getIntExtra("command", COMMAND_UNKNOWN);
			//执行命令
		    switch (command){
		    case COMMAND_SEEK_TO:
		    	seekTo(intent.getIntExtra("time", 0));
		    	break;
		    case COMMAND_PLAY:
		    case COMMAND_PREVIOUS:
		    case COMMAND_NEXT:
		    	int number = intent.getIntExtra("number", 1);
		    	Toast.makeText(MusicService.this, "正在播放第" + number + "首", 
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
    /** 绑定广播接收器*/
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
    /** 读取音乐文件*/
    private void load(int number){
		//之前的资源不用，释放掉
    	if(player != null){
			player.release();
		}
		Uri musicUri = Uri.withAppendedPath(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + number);
	    //读取音乐文件，创建MediaPlayer对象
		player = MediaPlayer.create(this, musicUri);
		//注册监听器
		player.setOnCompletionListener(completionListener);
	}
	
    //播放结束监听器
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
		//停止当前播放
		if(player != null && player.isPlaying()){
			player.stop();
		}
		load(number);
		player.start();
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
	/**暂停音乐*/
	private void pause(){
		if (player.isPlaying()) {
			player.pause();
			sendBroadcastOnStatusChanged(MusicService.STATUS_PAUSED);
		}
	}
	/**停止播放*/
	private void stop(){
		if(player != null){
			player.stop();
			sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
		}
	}
	/**恢复播放（停止之后）*/
	private void resume(){
		player.start();
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
	/**重新播放（播放完成之后）*/
	private void replay(){
		player.start();
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
	/** 跳转至播放位置*/
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
