package me.weey.graduationproject.client.smessager.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.vondear.rxtools.RxActivityTool;
import com.vondear.rxtools.view.RxToast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemSelected;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.adapter.FriendsListAdapter;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.UIUtil;

/**
 * 新建聊天界面的Activity
 * Created by weikai on 2018/02/10/0010.
 */

public class NewChatListActivity extends AppCompatActivity {

    @BindView(R.id.rv_friends) RecyclerView friendsRecyclerView;
    @BindView(R.id.new_chat_toolbar) Toolbar mToolbar;
    @BindView(R.id.et_search_words) EditText mSearchResult;
    private FriendsListAdapter mFriendsListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取主题的颜色
        int color = UIUtil.getThemeColorAccent(NewChatListActivity.this);
        //设置导航栏颜色
        getWindow().setNavigationBarColor(color);
        setContentView(R.layout.activity_new_chat_list);
        //绑定UI
        ButterKnife.bind(this);
        //初始化UI
        initUI();
    }

    /**
     * 初始化UI
     */
    private void initUI() {
        final ArrayList<User> friendsListInstant = Constant.getFriendsListInstant();
        mFriendsListAdapter = new FriendsListAdapter(friendsListInstant, this,
                new FriendsListAdapter.onRecyclerViewItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //RecyclerView中的条目的点击事件
                Intent intent = new Intent(NewChatListActivity.this, ChatActivity.class);
                intent.putExtra(ChatActivity.CHAT_INFO, friendsListInstant.get(position));
                startActivity(intent);
                finish();
            }
        });
        //初始化RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        friendsRecyclerView.setLayoutManager(layoutManager);
        //设置为垂直布局，这也是默认的
        layoutManager.setOrientation(OrientationHelper.VERTICAL);
        //设置Adapter
        friendsRecyclerView.setAdapter(mFriendsListAdapter);
        //设置增加或删除条目的动画
        friendsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mSearchResult.clearFocus();
        //显示向后退的按钮
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

            //后退按钮的点击事件
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                    finish();
                }
            });
        }

        //后退按钮设置成白色
        Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material);
        if (upArrow == null) return;
        upArrow.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        //设置文本改变的监听器
        mSearchResult.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //根据关键词刷新RecyclerView的Adapter

            }
        });
    }


}
