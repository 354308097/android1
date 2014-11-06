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
    //���½�������Handle
    //qqqqqqqq
    private Handler seekBarHandler;
    //��ǰ�����ĳ���ʱ��͵�ǰλ�ã������ڽ�����
    private int duration;
    private int time;
    //���������Ƴ���
    private static final int PROGRESS_INCREASE = 0;
    private static final int PROGRESS_PAUSE = 1;
    private static final int PROGRESS_RESET = 2;
    //����״̬
    private int status;
    // ��ǰ��������ţ��±��1��ʼ
    private int number;
    // �㲥������
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
		//�󶨹㲥�����������Խ��չ㲥
		bingStatusChangedReceiver();
		//��鲥�����Ƿ����ڲ��š�������ڲ��ţ����ϰ󶨵Ľ�������ı�UI
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
				// ���͹㲥��MusicService��ִ����ת
				sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
				if(isPlaying()) {
					//�������ָ��ƶ�
					seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
				}
				
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// ��������ͣ�ƶ�
				seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
				
			}
			@Override
			public void onProgressChanged(SeekBar seekbar,int progress,
					boolean fromUser) {
				time = progress;
				// //�����ı�
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
	/**��ʼ�������б�������ȡ���ּ��͸�����ʾ�б�*/
	private void initMusicList(){
		Cursor cursor = getMusicCursor();
		setListContent(cursor);
	}
	/**�����б������*/
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
	
	/** ��������������ֲ��š�����������MusicService����*/
	private void sendBroadcastOnCommand(int command) {
		Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
		intent.putExtra("command", command);
		//���ݲ�ͬ�����װ��ͬ����
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
	/** �Ƿ����ڲ�������*/
	private boolean isPlaying(){
		return status == MusicService.STATUS_PLAYING;
	}
	/** �Ƿ���ͣ�˲�������*/
	private boolean isPaused(){
		return status == MusicService.STATUS_PAUSED;
	}
	/** �Ƿ�ֹͣ״̬*/
    private boolean isStopped(){
    	return status == MusicService.STATUS_STOPPED;
    }
    
    class StatusChangedReceiver extends BroadcastReceiver{
		public void onReceive(Context context, Intent intent) {
			// ��ȡ������״̬
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
				MainActivity.this.setTitle("���ڲ��ţ�" + title + "-GraeceP;ayer");
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
			//������Ž�����������һ��
			if(status == MusicService.STATUS_COMPLETED){
				sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
			}
			//����UI
			updateUI(status);
		}
		/**���ݲ������Ĳ���״̬������UI */
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
						//������ǰ��1��
						seekBar.incrementProgressBy(1000);
						seekBarHandler.sendEmptyMessageDelayed(
								PROGRESS_INCREASE, 1000);
						//�޸���ʾ��ǰ���ȵ��ı�
						text_Current.setText(formatTime(time));
						time += 1000;
					}
					break;
				case PROGRESS_PAUSE:
					seekBarHandler.removeMessages(PROGRESS_INCREASE);
					break;
				case PROGRESS_RESET:
					//���ý���������
					seekBarHandler.removeMessages(PROGRESS_INCREASE);
					seekBar.setProgress(0);
				    text_Current.setText("00:00");
				    break;
				}
			}

		};
	}
	//Menu����
	public static final int MENU_THEME = Menu.FIRST;
	public static final int MENU_ABOUT = Menu.FIRST + 1;
	/**�����˵�*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		menu.add(0, MENU_THEME, 0, "����");
		menu.add(0, MENU_ABOUT, 1, "����");
		return super.onCreateOptionsMenu(menu);
	}
	/**����˵�����¼�*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case MENU_THEME:
			//��ʾ�б�Ի���
			new AlertDialog.Builder(this)
				.setTitle("ѡ������")
				.setItems(R.array.theme,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
										//��ȡ��array.xml�ж������������
										String theme = PropertyBean.THEMES[which];
										//����Activity����
										setTheme(theme);
										//����ѡ������
										PropertyBean property = new PropertyBean(
												MainActivity.this);
										property.setAndSaveTheme(theme);
												}
								}).show();
					break;
		case MENU_ABOUT:
			//��ʾ�ı��Ի���
			new AlertDialog.Builder(MainActivity.this).setTitle("��Լ")
			.setMessage(MainActivity.this.getString(R.string.about)).show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	/**����Activity�����⣬�����޸ı���ͼƬ��*/
	private void setTheme(String theme){
		if ("����".equals(theme)){
			root_Layout.setBackgroundResource(R.drawable.bg_bl);	 		
		}else if ("�·�".equals(theme)){
			root_Layout.setBackgroundResource(R.drawable.bg_fs);	
		}else if ("Ҷ��".equals(theme)){
			root_Layout.setBackgroundResource(R.drawable.bg_color);
		}
}
}
