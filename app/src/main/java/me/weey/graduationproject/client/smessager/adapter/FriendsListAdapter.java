package me.weey.graduationproject.client.smessager.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.glide.GlideApp;
import me.weey.graduationproject.client.smessager.utils.Constant;

/**
 * 好友列表的Adapter
 * Created by weikai on 2018/02/10/0010.
 */
public class FriendsListAdapter extends RecyclerView.Adapter<FriendsListAdapter.FriendListViewHolder> {

    private List<User> mFriendsLists;
    private onRecyclerViewItemClickListener listener;
    private Context mContext;
    private LayoutInflater inflater;

    public FriendsListAdapter(List<User> mFriendsLists, Context mContext, onRecyclerViewItemClickListener listener) {
        this.mFriendsLists = mFriendsLists;
        this.mContext = mContext;
        this.listener = listener;
        inflater = LayoutInflater.from(mContext);
    }

    /**
     *  这个方法主要生成为每个Item inflater出一个View，但是该方法返回的是一个ViewHolder。
     *  该方法把View直接封装在ViewHolder中，然后我们面向的是ViewHolder这个实例，当然这个ViewHolder需要我们自己去编写。
     *  直接省去了当初的convertView.setTag(holder)和convertView.getTag()这些繁琐的步骤
     */
    @Override
    public FriendListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //引入View
        View view = inflater.inflate(R.layout.list_friends, parent, false);
        //初始化好ViewHolder
        return new FriendListViewHolder(view);
    }

    /**
     * 主要用于适配渲染数据到View中。方法提供给你了一个viewHolder，而不是原来的convertView
     */
    @Override
    public void onBindViewHolder(FriendListViewHolder holder, int position) {
        User friendList = mFriendsLists.get(position);
        /**
         * 把取来的数据显示到Item中
         */
        setAvatar(holder, friendList);
        //设置用户名
        holder.userName.setText(friendList.getUserName());
    }

    /**
     * 设置头像显示
     */
    private void setAvatar(FriendListViewHolder holder, User user) {
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
                .into(holder.avatarView);
    }

    @Override
    public int getItemCount() {
        return mFriendsLists.size();
    }

    /**
     * 在这里查到到list里面的控件
     */
    class FriendListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.iv_avatar)
        ImageView avatarView;
        @BindView(R.id.tv_friends_name)
        TextView userName;

        public FriendListViewHolder(View itemView) {
            super(itemView);
            //绑定UI
            ButterKnife.bind(this, itemView);
            //设置条目的点击事件
            itemView.setOnClickListener(this);
        }

        /**
         * 把方法提供给暴露的方法
         */
        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onItemClick(v, getAdapterPosition());
            }
        }
    }

    /**
     * 把Item点击的接口暴露出去
     */
    public interface onRecyclerViewItemClickListener {
        void onItemClick(View view, int position);
    }
}
