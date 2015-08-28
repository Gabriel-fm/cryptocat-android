package net.dirbaio.cryptocat.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import net.dirbaio.cryptocat.ExceptionRunnable;
import net.dirbaio.cryptocat.MainActivity;
import net.dirbaio.cryptocat.R;
import net.dirbaio.cryptocat.serverlist.ServerConfig;
import net.dirbaio.cryptocat.serverlist.ServerList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CryptocatService extends Service implements CryptocatStateListener
{

	private static CryptocatService instance;

	public static CryptocatService getInstance()
	{
		return instance;
	}

	private Looper serviceLooper;
	private Handler serviceHandler;
	private Handler uiHandler;
	private final Map<String, CryptocatServer> servers = new HashMap<>();
	private final List<CryptocatStateListener> listeners = new ArrayList<>();

	// Binder given to clients
	private final IBinder binder = new CryptocatBinder();

    public ServerList serverList = new ServerList();

	public boolean hasServers()
	{
		return !servers.isEmpty();
	}

	/**
	 * Class used for the client Binder.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class CryptocatBinder extends Binder
	{
		public CryptocatService getService()
		{
			// Return this instance of LocalService so clients can call public methods
			return CryptocatService.this;
		}
	}

	//State listener stuff.
	public void addStateListener(CryptocatStateListener listener)
	{
		listeners.add(listener);
	}

	public void removeStateListener(CryptocatStateListener listener)
	{
		listeners.remove(listener);
	}

	@Override
	public void stateChanged()
	{
		for (CryptocatStateListener l : listeners)
			l.stateChanged();
	}

	@Override
	public void onCreate()
	{
		instance = this;

		HandlerThread thread = new HandlerThread("ServiceStartArguments",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		serviceLooper = thread.getLooper();
		serviceHandler = new Handler(serviceLooper);

		// Create a handler to run stuff on the UI thread.
		uiHandler = new Handler();

        //Load server list
        serverList = new ServerList();
        serverList.load(this);
	}

	@Override
	public void onDestroy()
	{
		serviceLooper.quit();

		instance = null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Intent notificationIntent = new Intent(this, MainActivity.class);
		//TODO: If new activities are created, they may move below the main activity  as it is SingleTop
		//Maybe should be controlled under the onNewIntent in the Main Activity.
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		Notification notification = new NotificationCompat.Builder(this.getApplicationContext())
				.setContentTitle(getText(R.string.ticker_text))
                .setContentText(getText(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_launcher)
				.setContentIntent(pendingIntent)
				.getNotification();

		startForeground(1, notification);

		return START_STICKY;
	}

	public IBinder onBind(Intent intent)
	{
		return binder;
	}

	//CryptocatServer stuff.

	public CryptocatServer createServer(ServerConfig config)
	{
		CryptocatServer s = new CryptocatServer(config);
		if(servers.containsKey(s.id))
			throw new AlreadyConnectedException();
		servers.put(s.id, s);
		s.addStateListener(this);
		stateChanged();
		return s;
	}

	public CryptocatServer getServer(String id)
	{
		return servers.get(id);
	}

	public void removeServer(String id)
	{
		if (getServer(id).getState() != CryptocatServer.State.Disconnected)
			throw new IllegalStateException("Server must be disconnected");

		CryptocatServer s = getServer(id);
		s.removeStateListener(this);
		servers.remove(id);

		stateChanged();
	}

	public void getConversationList(List<Object> list)
	{
		list.clear();
		for (CryptocatServer s : servers.values())
		{
			list.add(s);
			for (MultipartyConversation c : s.conversations.values())
				list.add(c);
		}
	}

	void post(final ExceptionRunnable r)
	{
		serviceHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					r.run();
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	void post(final Runnable r)
	{
		serviceHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					r.run();
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}


	void uiPost(final ExceptionRunnable r)
	{
		uiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					r.run();
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	void uiPost(final Runnable r)
	{
		uiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					r.run();
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}
}
