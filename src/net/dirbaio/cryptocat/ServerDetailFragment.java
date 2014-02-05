package net.dirbaio.cryptocat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import net.dirbaio.cryptocat.service.CryptocatServer;
import net.dirbaio.cryptocat.service.CryptocatStateListener;
import net.dirbaio.cryptocat.service.MultipartyConversation;
import org.jivesoftware.smack.XMPPException;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class ServerDetailFragment extends BaseFragment implements CryptocatStateListener
{

	private String serverId;
	private CryptocatServer server;

	private View rootView;
	private int oldVisible = -1;

	@Override
	public void stateChanged()
	{
		int visible = R.id.not_connected;
		if(server.getState() == CryptocatServer.State.Connected)
			visible = R.id.join;
		if(server.getState() == CryptocatServer.State.Connecting)
			visible = R.id.connecting;

		if(oldVisible != visible)
		{
			rootView.findViewById(R.id.not_connected).setVisibility(visible==R.id.not_connected?View.VISIBLE:View.GONE);
			rootView.findViewById(R.id.join).setVisibility(visible==R.id.join?View.VISIBLE:View.GONE);
			rootView.findViewById(R.id.connecting).setVisibility(visible==R.id.connecting?View.VISIBLE:View.GONE);

			oldVisible = visible;

            if(disconnectMenuItem != null)
            {
                disconnectMenuItem.setVisible(visible != R.id.connecting);
                getActivity().supportInvalidateOptionsMenu();
            }
        }
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

		serverId = getArguments().getString(MainActivity.ARG_SERVER_ID);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		server = getService().getServer(serverId);
		server.addStateListener(this);
		stateChanged();
		updateTitle();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		server.removeStateListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.fragment_server_detail, container, false);

		final Button button = (Button) rootView.findViewById(R.id.join_button);
		final EditText roomNameText = (EditText) rootView.findViewById(R.id.name);
		final EditText nicknameText = (EditText) rootView.findViewById(R.id.nickname);

		button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				String roomName = roomNameText.getText().toString();
				String nickname = nicknameText.getText().toString();
				try
				{
					MultipartyConversation c;
					c = server.createConversation(roomName, nickname);
					c.join();
					callbacks.onItemSelected(serverId, c.id, null);
				}
				catch (XMPPException e)
				{
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                }
            }
		});

		final Button button2 = (Button) rootView.findViewById(R.id.reconnect_button);
		button2.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				server.connect();
			}
		});
		return rootView;
	}

	@Override
	protected void onMustUpdateTitle(ActionBar ab)
	{
        ab.setTitle(server.config.server);
        ab.setSubtitle("Join chat room");
	}

    MenuItem disconnectMenuItem;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.server_menu, menu);
        disconnectMenuItem = menu.findItem(R.id.leave);
        if(server != null)
            disconnectMenuItem.setVisible(server.getState() != CryptocatServer.State.Connecting);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.leave:
                if(server.getState() == CryptocatServer.State.Connected)
                {
                    server.disconnect();
                    callbacks.onItemSelected(null, null, null);
                }

                if(server.getState() == CryptocatServer.State.Disconnected)
                    getService().removeServer(serverId);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}