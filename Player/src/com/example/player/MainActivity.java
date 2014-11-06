package com.example.player;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
public class MainActivity extends Activity {
	private ImageButton imgBtn_Previous;
    private ImageButton imgBtn_PlayOrPause;
    private ImageButton imgBtn_Stop;
    private ImageButton imgBtn_Next;
    private ListView list;
    private TextView text_Current;
    private TextView text_Duration;
    private SeekBar seekBar;
    //更新进度条的Handle
    //qqqqqqqq
    private Handler seekBarHandler;
    //当前歌曲的持续时间和当前位置，作用于进度条
    private int duration;
    private int time;
    //进度条控制常量
    private static final int PROGRESS_INCREASE = 0;
    private static final int PROGRESS_PAUSE = 1;
    private static final int PROGRESS_RESET = 2;
    //播放状态
    private int status;
    // 当前歌曲的序号，下标从1开始
    private int number;
    // 广播接收器
    private StatusChangedReceiver receiver;
    private RelativeLayout root_Layout;
    
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findView();
		registerListeners();
		number = 1;
		status = MusicService.STATUS_STOPPED;
		duration = 0;
		time = 0;
		startService(new Intent(this, MusicService.class));
		//绑定广播接收器，可以接收广播
		bingStatusChangedReceiver();
		//检查播放器是否正在播放。如果正在播放，以上绑定的接收器会改变UI
		sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
		initSeekBarHandler();
	}
	private void bingStatusChangedReceiver(){
		receiver = new StatusChangedReceiver();
		IntentFilter filter = new IntentFilter(
				MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
		registerReceiver(receiver,filter);
	}
	
	private void findView(){
	    imgBtn_Previous = (ImageButton) findViewById(R.id.imageButton1);
	    imgBtn_PlayOrPause =(ImageButton) findViewById(R.id.imageButton2);
	    imgBtn_Stop = (ImageButton) findViewById(R.id.imageButton3);
	    imgBtn_Next = (ImageButton) findViewById(R.id.imageButton4);
	    list = (ListView) findViewById(R.id.listView1);
	    seekBar = (SeekBar) findViewById(R.id.seekBar1);
	    text_Current = (TextView) findViewById(R.id.textView1);
	    text_Duration = (TextView) findViewById(R.id.textView2);
		root_Layout = (RelativeLayout) findViewById(R.id.relativeLayout1);	    
	}
	
	private void registerListeners(){
        imgBtn_Previous.setOnClickListener(new OnClickListener(){
	        public void onClick(View view){
               sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
	        }
        });
        imgBtn_PlayOrPause.setOnClickListener(new OnClickListener(){
	        public void onClick(View view){
	        	if(isPlaying()){
	        		sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
	        	}else if(isPaused()){
	        		sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
	        	}else if(isStopped()){
	        		sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
	        	}
	        }
        });
        imgBtn_Stop.setOnClickListener(new OnClickListener(){
        	public void onClick(View view){
        		sendBroadcastOnCommand(MusicService.COMMAND_STOP);
        	}
        });
        imgBtn_Next.setOnClickListener(new OnClickListener(){
        	public void onClick(View view){
        		sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
        	}
        });
        list.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> parent, View view,
        			int position, long id) {
        		number = position + 1;
        		sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
        	}
        });
    	seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// 发送广播给MusicService，执行跳转
				sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
				if(isPlaying()) {
					//进度条恢复移动
					seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
				}
				
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// 进度条暂停移动
				seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
				
			}
			@Override
			public void onProgressChanged(SeekBar seekbar,int progress,
					boolean fromUser) {
				time = progress;
				// //更新文本
				text_Current.setText(formatTime(time));
				
			}
    	});
    }
	
	protected void onResume() {
		super.onResume();
		initMusicList();
		if(list.getCount() == 0){
			imgBtn_Previous.setEnabled(false);
		    imgBtn_PlayOrPause.setEnabled(false);
		    imgBtn_Stop.setEnabled(false);
		    imgBtn_Next.setEnabled(false);
            Toast.makeText(this, this.getString(R.string.tip_no_music_file),
            		Toast.LENGTH_SHORT).show();
		}else{
			imgBtn_Previous.setEnabled(true);
		    imgBtn_PlayOrPause.setEnabled(true);
		    imgBtn_Stop.setEnabled(true);
		    imgBtn_Next.setEnabled(true);

		}
		PropertyBean property = new PropertyBean(MainActivity.this);
		String theme = property.getTheme();
	}
	/**初始化音乐列表。包括获取音乐集和更新显示列表*/
	private void initMusicList(){
		Cursor cursor = getMusicCursor();
		setListContent(cursor);
	}
	/**更新列表的内容*/
	private void setListContent(Cursor musicCursor){
		CursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_2,musicCursor,new String[]{
		               MediaStore.Audio.AudioColumns.TITLE,	
		               MediaStore.Audio.AudioColumns.ARTIST },new int[]{
				       android.R.id.text1, android.R.id.text2 });
		list.setAdapter(adapter);
	}
	
	private Cursor getMusicCursor(){
		ContentResolver resolver = getContentResolver();
	    Cursor cursor = resolver.query(
	    		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, 
	    		null);
		return cursor;
		}
	
	
    

	private void moveNumberToNext(){
		if((number + 1) > list.getCount()){
			number =1;
			Toast.makeText(MainActivity.this, 
					MainActivity.this.getString(R.string.tip_reach__bottom)
					, Toast.LENGTH_SHORT).show();
		}else{
			++number;
		}
	}
	private void moveNumberToPrevious(){
		if(number == 1){
			number = list.getCount();
			Toast.makeText(MainActivity.this,
					MainActivity.this.getString(R.string.tip_reach_top),
					Toast.LENGTH_SHORT).show();
		}else{
			--number;
		}
	}
	
	/** 发送命令。控制音乐播放。参数定义在MusicService类中*/
	private void sendBroadcastOnCommand(int command) {
		Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
		intent.putExtra("command", command);
		//根据不同命令，封装不同数据
		switch (command) {
		case MusicService.COMMAND_PLAY:
		     intent.putExtra("number", number);
		     break;
		     
		case MusicService.COMMAND_PREVIOUS:
		     moveNumberToPrevious();
		     intent.putExtra("number", number);
		     break;
		
		case MusicService.COMMAND_NEXT:
		     moveNumberToNext();
		     intent.putExtra("number", number);
		     break;
		case MusicService.COMMAND_SEEK_TO:
			intent.putExtra("time", time);
		case MusicService.COMMAND_PAUSE:
		case MusicService.COMMAND_STOP:
		case MusicService.COMMAND_RESUME:
		default:
			break;
		}
		sendBroadcast(intent);
	}
	/** 是否正在播放音乐*/
	private boolean isPlaying(){
		return status == MusicService.STATUS_PLAYING;
	}
	/** 是否暂停了播放音乐*/
	private boolean isPaused(){
		return status == MusicService.STATUS_PAUSED;
	}
	/** 是否停止状态*/
    private boolean isStopped(){
    	return status == MusicService.STATUS_STOPPED;
    }
    
    class StatusChangedReceiver extends BroadcastReceiver{
		public void onReceive(Context context, Intent intent) {
			// 获取播放器状态
			status = intent.getIntExtra("status", -1);
			switch (status) {
			case MusicService.STATUS_PLAYING:
				time = intent.getIntExtra("time", 0);
				duration = intent.getIntExtra("duration", 0);
				seekBarHandler.removeMessages(PROGRESS_INCREASE);
				seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
				seekBar.setMax(duration);
				seekBar.setProgress(time);
				text_Duration.setText(formatTime(duration));
				imgBtn_PlayOrPause.setBackgroundResource(R.drawable.pause);
				Cursor cursor = MainActivity.this.getMusicCursor();
				cursor.moveToPosition(number - 1);
				String title = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
				MainActivity.this.setTitle("正在播放：" + title + "-GraeceP;ayer");
				break;
			case MusicService.STATUS_PAUSED:
				seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
				imgBtn_PlayOrPause.setBackgroundResource(R.drawable.play);
				break;
			case MusicService.STATUS_STOPPED:
				seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
				imgBtn_PlayOrPause.setBackgroundResource(R.drawable.play);
				break;
			case MusicService.STATUS_COMPLETED:
				sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
				seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
				imgBtn_PlayOrPause.setBackgroundResource(R.drawable.play);
				break;
				default:
					break;
			}
			//如果播放结束，播放下一曲
			if(status == MusicService.STATUS_COMPLETED){
				sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
			}
			//更新UI
			updateUI(status);
		}
		/**根据播放器的播放状态，更新UI */
		private void updateUI(int status){
			switch (status){
			case MusicService.STATUS_PLAYING:
				imgBtn_PlayOrPause.setBackgroundResource(R.drawable.play);
				break;
			default:
				break;
			}
		}
    	
    }
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	protected void onDestroy(){
		if (isStopped()){
			stopService(new Intent(this, MusicService.class));
		}
		super.onDestroy();
	}
	private String formatTime(int msec){
		int minute = (msec / 1000) / 60;
		int second = (msec / 1000) % 60;
		String minuteString;
		String secondString;
		if(minute < 10){
			minuteString = "0" + minute;
		}else{
			minuteString = "" + minute;
		}
		if(second < 10){
			secondString = "0" + second;
		}else{
			secondString = "" + second;
		}
		return minuteString + ":" +secondString;
	}
	private void initSeekBarHandler(){
		seekBarHandler = new Handler(){
			public void handleMessage(Message msg){
				super.handleMessage(msg);
				switch (msg.what) {
				case PROGRESS_INCREASE:
					if(seekBar.getProgress() < duration) {
						//进度条前进1秒
						seekBar.incrementProgressBy(1000);
						seekBarHandler.sendEmptyMessageDelayed(
								PROGRESS_INCREASE, 1000);
						//修改显示当前进度的文本
						text_Current.setText(formatTime(time));
						time += 1000;
					}
					break;
				case PROGRESS_PAUSE:
					seekBarHandler.removeMessages(PROGRESS_INCREASE);
					break;
				case PROGRESS_RESET:
					//重置进度条界面
					seekBarHandler.removeMessages(PROGRESS_INCREASE);
					seekBar.setProgress(0);
				    text_Current.setText("00:00");
				    break;
				}
			}

		};
	}
	//Menu常量
	public static final int MENU_THEME = Menu.FIRST;
	public static final int MENU_ABOUT = Menu.FIRST + 1;
	/**创建菜单*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		menu.add(0, MENU_THEME, 0, "主题");
		menu.add(0, MENU_ABOUT, 1, "关于");
		return super.onCreateOptionsMenu(menu);
	}
	/**处理菜单点击事件*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case MENU_THEME:
			//显示列表对话框
			new AlertDialog.Builder(this)
				.setTitle("选择主题")
				.setItems(R.array.theme,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
										//获取在array.xml中定义的主题名称
										String theme = PropertyBean.THEMES[which];
										//设置Activity主题
										setTheme(theme);
										//保存选择主题
										PropertyBean property = new PropertyBean(
												MainActivity.this);
										property.setAndSaveTheme(theme);
												}
								}).show();
					break;
		case MENU_ABOUT:
			//显示文本对话框
			new AlertDialog.Builder(MainActivity.this).setTitle("简约")
			.setMessage(MainActivity.this.getString(R.string.about)).show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	/**设置Activity的主题，包括修改背景图片等*/
	private void setTheme(String theme){
		if ("波浪".equals(theme)){
			root_Layout.setBackgroundResource(R.drawable.bg_bl);	 		
		}else if ("衣服".equals(theme)){
			root_Layout.setBackgroundResource(R.drawable.bg_fs);	
		}else if ("叶子".equals(theme)){
			root_Layout.setBackgroundResource(R.drawable.bg_color);
		}
}
}
