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
import net.dirbaio.cryptocat.serverlist.ServerConfig;
import net.dirbaio.cryptocat.service.CryptocatServer;
import net.dirbaio.cryptocat.service.CryptocatService;
import net.dirbaio.cryptocat.service.CryptocatStateListener;
import net.dirbaio.cryptocat.service.MultipartyConversation;
import org.jivesoftware.smack.XMPPException;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class JoinConversationFragment extends BaseFragment
{

/*    private String serverId;
    private CryptocatServer server;
*/

	private View rootView;
	private int oldVisible = -1;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		updateTitle();
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.fragment_join_conversation, container, false);

		final Button button = (Button) rootView.findViewById(R.id.join_button);
		final EditText roomNameText = (EditText) rootView.findViewById(R.id.name);
		final EditText nicknameText = (EditText) rootView.findViewById(R.id.nickname);

		button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO Get this from DB
                final ServerConfig config = new ServerConfig();

                CryptocatService service = CryptocatService.getInstance();
                CryptocatServer server = service.getServer(config.server);
                if(server == null)
                {
                    server = getService().createServer(config);
                    server.connect();
                }

                rootView.findViewById(R.id.join).setVisibility(View.GONE);
                rootView.findViewById(R.id.connecting).setVisibility(View.VISIBLE);

                final String roomName = roomNameText.getText().toString();
                final String nickname = nicknameText.getText().toString();
                final CryptocatServer serverf = server;
                server.runWhenConnected(new ExceptionRunnable() {
                    @Override
                    public void run() throws Exception {
                        MultipartyConversation c = serverf.createConversation(roomName, nickname);
                        c.join();
                        callbacks.onItemSelected(serverf.id, c.id, null);

                    }
                });
            }
        });

		return rootView;
	}

	@Override
	protected void onMustUpdateTitle(ActionBar ab)
	{
        ab.setTitle("Cryptocat");
        ab.setSubtitle(getResources().getString(R.string.join_chat_room));
	}
}