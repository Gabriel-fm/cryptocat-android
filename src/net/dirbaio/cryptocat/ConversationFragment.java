package net.dirbaio.cryptocat;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import net.dirbaio.cryptocat.service.*;

/**
 * A fragment representing a single Conversation screen.
 * This fragment is contained in a {@link MainActivity}
 */
public class ConversationFragment extends BaseFragment implements CryptocatMessageListener, CryptocatBuddyListener, CryptocatStateListener
{
	private String serverId;
	private String conversationId;
	private String buddyId;
	private Conversation conversation;

	private MessageListView conversationView;
	private View rootView;
	private ImageButton sendButton;
	private EditText text;

	/**
     *
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ConversationFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		serverId = getArguments().getString(MainActivity.ARG_SERVER_ID);
		conversationId = getArguments().getString(MainActivity.ARG_CONVERSATION_ID);
		buddyId = getArguments().getString(MainActivity.ARG_BUDDY_ID);
	}

	@Override
	public void onStart()
	{
		super.onStart();
	}

	@Override
	protected void onMustUpdateTitle(ActionBar ab)
	{
		if(conversation == null) return;

		if(conversation instanceof MultipartyConversation)
		{
			MultipartyConversation mp = (MultipartyConversation) conversation;

			String subtitle = "";
			for(MultipartyConversation.Buddy b : mp.buddies)
			{
				if(!subtitle.isEmpty())
					subtitle += ", ";
				subtitle += b.nickname;
			}
			ab.setTitle(mp.roomName);
			ab.setSubtitle(subtitle);
		}
		else if(conversation instanceof OtrConversation)
		{
			OtrConversation priv = (OtrConversation) conversation;

			ab.setTitle(priv.parent.roomName);
			ab.setSubtitle(priv.buddy.nickname);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		conversation = getService().getServer(serverId).getConversation(conversationId);
		if(buddyId != null)
			conversation = ((MultipartyConversation)conversation).getPrivateConversation(buddyId);

        conversationView.setHistory(conversation.history);

		conversation.addMessageListener(ConversationFragment.this);
		if(conversation instanceof MultipartyConversation)
			((MultipartyConversation)conversation).addBuddyListener(ConversationFragment.this);

        getService().addStateListener(this);

        stateChanged();
		updateTitle();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		conversation.removeMessageListener(ConversationFragment.this);
        if(conversation instanceof MultipartyConversation)
            ((MultipartyConversation)conversation).removeBuddyListener(ConversationFragment.this);

        getService().removeStateListener(this);
	}

	@Override
	public void messageReceived(final CryptocatMessage message)
	{
		conversationView.notifyDataSetChanged();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.fragment_conversation, container, false);

		conversationView = (MessageListView) rootView.findViewById(R.id.conversation);

		sendButton = (ImageButton) rootView.findViewById(R.id.send);
		text = (EditText) rootView.findViewById(R.id.text);
		sendButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				sendMessage(text.getText().toString());
			}
		});
		text.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEND)
				{
					sendMessage(text.getText().toString());
					handled = true;
				}
				return handled;
			}
		});

        text.setFocusableInTouchMode(true);
        text.requestFocus();

		return rootView;
	}

	private void sendMessage(String str)
	{
		if (!str.isEmpty())
		{
			try
			{
				conversation.sendMessage(str);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			text.setText("");
		}
	}

	@Override
	public void buddyListChanged()
	{
		updateTitle();
	}


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.conversation_menu, menu);

        if(conversation instanceof MultipartyConversation)
            menu.findItem(R.id.showFingerprints).setVisible(false);
        else
            menu.findItem(R.id.leave).setVisible(false);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
            case R.id.buddies:
                callbacks.showRightMenu();
                return true;
            case R.id.showFingerprints:
                BuddyInfoActivity.showInfo(getActivity(), ((OtrConversation)conversation).buddy);
                return true;
            case R.id.showMyFingerprints:
                BuddyInfoActivity.showInfo(getActivity(), conversation.me);
                return true;
            case R.id.leave:
                if(conversation.getState() == Conversation.State.Joined)
                {
                    conversation.leave();
                    conversation.server.removeConversation(conversationId);
                    callbacks.onItemSelected(serverId, null, null);
                }
                return true;
		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    public void stateChanged() {
        String error = null;
        if(conversation.server.getState() == CryptocatServer.State.Connecting)
            error = "Connecting...";
        if(conversation.server.getState() == CryptocatServer.State.Error)
            error = "There was an error connecting to the server.";
        else if(conversation.server.getState() != CryptocatServer.State.Connected)
            error = "You're not connected to the server.";
        else if(conversation.getState() == Conversation.State.Error)
            error = "There was an error joining the conversation.";
        else if(conversation.getState() == Conversation.State.Joining)
            error = "Joining...";
        else if(conversation.getState() != Conversation.State.Joined)
            error = "You have left this conversation.";

        TextView errorView = (TextView) rootView.findViewById(R.id.warningText);
        if(error != null)
        {
            errorView.setVisibility(View.VISIBLE);
            errorView.setText(error);
        }
        else
            errorView.setVisibility(View.GONE);
    }
}
