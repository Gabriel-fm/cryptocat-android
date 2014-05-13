package net.dirbaio.cryptocat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import net.dirbaio.cryptocat.service.CryptocatMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageListView extends ListView
{
    public MessageListView(Context context) {
        super(context);
    }

    public MessageListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    ConversationAdapter conversationArrayAdapter;

    public void setHistory(List<CryptocatMessage> history)
    {
        conversationArrayAdapter = new ConversationAdapter(getContext(), history);
        setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        setAdapter(conversationArrayAdapter);
    }

    public void notifyDataSetChanged()
    {
        conversationArrayAdapter.notifyDataSetChanged();
    }

    Paint paint = new Paint();

    //Trick to make the code below simpler.
    public static final CryptocatMessage dummyMessage = new CryptocatMessage(CryptocatMessage.Type.Error, "dummy", "dummy");
    private static Rect tempRect = new Rect();
    private static Rect bubbleRect = new Rect();
    private static Rect thisRect = new Rect();

    private int dpToPixels(int dp)
    {
        Resources r = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(Color.RED);
        paint.setStrokeWidth(0);

        final int count = getChildCount();

        boolean inGroup = false;
        int groupStart = 0;

        for(int i = 0; i < count; i++)
        {
            CryptocatMessage msg = dummyMessage;

            //Every message_* layout is a LinearLayout to align something to the left/center/right
            //We're interested in the position of its child, so we get it this way.
            ViewGroup vg = (ViewGroup) getChildAt(i);
            ViewHolder holder = (ViewHolder)vg.getTag();
            if(holder == null)
                continue;

            msg = holder.message;
            View v = vg.getChildAt(0);

            //See if new group starts
            if(!inGroup && (msg.type == CryptocatMessage.Type.Message || msg.type == CryptocatMessage.Type.MessageMine))
            {
                inGroup = true;
                groupStart = i;

                bubbleRect.set(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            }

            if(inGroup)
            {
                //group continues
                thisRect.set(vg.getLeft()+v.getLeft(), vg.getTop()+v.getTop(), vg.getLeft()+v.getRight(), vg.getTop()+v.getBottom());

                if(msg.screenWidth == getWidth())
                {
                    thisRect.left = min(thisRect.left, msg.left);
                    thisRect.right = max(thisRect.right, msg.right);
                }

                if(msg.firstInGroup && msg.type == CryptocatMessage.Type.Message)
                    thisRect.top += dpToPixels(10);

                if(msg.type == CryptocatMessage.Type.Message)
                    thisRect.left += dpToPixels(10);

                bubbleRect.union(thisRect);

                //Group ends
                if(msg.lastInGroup || i == count-1)
                {
                    //Choose drawable
                    NinePatchDrawable bubble;
                    if(msg.type == CryptocatMessage.Type.MessageMine)
                        bubble = (NinePatchDrawable) getResources().getDrawable(R.drawable.bubble_rev);
                    else
                        bubble = (NinePatchDrawable) getResources().getDrawable(R.drawable.bubble);

                    bubble.getPadding(tempRect);

                    //Set bounds on drawable
                    bubble.setBounds(bubbleRect.left - tempRect.left, bubbleRect.top - tempRect.top, bubbleRect.right + tempRect.right, bubbleRect.bottom + tempRect.bottom);

                    //Draw it!
                    bubble.draw(canvas);

                    //Exit bubble group and save coordinates
                    inGroup = false;
                    for(int j = groupStart; j <= i; j++)
                    {
                        ViewGroup vg2 = (ViewGroup) getChildAt(j);
                        ViewHolder holder2 = (ViewHolder)vg2.getTag();
                        if(holder2 == null)
                            continue;

                        CryptocatMessage msg2 = holder2.message;
                        msg2.screenWidth = getWidth();
                        msg2.left = bubbleRect.left;
                        msg2.right = bubbleRect.right;
                    }
                }
            }
        }
    }

    private static int min(int a, int b)
    {
        return a<b?a:b;
    }
    private static int max(int a, int b)
    {
        return a>b?a:b;
    }

    private static class ViewHolder {
        TextView nickname;
        TextView text;
        CryptocatMessage message;
    }

    private class ConversationAdapter extends ArrayAdapter<CryptocatMessage>
    {

        private Context context;

        public ConversationAdapter(Context context, List<CryptocatMessage> items)
        {
            super(context, 0, items);
            this.context = context;
        }

        @Override
        public int getViewTypeCount() {
            return CryptocatMessage.Type.values().length;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).type.ordinal();
        }

        private boolean sameGroup(CryptocatMessage a, CryptocatMessage b)
        {
            if(a.type != b.type)
                return false;
            if(a.type != CryptocatMessage.Type.Message && a.type != CryptocatMessage.Type.MessageMine)
                return false;

            return a.nickname.equals(b.nickname);
        }


        private final Spannable.Factory spannableFactory = Spannable.Factory
                .getInstance();

        private final Map<Pattern, Integer> emoticons = new HashMap<Pattern, Integer>();
        {
            addPattern(emoticons, ":)", R.drawable.smile);
            addPattern(emoticons, ":-)", R.drawable.smile);
            addPattern(emoticons, "^_^", R.drawable.smile);

        }

        private void addPattern(Map<Pattern, Integer> map, String smile,
                                int resource) {
            map.put(Pattern.compile(Pattern.quote(smile)), resource);
        }

        public boolean addSmiles(Context context, Spannable spannable) {
            boolean hasChanges = false;
            for (Map.Entry<Pattern, Integer> entry : emoticons.entrySet()) {
                Matcher matcher = entry.getKey().matcher(spannable);
                while (matcher.find()) {
                    boolean set = true;
                    for (ImageSpan span : spannable.getSpans(matcher.start(),
                            matcher.end(), ImageSpan.class))
                        if (spannable.getSpanStart(span) >= matcher.start()
                                && spannable.getSpanEnd(span) <= matcher.end())
                            spannable.removeSpan(span);
                        else {
                            set = false;
                            break;
                        }
                    if (set) {
                        hasChanges = true;
                        spannable.setSpan(new ImageSpan(context, entry.getValue()),
                                matcher.start(), matcher.end(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
            return hasChanges;
        }

        public Spannable getSmiledText(Context context, CharSequence text) {
            Spannable spannable = spannableFactory.newSpannable(text);
            addSmiles(context, spannable);
            return spannable;
        }

        public View getView(int position, View view, ViewGroup parent)
        {
            CryptocatMessage msg = getItem(position);

            CryptocatMessage prevMsg = dummyMessage;
            if(position > 0)
                prevMsg = getItem(position-1);

            CryptocatMessage nextMsg = dummyMessage;
            if(position < getCount()-1)
                nextMsg = getItem(position+1);

            //Calculate message grouping
            msg.firstInGroup = !sameGroup(msg, prevMsg);
            msg.lastInGroup = !sameGroup(msg, nextMsg);

            ViewHolder holder;

            if (view == null)
            {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                int id;

                switch (msg.type)
                {
                    case Message:       id = R.layout.message; break;
                    case MessageMine:   id = R.layout.message_mine; break;
                    case Join:          id = R.layout.message_join; break;
                    case Leave:         id = R.layout.message_leave; break;
                    case File:          id = R.layout.message_error; break;
                    case Error:         id = R.layout.message_error; break;
                    default: throw new IllegalStateException("Unknown item type");
                }

                view = inflater.inflate(id, null);

                holder = new ViewHolder();

                if(msg.type != CryptocatMessage.Type.Error && msg.type != CryptocatMessage.Type.MessageMine)
                    holder.nickname = (TextView) view.findViewById(R.id.nickname);
                if(msg.type != CryptocatMessage.Type.Join && msg.type != CryptocatMessage.Type.Leave)
                    holder.text = (TextView) view.findViewById(R.id.text);

                view.setTag(holder);
            }
            else
                holder = (ViewHolder) view.getTag();

            holder.message = msg;

            if(holder.nickname != null)
            {
                holder.nickname.setVisibility(msg.firstInGroup ? View.VISIBLE : View.GONE);
                holder.nickname.setText(msg.nickname);
            }

            if(holder.text != null)
            {
                if (msg.text.contains(":)"))
                {
                    holder.text.setText(getSmiledText(this.getContext(),msg.text));

                }else
                    holder.text.setText(msg.text);
            }

            view.setPadding(
                    view.getPaddingLeft(),
                    msg.type == CryptocatMessage.Type.MessageMine && msg.firstInGroup ? dpToPixels(12) : dpToPixels(3),
                    view.getPaddingRight(),
                    (msg.type == CryptocatMessage.Type.MessageMine || msg.type == CryptocatMessage.Type.Message) && msg.lastInGroup ? dpToPixels(20) : dpToPixels(3)
            );
            return view;
        }
    }

}
