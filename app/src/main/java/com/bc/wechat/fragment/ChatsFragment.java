package com.bc.wechat.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.bc.wechat.R;
import com.bc.wechat.activity.ChatActivity;
import com.bc.wechat.adapter.ConversationAdapter;
import com.bc.wechat.cons.Constant;
import com.bc.wechat.dao.UserDao;
import com.bc.wechat.entity.User;
import com.bc.wechat.utils.PreferencesUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.enums.ConversationType;
import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.model.GroupInfo;
import cn.jpush.im.android.api.model.UserInfo;

public class ChatsFragment extends BaseFragment {

    @BindView(R.id.tv_title)
    TextView mTitleTv;

    @BindView(R.id.lv_conversation)
    ListView mConversationLv;

    ConversationAdapter mConversationAdapter;
    List<Conversation> mConversationList;

    private static final int REFRESH_CONVERSATION_LIST = 0x3000;
    UserDao mUserDao;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversation, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setTitleStrokeWidth(mTitleTv);

        mUserDao = new UserDao();
        mConversationList = JMessageClient.getConversationList();
        if (null == mConversationList) {
            mConversationList = new ArrayList<>();
        }
        mConversationAdapter = new ConversationAdapter(getActivity(), mConversationList);
        mConversationLv.setAdapter(mConversationAdapter);

        mConversationLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Conversation conversation = mConversationList.get(position);
                int newMsgsUnreadNum = PreferencesUtil.getInstance().getNewMsgsUnreadNumber() - conversation.getUnReadMsgCnt();
                if (newMsgsUnreadNum < 0) {
                    newMsgsUnreadNum = 0;
                }
                PreferencesUtil.getInstance().setNewMsgsUnreadNumber(newMsgsUnreadNum);
                // 清除未读
                conversation.resetUnreadCount();

                if (conversation.getType().equals(ConversationType.single)) {
                    UserInfo userInfo = (UserInfo) conversation.getTargetInfo();

                    User user = mUserDao.getUserById(userInfo.getUserName());
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    intent.putExtra("targetType", Constant.TARGET_TYPE_SINGLE);
                    intent.putExtra("contactId", user.getUserId());
                    intent.putExtra("contactNickName", user.getUserNickName());
                    intent.putExtra("contactAvatar", user.getUserAvatar());
                    startActivity(intent);
                } else {
                    GroupInfo groupInfo = (GroupInfo) conversation.getTargetInfo();
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    intent.putExtra("targetType", Constant.TARGET_TYPE_GROUP);
                    intent.putExtra("groupId", String.valueOf(groupInfo.getGroupID()));
                    intent.putExtra("groupDesc", groupInfo.getGroupDescription());
                    intent.putExtra("memberNum", String.valueOf(groupInfo.getGroupMemberInfos().size()));
                    startActivity(intent);
                }
            }
        });
    }

    public void refreshConversationList() {
        mHandler.sendMessage(mHandler.obtainMessage(REFRESH_CONVERSATION_LIST));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REFRESH_CONVERSATION_LIST) {
                List<Conversation> newConversationList = JMessageClient.getConversationList();
                mConversationAdapter.setData(newConversationList);
                mConversationAdapter.notifyDataSetChanged();
            }
        }
    };
}
