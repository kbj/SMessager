package me.weey.graduationproject.client.smessager.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vondear.rxtools.RxTimeTool;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.entity.ChatList;
import me.weey.graduationproject.client.smessager.glide.GlideApp;

/**
 * 聊天列表的Adapter
 * Created by weikai on 2018/02/06/0006.
 */

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<ChatList> mChatLists;
    private Context mContext;
    private LayoutInflater inflater;
    private Boolean isLoadFromNet;

    public ChatListAdapter(List<ChatList> ChatLists, Context context, Boolean isLoadFromNet) {
        this.mChatLists = ChatLists;
        this.mContext = context;
        this.isLoadFromNet = isLoadFromNet;
        inflater = LayoutInflater.from(mContext);
    }

    /**
     *  这个方法主要生成为每个Item inflater出一个View，但是该方法返回的是一个ViewHolder。
     *  该方法把View直接封装在ViewHolder中，然后我们面向的是ViewHolder这个实例，当然这个ViewHolder需要我们自己去编写。
     *  直接省去了当初的convertView.setTag(holder)和convertView.getTag()这些繁琐的步骤
     */
    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //引入View
        View view = inflater.inflate(R.layout.list_chat, parent, false);
        //创建好Holder
        //返回Holder
        return new ChatViewHolder(view);
    }

    /**
     * 主要用于适配渲染数据到View中。方法提供给你了一个viewHolder，而不是原来的convertView
     */
    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        ChatList chatList = mChatLists.get(position);

        /**
         * 对数据进行处理
         */
        //头像
        setAvatar(holder, chatList);
        //最新消息
        holder.latestMessage.setText(chatList.getLatestMessage().trim());
        //用户名
        holder.userName.setText(chatList.getUserName());
        //是否有未读消息
        if (chatList.isNewMessage()) {
            holder.newMessage.setVisibility(View.VISIBLE);
        } else {
            holder.newMessage.setVisibility(View.INVISIBLE);
        }
        //设置时间
        setTime(holder, chatList.getTime());
    }

    /**
     * 设置消息的时间
     */
    private void setTime(ChatViewHolder holder, Date time) {
        //获取当前的日期
        String year = RxTimeTool.getCurrentDateTime("yyyy");
        String month = RxTimeTool.getCurrentDateTime("MM");
        String day = RxTimeTool.getCurrentDateTime("dd");
        //获取传来消息的时间
        String thatYear = RxTimeTool.simpleDateFormat("yyyy", time);
        String thatMonth = RxTimeTool.simpleDateFormat("MM", time);
        String thatDay = RxTimeTool.simpleDateFormat("dd", time);
        //对时间判断显示
        if (!year.equals(thatYear)) {
            holder.messageTime.setText(RxTimeTool.simpleDateFormat("yyyy-MM-dd", time));
        } else if (year.equals(thatYear) && !month.equals(thatMonth)) {
            holder.messageTime.setText(RxTimeTool.simpleDateFormat("MM-dd", time));
        } else if (year.equals(thatYear) && month.equals(thatMonth) && !day.equals(thatDay)) {
            holder.messageTime.setText(RxTimeTool.simpleDateFormat("EEEE", time));
        } else if (year.equals(thatYear) && month.equals(thatMonth) && day.equals(thatDay)) {
            holder.messageTime.setText(RxTimeTool.simpleDateFormat("hh:mm", time));
        }
    }

    /**
     * 显示头像
     */
    private void setAvatar(ChatViewHolder holder, ChatList chatList) {
        GlideApp.with(mContext)
                .load(chatList.getAvatarUrl())
                //占位图
                .placeholder(R.mipmap.book_user)
                //圆形显示
                .circleCrop()
                //错误占位符
                .error(R.mipmap.book_user)
                //后备回调符
                .fallback(R.mipmap.book_user)
                //判断是否需要只从缓存中读取
                .onlyRetrieveFromCache(!isLoadFromNet)
                .into(holder.avatarImage);
    }


    /**
     * 类似于BaseAdapter的getCount方法了，即总共有多少个条目
     */
    @Override
    public int getItemCount() {
        return mChatLists.size();
    }

    /**
     * 在这里查到到list里面的控件
     */
    class ChatViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_avatar) ImageView avatarImage;
        @BindView(R.id.tv_user) TextView userName;
        @BindView(R.id.tv_latest_message) TextView latestMessage;
        @BindView(R.id.tv_msg_time) TextView messageTime;
        @BindView(R.id.iv_new_msg) ImageView newMessage;

        public ChatViewHolder(View itemView) {
            super(itemView);
            //绑定UI
            ButterKnife.bind(this, itemView);
        }
    }
}
