package net.dirbaio.cryptocat.service;

import net.java.otr4j.OtrException;
import org.jivesoftware.smack.XMPPException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;

public abstract class Conversation implements ConversationItem
{
	private final ArrayList<CryptocatMessageListener> msgListeners = new ArrayList<>();
	public CryptocatServer server;
	public String nickname;
	public String id;
	public final ArrayList<CryptocatMessage> history = new ArrayList<>();
    public MultipartyConversation.Buddy me;

	private volatile State state;
    private int unread;

	public enum State
	{
		Left,
		Joined,
		Leaving,
		Joining,
		Error
	}

	public Conversation(CryptocatServer server, String nickname)
	{
		Utils.assertUiThread();
		this.server = server;
		this.nickname = nickname;
		this.state = State.Left;

		addMessageListener(CryptocatService.getInstance());
	}

    protected final void setState(State newState)
    {
        if(state == newState) return;

        state = newState;
        server.notifyStateChanged();
    }

	public final State getState()
	{
		Utils.assertUiThread();
		return state;
	}

    public final int getUnreadCount()
    {
        return unread;
    }

	public abstract void join() throws NoSuchAlgorithmException, XMPPException, NoSuchProviderException;
	public abstract void leave();
	public abstract void sendMessage(String msg) throws OtrException, UnsupportedEncodingException, InvalidKeyException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchProviderException, XMPPException, NoSuchPaddingException, OtrException;

	public final void addMessageListener(CryptocatMessageListener l)
	{
		Utils.assertUiThread();
		msgListeners.add(l);

        if(unread != 0)
        {
            unread = 0;
            server.notifyStateChanged();
        }

	}

	public final void removeMessageListener(CryptocatMessageListener l)
	{
		Utils.assertUiThread();
		msgListeners.remove(l);
	}

	protected void addMessage(CryptocatMessage msg)
	{
		Utils.assertUiThread();

		history.add(msg);

        if(msgListeners.size() == 0 && msg.type != CryptocatMessage.Type.Join && msg.type != CryptocatMessage.Type.Leave)
            unread++;

        server.notifyStateChanged();

		for (CryptocatMessageListener l : msgListeners)
			l.messageReceived(msg);
	}

    @Override
    public String getSubtitle()
    {
        if(history.size() == 0)
            return "";

        return history.get(history.size()-1).toString();
    }

}

