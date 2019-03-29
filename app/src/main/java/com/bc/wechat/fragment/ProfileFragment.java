package com.bc.wechat.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bc.wechat.R;
import com.bc.wechat.activity.BigImageActivity;
import com.bc.wechat.activity.MyUserInfoActivity;
import com.bc.wechat.activity.SettingActivity;
import com.bc.wechat.entity.User;
import com.bc.wechat.utils.PreferencesUtil;
import com.facebook.drawee.view.SimpleDraweeView;

/**
 * tab - "我"
 */
public class ProfileFragment extends Fragment implements View.OnClickListener {

    private RelativeLayout mMyInfoRl;
    private RelativeLayout mSettingRl;
    private SimpleDraweeView mAvatarSdv;
    private TextView mNickNameTv;
    private TextView mWxIdTv;
    User user;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PreferencesUtil.getInstance().init(getActivity());
        user = PreferencesUtil.getInstance().getUser();
        initView();

    }

    private void initView() {
        mMyInfoRl = getView().findViewById(R.id.rl_myinfo);
        mSettingRl = getView().findViewById(R.id.rl_setting);
        mAvatarSdv = getView().findViewById(R.id.sdv_avatar);
        mNickNameTv = getView().findViewById(R.id.tv_name);
        mWxIdTv = getView().findViewById(R.id.tv_wxid);

        mMyInfoRl.setOnClickListener(this);
        mSettingRl.setOnClickListener(this);
        mAvatarSdv.setOnClickListener(this);

        mNickNameTv.setText(PreferencesUtil.getInstance().getUserNickName());
        mWxIdTv.setText("微信号:" + PreferencesUtil.getInstance().getUserWxId());
        String userAvatar = user.getUserAvatar();
        if (!TextUtils.isEmpty(userAvatar)) {
            mAvatarSdv.setImageURI(Uri.parse(userAvatar));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            // 个人页面
            case R.id.rl_myinfo:
                startActivity(new Intent(getActivity(), MyUserInfoActivity.class));
                break;
            // 设置页面
            case R.id.rl_setting:
                startActivity(new Intent(getActivity(), SettingActivity.class));
                break;
            // 头像点击放大
            case R.id.sdv_avatar:
                Intent intent = new Intent(getActivity(), BigImageActivity.class);
                intent.putExtra("imgUrl", user.getUserAvatar());
                startActivity(intent);
                break;

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mNickNameTv.setText(PreferencesUtil.getInstance().getUserNickName());
        mWxIdTv.setText("微信号:" + PreferencesUtil.getInstance().getUserWxId());
    }
}
