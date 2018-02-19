package me.weey.graduationproject.client.smessager.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vondear.rxtools.RxTimeTool;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.entity.ChatMessage;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.glide.GlideApp;
import me.weey.graduationproject.client.smessager.utils.Constant;

/**
 * 聊天界面的Adapter
 * Created by weikai on 2018/02/17/0017.
 */

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> mChatMessages;
    private Context mContext;
    private LayoutInflater inflater;
    private User mMyUser;
    private User friendUser;

    //建立枚举，3种不同的消息类型
    public enum MESSAGE_TYPE {
        TEXT,
        AUDIO,
        PICTURE
    }

    public ChatAdapter(List<ChatMessage> mChatMessages, Context mContext, User mMyUser, User friendUser) {
        this.mChatMessages = mChatMessages;
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
        this.mMyUser = mMyUser;
        this.friendUser = friendUser;
    }

    /**
     *  这个方法主要生成为每个Item inflater出一个View，但是该方法返回的是一个ViewHolder。
     *  该方法把View直接封装在ViewHolder中，然后我们面向的是ViewHolder这个实例，当然这个ViewHolder需要我们自己去编写。
     *  直接省去了当初的convertView.setTag(holder)和convertView.getTag()这些繁琐的步骤
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == MESSAGE_TYPE.TEXT.ordinal()) {
            //引入View
            View view = inflater.inflate(R.layout.list_bubble_text, parent, false);
            //返回Holder
            return new TextMessageViewHolder(view);
        }
        return null;
    }

    /**
     * 主要用于适配渲染数据到View中。方法提供给你了一个viewHolder，而不是原来的convertView
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = mChatMessages.get(position);
        switch (message.getMessageType()) {
            case 0:
                //文字信息
                if (message.getChatType() == 0) {
                    //发送的消息
                    ((TextMessageViewHolder)holder).mReceiveMsg.setVisibility(View.GONE);
                    ((TextMessageViewHolder)holder).mSendMsg.setVisibility(View.VISIBLE);
                    setAvatar(((TextMessageViewHolder)holder).mSendAvatar, mMyUser);
                    setTime(((TextMessageViewHolder)holder).mSendMsgTime, message.getTime());
                    ((TextMessageViewHolder)holder).mSendMsgContent.setText(message.getMessage().trim());
                    //字数少的时候加点料
                    if (message.getMessage().trim().length() < 5) {
                        ((TextMessageViewHolder)holder).mSendMsgTime.setText("     " + ((TextMessageViewHolder)holder).mSendMsgTime.getText());
                    }
                } else if (message.getChatType() == 1) {
                    //接收的消息
                    ((TextMessageViewHolder)holder).mReceiveMsg.setVisibility(View.VISIBLE);
                    ((TextMessageViewHolder)holder).mSendMsg.setVisibility(View.GONE);
                    setAvatar(((TextMessageViewHolder)holder).mReceiveAvatar, friendUser);
                    setTime(((TextMessageViewHolder)holder).mReceiveMsgTime, message.getTime());
                    ((TextMessageViewHolder)holder).mReceiveMsgContent.setText(message.getMessage().trim());
                    //字数少的时候加点料
                    if (message.getMessage().trim().length() < 5) {
                        ((TextMessageViewHolder)holder).mReceiveMsgTime.setText("     " + ((TextMessageViewHolder)holder).mReceiveMsgTime.getText().toString());
                    }
                }
                break;
        }
    }

    /**
     * 对时间显示的处理
     */
    private void setTime(TextView time, String timeString) {
        Date date = RxTimeTool.string2Date(timeString);
        //获取当前的日期
        String year = RxTimeTool.getCurrentDateTime("yyyy");
        String month = RxTimeTool.getCurrentDateTime("MM");
        String day = RxTimeTool.getCurrentDateTime("dd");
        //获取传来消息的时间
        String thatYear = RxTimeTool.simpleDateFormat("yyyy", date);
        String thatMonth = RxTimeTool.simpleDateFormat("MM", date);
        String thatDay = RxTimeTool.simpleDateFormat("dd", date);
        //对时间判断显示
        if (!year.equals(thatYear)) {
            time.setText(RxTimeTool.simpleDateFormat("yyyy-MM-dd", date));
        } else if (year.equals(thatYear) && !month.equals(thatMonth)) {
            time.setText(RxTimeTool.simpleDateFormat("MM-dd", date));
        } else if (year.equals(thatYear) && month.equals(thatMonth) && !day.equals(thatDay)) {
            time.setText(RxTimeTool.simpleDateFormat("EEEE", date));
        } else if (year.equals(thatYear) && month.equals(thatMonth) && day.equals(thatDay)) {
            time.setText(RxTimeTool.simpleDateFormat("hh:mm", date));
        }
    }

    /**
     * 显示头像
     */
    private void setAvatar(ImageView holder, User user) {
        String avatarURL = Constant.SERVER_ADDRESS + "account/avatars/" + user.getId();
        GlideApp.with(mContext)
                .load(avatarURL)
                //占位图
                .placeholder(R.mipmap.book_user)
                //圆形显示
                .circleCrop()
                //错误占位符
                .error(R.mipmap.book_user)
                //后备回调符
                .fallback(R.mipmap.book_user)
                //判断是否需要只从缓存中读取
                //.onlyRetrieveFromCache(!isLoadFromNet)
                .into(holder);
    }


    @Override
    public int getItemCount() {
        return mChatMessages.size();
    }

    //设置ITEM类型
    @Override
    public int getItemViewType(int position) {
        int messageType = mChatMessages.get(position).getMessageType();
        if (messageType == 1) {
            return MESSAGE_TYPE.PICTURE.ordinal();
        } else if (messageType == 2) {
            return MESSAGE_TYPE.AUDIO.ordinal();
        } else {
            return MESSAGE_TYPE.TEXT.ordinal();
        }
    }

    /**
     * 新增文本条目
     */
    public void addTextMessage(ChatMessage chatMessage) {
        mChatMessages.add(chatMessage);
        notifyItemInserted(mChatMessages.size() - 1);
    }

    /**
     * 文字消息的ViewHolder
     */
    class TextMessageViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.rv_chat_receive_text) RelativeLayout mReceiveMsg;
        @BindView(R.id.iv_receive_text_avatar) ImageView mReceiveAvatar;
        @BindView(R.id.tv_chat_message_receive_text_content) TextView mReceiveMsgContent;
        @BindView(R.id.tv_chat_message_receive_text_time) TextView mReceiveMsgTime;

        @BindView(R.id.rv_chat_send_text) RelativeLayout mSendMsg;
        @BindView(R.id.iv_send_text_avatar) ImageView mSendAvatar;
        @BindView(R.id.tv_chat_message_send_text_content) TextView mSendMsgContent;
        @BindView(R.id.tv_chat_message_send_text_time) TextView mSendMsgTime;

        public TextMessageViewHolder(View itemView) {
            super(itemView);
            //绑定UI
            ButterKnife.bind(this, itemView);
        }
    }
}
