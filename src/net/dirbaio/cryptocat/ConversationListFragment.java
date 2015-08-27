package net.dirbaio.cryptocat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import net.dirbaio.cryptocat.service.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A list fragment representing a list of Conversations. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ConversationFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ConversationListFragment extends BaseListFragment implements CryptocatStateListener
{

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int activatedPosition = ListView.INVALID_POSITION;
	private final List<Object> conversations = new ArrayList<Object>();
	private ArrayAdapter<Object> conversationArrayAdapter;

	private MultipartyConversation conversation;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ConversationListFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		if(getArguments() != null)
		{
			String serverId = getArguments().getString(MainActivity.ARG_SERVER_ID);
			String conversationId = getArguments().getString(MainActivity.ARG_CONVERSATION_ID);

			if(serverId != null)
				conversation = getService().getServer(serverId).getConversation(conversationId);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		/*getService().addStateListener(this);
		conversationArrayAdapter = new ConversationAdapter(getAltContext(), conversations);
		setListAdapter(conversationArrayAdapter);

		stateChanged(); //Fire initial update.

		setActivateOnItemClick(true);*/
		Intent intent = new Intent(getActivity(), CryptocatService.class);
		getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getService().removeStateListener(this);
		getActivity().unbindService(connection);

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION))
			setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
	}

	@Override
	public void stateChanged()
	{
		if(conversation != null)
			conversation.getPrivateConversationList(conversations);
		else
			getService().getConversationList(conversations);

		conversationArrayAdapter.notifyDataSetChanged();
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id)
	{
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		Object o = conversations.get(position);
		if (o instanceof OtrConversation)
		{
			OtrConversation conv = (OtrConversation) o;
			callbacks.onItemSelected(conv.server.id, conv.parent.id, conv.id);
		}
		else if (o instanceof MultipartyConversation)
		{
			MultipartyConversation conv = (MultipartyConversation) o;
			callbacks.onItemSelected(conv.server.id, conv.id, null);
		}
		else if (o instanceof CryptocatServer)
		{
			CryptocatServer srv = (CryptocatServer) o;
			callbacks.onItemSelected(srv.id, null, null);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		if (activatedPosition != ListView.INVALID_POSITION)
		{
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
		}
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick)
	{
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(activateOnItemClick
				? ListView.CHOICE_MODE_SINGLE
				: ListView.CHOICE_MODE_NONE);
	}

	private void setActivatedPosition(int position)
	{
		if (position == ListView.INVALID_POSITION)
		{
			getListView().setItemChecked(activatedPosition, false);
		} else
		{
			getListView().setItemChecked(position, true);
		}

		activatedPosition = position;
	}

	private int getPositionFor(String server, String conversation, String buddy)
	{
		int i = 0;
		for(Object o : conversations)
		{
			if(o instanceof CryptocatServer && conversation == null && buddy == null)
			{
				CryptocatServer s = (CryptocatServer) o;
				if(s.id.equals(server))
					return i;
			}
			if(o instanceof MultipartyConversation && buddy == null)
			{
				MultipartyConversation c = (MultipartyConversation) o;
				if(c.server.id.equals(server) && c.id.equals(conversation))
					return i;
			}
			if(o instanceof OtrConversation)
			{
				OtrConversation c = (OtrConversation) o;
				if(c.server.id.equals(server) && c.parent.id.equals(conversation) && c.id.equals(buddy))
					return i;
			}
			i++;
		}
		return -1;
	}
	public void setSelectedItem(String server, String conversation, String buddy)
	{
		if(getView() != null)
			setSelection(getPositionFor(server, conversation, buddy));
	}

	private class ConversationAdapter extends ArrayAdapter<Object>
	{

		private Context context;

		public ConversationAdapter(Context context, List<Object> items)
		{
			super(context, 0, items);
			this.context = context;
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (view == null)
			{
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.item_conversation, null);
			}

			ConversationItem item = (ConversationItem) getItem(position);

            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(item.getTitle());

            TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
            subtitle.setText(item.getSubtitle());

            ImageView icon = (ImageView) view.findViewById(R.id.image);
            int id = item.getImage();
            if(id == 0)
                icon.setImageResource(android.R.color.transparent);
            else
                icon.setImageResource(id);

            return view;
		}
	}

	@Override
	protected void onMustUpdateTitle(ActionBar ab)
	{
		ab.setTitle("Cryptocat");
		ab.setSubtitle(null);
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        if(conversation == null)
            inflater.inflate(R.menu.conversation_list_menu, menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.newserver:
				callbacks.onItemSelected(null, null, null);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private ServiceConnection connection = new ServiceConnection()
	{

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {

			//It's important to create the conversation list before calling selectItem()


					getService().addStateListener(ConversationListFragment.this);
					conversationArrayAdapter = new ConversationAdapter(getAltContext(), conversations);
					setListAdapter(conversationArrayAdapter);

					stateChanged(); //Fire initial update.

					setActivateOnItemClick(true);

		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {

		}
	};


}

