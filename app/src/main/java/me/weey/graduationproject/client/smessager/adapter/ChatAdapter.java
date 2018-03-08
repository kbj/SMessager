package me.weey.graduationproject.client.smessager.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ruochuan.bubblelayout.BubbleLayout;
import com.vondear.rxtools.RxTimeTool;

import java.io.File;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.entity.ChatMessage;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.glide.GlideApp;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.MediaManager;

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

    public static Boolean mIsPlayingVoice = false;     //是否正在播放音乐
    public static Integer mPlayListPosition = -1;         //正在播放音乐的那个ImageView的ID

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
            //文本类型
            //引入View
            View view = inflater.inflate(R.layout.list_bubble_text, parent, false);
            //返回Holder
            return new TextMessageViewHolder(view);
        } else if (viewType == MESSAGE_TYPE.AUDIO.ordinal()) {
            //语音类型
            View view = inflater.inflate(R.layout.list_bubble_voice, parent, false);
            //返回holder
            return new VoiceMessageViewHolder(view);
        } else if (viewType == MESSAGE_TYPE.PICTURE.ordinal()) {
            //图像类型
            View view = inflater.inflate(R.layout.list_bubble_image, parent, false);
            //返回Holder
            return new ImageMessageViewHolder(view);
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
            case Constant.CHAT_MESSAGE_TYPE_TEXT:
                //文字信息
                showText((TextMessageViewHolder) holder, message);
                break;
            case Constant.CHAT_MESSAGE_TYPE_VOICE_HAVE_LISTEN:
            case Constant.CHAT_MESSAGE_TYPE_VOICE_NEW:
                //语音信息
                showVoice((VoiceMessageViewHolder) holder, message);
                break;
            case Constant.CHAT_MESSAGE_TYPE_IMAGE:
                //图片信息
                showImage((ImageMessageViewHolder) holder, message);
                break;
        }
    }

    /**
     * 对图片类型的消息的显示
     */
    private void showImage(ImageMessageViewHolder holder, ChatMessage message) {
        //判断消息收到还是发送
        if (message.getChatType() == Constant.CHAT_TYPE_SEND) {
            //发送的消息
            holder.mReceiveMsg.setVisibility(View.GONE);
            holder.mSendMsg.setVisibility(View.VISIBLE);
            setAvatar(holder.mSendAvatar, mMyUser);
            setTime(holder.mSendImageTime, message.getTime());
            setImageMessage(message.getMessage(), message.getVoiceSecond(), holder.mSendImage);
        } else if (message.getChatType() == Constant.CHAT_TYPE_RECEIVE) {
            //接收的消息
            holder.mReceiveMsg.setVisibility(View.VISIBLE);
            holder.mSendMsg.setVisibility(View.GONE);
            setAvatar(holder.mReceiveAvatar, mMyUser);
            setTime(holder.mReceiveImageTime, message.getTime());
            setImageMessage(message.getMessage(), message.getVoiceSecond(), holder.mReceiveImage);
        }
    }

    /**
     * 对语音类型的消息的显示
     */
    private void showVoice(VoiceMessageViewHolder holder, ChatMessage message) {
        //判断消息收到还是发送
        if (message.getChatType() == Constant.CHAT_TYPE_SEND) {
            //发送的消息
            holder.mReceiveMsg.setVisibility(View.GONE);
            holder.mSendMsg.setVisibility(View.VISIBLE);
            holder.mReceiveVoice.setVisibility(View.GONE);
            holder.mSendVoice.setVisibility(View.VISIBLE);
            setAvatar(holder.mSendAvatar, mMyUser);
            setTime(holder.mSendVoiceTime, message.getTime());
            holder.mSendVoiceSecond.setText(message.getVoiceSecond());
        } else if (message.getChatType() == Constant.CHAT_TYPE_RECEIVE){
            //接收的消息
            holder.mReceiveMsg.setVisibility(View.VISIBLE);
            holder.mSendMsg.setVisibility(View.GONE);
            holder.mReceiveVoice.setVisibility(View.VISIBLE);
            holder.mSendVoice.setVisibility(View.GONE);
            setAvatar(holder.mReceiveAvatar, friendUser);
            setTime(holder.mReceiveVoiceTime, message.getTime());
            holder.mReceiveVoiceSecond.setText(message.getVoiceSecond());
            if (message.getMessageType() == Constant.CHAT_MESSAGE_TYPE_VOICE_HAVE_LISTEN) {
                //是已读的语音消息
                holder.mUnreadVoice.setVisibility(View.GONE);
            } else if (message.getMessageType() == Constant.CHAT_MESSAGE_TYPE_VOICE_NEW) {
                //未读的语音消息
                holder.mUnreadVoice.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 对文本类型的消息显示
     */
    private void showText(TextMessageViewHolder holder, ChatMessage message) {
        if (message.getChatType() == Constant.CHAT_TYPE_SEND) {
            //发送的消息
            holder.mReceiveMsg.setVisibility(View.GONE);
            holder.mSendMsg.setVisibility(View.VISIBLE);
            setAvatar(holder.mSendAvatar, mMyUser);
            setTime(holder.mSendMsgTime, message.getTime());
            holder.mSendMsgContent.setText(message.getMessage().trim());
            //字数少的时候加点料
            if (message.getMessage().trim().length() < 5) {
                holder.mSendMsgTime.setText("     " + holder.mSendMsgTime.getText());
            }
        } else if (message.getChatType() == Constant.CHAT_TYPE_RECEIVE) {
            //接收的消息
            holder.mReceiveMsg.setVisibility(View.VISIBLE);
            holder.mSendMsg.setVisibility(View.GONE);
            setAvatar(holder.mReceiveAvatar, friendUser);
            setTime(holder.mReceiveMsgTime, message.getTime());
            holder.mReceiveMsgContent.setText(message.getMessage().trim());
            //字数少的时候加点料
            if (message.getMessage().trim().length() < 5) {
                holder.mReceiveMsgTime.setText("     " + holder.mReceiveMsgTime.getText().toString());
            }
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
     * 对图片消息的显示
     * @param message           压缩图的缓存地址
     * @param voiceSecond       原图的路径（如果没有原图就为空）
     * @param mSendImage        显示图片的控件
     */
    private void setImageMessage(String message, String voiceSecond, ImageView mSendImage) {
        if (mSendImage == null) return;
        //对输入值的校验
        String loadUrl = TextUtils.isEmpty(voiceSecond) ? message : voiceSecond;
        File img = new File(loadUrl);
        if (!img.exists()) return;
        //加载图片
        GlideApp.with(mContext).load(img).override(300, 300).into(mSendImage);
    }

    /**
     * 显示头像
     */
    private void setAvatar(ImageView holder, User user) {
        String avatarURL;

        if (user != null) {
            avatarURL = Constant.SERVER_ADDRESS + "account/avatars/" + user.getId();
        } else {
            avatarURL = "";
        }

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
        if (messageType == Constant.CHAT_MESSAGE_TYPE_IMAGE) {
            return MESSAGE_TYPE.PICTURE.ordinal();
        } else if (messageType == Constant.CHAT_MESSAGE_TYPE_VOICE_NEW || messageType == Constant.CHAT_MESSAGE_TYPE_VOICE_HAVE_LISTEN) {
            return MESSAGE_TYPE.AUDIO.ordinal();
        } else {
            return MESSAGE_TYPE.TEXT.ordinal();
        }
    }

    /**
     * 新增条目
     */
    public void addMessage(ChatMessage chatMessage) {
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

    /**
     * 语音消息的ViewHolder
     */
    class VoiceMessageViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.rv_chat_receive_voice) RelativeLayout mReceiveMsg;
        @BindView(R.id.iv_receive_voice_avatar) ImageView mReceiveAvatar;
        @BindView(R.id.iv_chat_message_receive_voice) View mReceiveVoice;
        @BindView(R.id.tv_chat_message_receive_voice_second) TextView mReceiveVoiceSecond;
        @BindView(R.id.tv_chat_message_receive_voice_time) TextView mReceiveVoiceTime;
        @BindView(R.id.iv_unread_voice) ImageView mUnreadVoice;

        @BindView(R.id.rv_chat_send_text) RelativeLayout mSendMsg;
        @BindView(R.id.iv_send_voice_avatar) ImageView mSendAvatar;
        @BindView(R.id.tv_chat_message_send_voice_second) TextView mSendVoiceSecond;
        @BindView(R.id.iv_chat_message_send_voice) View mSendVoice;
        @BindView(R.id.tv_chat_message_send_voice_time) TextView mSendVoiceTime;

        public VoiceMessageViewHolder(View itemView) {
            super(itemView);
            //绑定UI
            ButterKnife.bind(this, itemView);
        }

        /**
         * 气泡的点击事件
         * 播放语音，如果此时正在播放，需要先暂停，同时停止另外的动画播放
         */
        @OnClick({R.id.bl_send_bubble, R.id.bl_receive_bubble})
        public void clickVoiceBubble(View bubble) {
            bubbleClickListener.onClick(bubble, getAdapterPosition());
        }
    }

    /**
     * 图片消息的ViewHolder
     */
    class ImageMessageViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.rv_chat_receive_image) RelativeLayout mReceiveMsg;
        @BindView(R.id.iv_receive_image_avatar) ImageView mReceiveAvatar;
        @BindView(R.id.iv_chat_message_receive_image) ImageView mReceiveImage;
        @BindView(R.id.tv_chat_message_receive_image_time) TextView mReceiveImageTime;

        @BindView(R.id.rv_chat_send_image) RelativeLayout mSendMsg;
        @BindView(R.id.iv_send_image_avatar) ImageView mSendAvatar;
        @BindView(R.id.iv_chat_message_send_image) ImageView mSendImage;
        @BindView(R.id.tv_chat_message_send_text_time) TextView mSendImageTime;

        public ImageMessageViewHolder(View itemView) {
            super(itemView);
            //绑定UI
            ButterKnife.bind(this, itemView);
        }

        /**
         * 缩略图的点击事件
         */
        @OnClick({R.id.iv_chat_message_receive_image, R.id.iv_chat_message_send_image})
        public void clickImage(View imageView) {
            onClickImageListener.onClick(imageView, getAdapterPosition());
        }
    }

    /**
     * 语音消息的气泡点击事件
     */
    public interface onClickBubbleListener {
        void onClick(View view, int position);
    }

    private onClickBubbleListener bubbleClickListener;

    public void setBubbleClickListener(onClickBubbleListener bubbleClickListener) {
        this.bubbleClickListener = bubbleClickListener;
    }

    /**
     * 图片消息图片的点击事件
     */
    public interface onClickImageListener {
        void onClick(View view, int position);
    }

    private onClickImageListener onClickImageListener;

    public void setImageClickListener(onClickImageListener onClickImageListener) {
        this.onClickImageListener = onClickImageListener;
    }
}
