package net.dirbaio.cryptocat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import net.dirbaio.cryptocat.service.CryptocatService;

public class MainActivity extends SherlockFragmentActivity implements BaseFragment.Callbacks, SlidingMenu.OnOpenListener, SlidingMenu.OnCloseListener
{

	public static final String ARG_SERVER_ID = "net.dirbaio.cryptocat.SERVER_ID";
	public static final String ARG_CONVERSATION_ID = "net.dirbaio.cryptocat.CONVERSATION_ID";
	public static final String ARG_BUDDY_ID = "net.dirbaio.cryptocat.BUDDY_ID";
	public static final String STATE_MENU_SHOWING = "net.dirbaio.cryptocat.MENU_SHOWING";
	public static final String STATE_SECONDARY_MENU_SHOWING = "net.dirbaio.cryptocat.SECONDARY_MENU_SHOWING";
	public static final String STATE_SELECTED_SERVER = "net.dirbaio.cryptocat.SELECTED_SERVER";
	public static final String STATE_SELECTED_CONVERSATION = "net.dirbaio.cryptocat.SELECTED_CONVERSATION";
	public static final String STATE_SELECTED_BUDDY = "net.dirbaio.cryptocat.SELECTED_BUDDY";

	private SlidingMenu sm;

	public BaseFragment currCenterFragment;
	public BaseFragment currRightFragment;
	private ConversationListFragment conversationList;
	private String selectedServer, selectedConversation, selectedBuddy;

	private boolean bound = false;
	private Handler handler;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

        //Apply PRNG fixes
        PRNGFixes.apply();

		//Start the service if it isn't already started.
		//startService(new Intent(this, CryptocatService.class));

		handler = new Handler();

		//Main content view
		setContentView(R.layout.frame_center);

		// customize the SlidingMenu
		sm = new SlidingMenu(this);

		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.shadow);
		sm.setSecondaryShadowDrawable(R.drawable.shadowright);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setFadeDegree(0.35f);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		sm.setMode(SlidingMenu.LEFT_RIGHT);

		sm.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);

		//SlidingMenu left view (conversation list)
		sm.setMenu(R.layout.frame_left);

		//SlidingMenu right view (user list)
		sm.setSecondaryMenu(R.layout.frame_right);

		sm.setOnOpenListener(this);
		sm.setOnCloseListener(this);
		sm.setSecondaryOnOpenListner(this)
		;
		//Restore instance state.
		if(savedInstanceState != null)
		{
			if(savedInstanceState.getBoolean(STATE_SECONDARY_MENU_SHOWING))
				sm.showSecondaryMenu(false);
			else if(savedInstanceState.getBoolean(STATE_MENU_SHOWING))
				sm.showMenu(false);

			selectedServer = savedInstanceState.getString(STATE_SELECTED_SERVER);
			selectedConversation = savedInstanceState.getString(STATE_SELECTED_CONVERSATION);
			selectedBuddy = savedInstanceState.getString(STATE_SELECTED_BUDDY);
		}
		else
		{
			//If opening the app when already connected, show conversation list.
			if(CryptocatService.getInstance() != null && CryptocatService.getInstance().hasServers())
				sm.showMenu(false);
		}

		ActionBar ab = getSupportActionBar();
		ab.setTitle("Cryptocat");

		// TODO: If exposing deep links into your app, handle intents here.
	}
/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getSupportMenuInflater().inflate(R.menu.conversation_menu, menu);
		return true;
	}
*/
	@Override
	protected void onResume()
	{
		super.onResume();

		//Start the service if it isn't already started.
		startService(new Intent(this, CryptocatService.class));

		//Bind to the service.
		//This is only useful to ensure the service is started when we use it.
		//All communication with the service is done via CryptocatService.getInstance()
		Intent intent = new Intent(this, CryptocatService.class);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		if(bound)
			unbindService(connection);

		if (!CryptocatService.getInstance().hasServers())
		{
			Intent serviceIntent = new Intent(this, CryptocatService.class);
			stopService(serviceIntent);
		}

	}

	private void setFragment(int id, Fragment fragment)
	{
		FragmentTransaction t = getSupportFragmentManager().beginTransaction();
		t.replace(id, fragment);
		t.commit();
	}


    private void setLeftFragment(BaseFragment fragment)
    {
        setFragment(R.id.left_container, fragment);
    }

    private void setCenterFragment(BaseFragment fragment)
	{
        currCenterFragment = fragment;
		setFragment(R.id.center_container, fragment);
	}

	private void setRightFragment(BaseFragment fragment)
	{
        currRightFragment = fragment;
		setFragment(R.id.right_container, fragment);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_SECONDARY_MENU_SHOWING, sm.isSecondaryMenuShowing());
		outState.putBoolean(STATE_MENU_SHOWING, sm.isMenuShowing());
		outState.putString(STATE_SELECTED_SERVER, selectedServer);
		outState.putString(STATE_SELECTED_CONVERSATION, selectedConversation);
		outState.putString(STATE_SELECTED_BUDDY, selectedBuddy);
	}

	/**
	 * Callback method from {@link ConversationListFragment.Callbacks}
	 * indicating that the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(String server, String conversation, String buddy)
	{
		selectItem(server, conversation, buddy);
		sm.showContent();
	}

	@Override
	public void showLeftMenu()
	{
		sm.showMenu();
	}

	@Override
	public void showRightMenu()
	{
		sm.showSecondaryMenu();
	}

	@Override
	public void showContent()
	{
		sm.showContent();
	}

	public void selectItem(String server, String conversation, String buddy)
	{
		selectedServer = server;
		selectedConversation = conversation;
		selectedBuddy = buddy;

		conversationList.setSelectedItem(server, conversation, buddy);

		// In two-pane mode, show the detail view in this activity by
		// adding or replacing the detail fragment using a
		// fragment transaction.
		Bundle arguments = new Bundle();
		arguments.putString(ARG_SERVER_ID, server);
		arguments.putString(ARG_CONVERSATION_ID, conversation);
		arguments.putString(ARG_BUDDY_ID, buddy);

		BaseFragment centerFragment;
		BaseFragment rightFragment;

		if (server != null && conversation != null)
		{
			centerFragment = new ConversationFragment();
			rightFragment = new ConversationListFragment();
		}
		else
		{
			rightFragment = new ServerListFragment();
            centerFragment = new JoinConversationFragment();
		}

		currCenterFragment = centerFragment;
		centerFragment.setArguments(arguments);
		setCenterFragment(centerFragment);

		currRightFragment = rightFragment;
		rightFragment.setArguments(arguments);
		setRightFragment(rightFragment);

		updateFragmentVisibility();
	}

	private ServiceConnection connection = new ServiceConnection()
	{

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			bound = true;

			//It's important to create the conversation list before calling selectItem()

			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					//Create conversation list
					conversationList = new ConversationListFragment();
					setLeftFragment(conversationList);

					//All done, service is started, now set contents!
					selectItem(selectedServer, selectedConversation, selectedBuddy);
				}
			});
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			bound = false;
			//This is not supposed to happen unless we manually disconnect
			//from the service (I think).

		}
	};

	@Override
	public void onOpen()
	{
		updateFragmentVisibility();
	}

	@Override
	public void onClose()
	{
		updateFragmentVisibility();
	}

	private void updateFragmentVisibility()
	{
		if(conversationList == null || currCenterFragment == null)
			return;

		boolean menuShown = sm.isMenuShowing() && !sm.isSecondaryMenuShowing();

		conversationList.setSelected(menuShown);
		currCenterFragment.setSelected(!menuShown);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(!menuShown);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				if(sm.isSecondaryMenuShowing())
					sm.showContent();
				else
					sm.showMenu();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    public void onBackPressed() {
        if(sm.isSecondaryMenuShowing())
            showContent();
        else if(!sm.isMenuShowing())
            showLeftMenu();
        else
            super.onBackPressed();
    }
}
