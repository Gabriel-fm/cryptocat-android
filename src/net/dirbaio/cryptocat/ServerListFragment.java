package net.dirbaio.cryptocat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import net.dirbaio.cryptocat.serverlist.ServerConfig;
import net.dirbaio.cryptocat.service.CryptocatServer;
import net.dirbaio.cryptocat.service.CryptocatService;
import net.dirbaio.cryptocat.service.CryptocatStateListener;

import java.util.List;

public class ServerListFragment extends BaseListFragment
{
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unbindService(connection);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = new Intent(getActivity(), CryptocatService.class);
        getActivity().bindService(intent, connection, 0);

        //setListAdapter(new ServersAdapter(getAltContext(), CryptocatService.getInstance().serverList.servers));

                /*
        serversListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                CryptocatService service = CryptocatService.getInstance();
                ServerConfig config = service.serverList.servers.get(position);
                if(service.getServer(config.server) != null)
                    Toast.makeText(getActivity(), "You're already connected to this server", Toast.LENGTH_SHORT).show();
                else
                {
                    CryptocatServer server = getService().createServer(config);
                    server.connect();
                    callbacks.onItemSelected(server.id, null, null);
                }
            }
        });
        serversListView.setLongClickable(true);
        serversListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return false;
            }
        });
*/
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.server_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.newserver:
                //TODO
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ServersAdapter extends ArrayAdapter<ServerConfig>
    {

        private Context context;

        public ServersAdapter(Context context, List<ServerConfig> items)
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

            ServerConfig item = (ServerConfig) getItem(position);

            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(item.name);

            TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
            subtitle.setText(item.getDescription());

            ImageView icon = (ImageView) view.findViewById(R.id.image);
            icon.setImageResource(R.drawable.ic_action_web_site);

            return view;
        }
    }



    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            setListAdapter(new ServersAdapter(getAltContext(), CryptocatService.getInstance().serverList.servers));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}
