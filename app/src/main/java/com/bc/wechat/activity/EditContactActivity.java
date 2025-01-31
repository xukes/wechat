package com.bc.wechat.activity;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.bc.wechat.R;
import com.bc.wechat.cons.Constant;
import com.bc.wechat.dao.UserDao;
import com.bc.wechat.entity.User;
import com.bc.wechat.utils.DensityUtil;
import com.bc.wechat.utils.PreferencesUtil;
import com.bc.wechat.utils.VolleyUtil;
import com.bc.wechat.widget.LoadingDialog;
import com.zhy.view.flowlayout.FlowLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 编辑联系人(设置备注和标签)
 *
 * @author zhou
 */
public class EditContactActivity extends BaseActivity {

    @BindView(R.id.tv_title)
    TextView mTitleTv;

    @BindView(R.id.et_alias)
    EditText mAliasEt;

    @BindView(R.id.et_desc)
    EditText mDescEt;

    // 添加电话
    @BindView(R.id.rl_mobiles)
    RelativeLayout mMobilesRl;

    @BindView(R.id.rl_mobiles_container)
    RelativeLayout mMobilesContainerRl;

    @BindView(R.id.ll_mobile)
    LinearLayout mMobileLl;

    @BindView(R.id.et_mobile)
    EditText mMobileEt;

    @BindView(R.id.iv_clear_mobile)
    ImageView mClearMobileIv;

    // 保存
    @BindView(R.id.tv_save)
    TextView mSaveTv;

    @BindView(R.id.tv_add_tag)
    TextView mAddTagTv;

    @BindView(R.id.fl_add_tag)
    FlowLayout mAddTagFl;

    LinearLayout.LayoutParams mParams;

    String mContactId;
    User mContact;
    User mUser;
    UserDao mUserDao;
    LoadingDialog mDialog;
    VolleyUtil mVolleyUtil;

    @Override
    public int getContentView() {
        return R.layout.activity_edit_contact;
    }

    @Override
    public void initView() {
        initStatusBar();
        setTitleStrokeWidth(mTitleTv);
    }

    @Override
    public void initListener() {

    }

    @Override
    public void initData() {
        mVolleyUtil = VolleyUtil.getInstance(this);
        mUser = PreferencesUtil.getInstance().getUser();
        mUserDao = new UserDao();
        mContactId = getIntent().getStringExtra("contactId");
        mDialog = new LoadingDialog(EditContactActivity.this);
        mParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginLeft = DensityUtil.dip2px(EditContactActivity.this, 10);
        int marginTop = DensityUtil.dip2px(EditContactActivity.this, 10);
        mParams.setMargins(marginLeft, marginTop, 0, 0);
        loadTags();

        final String isFriend = getIntent().getStringExtra("isFriend");
        mContact = mUserDao.getUserById(mContactId);

        if (TextUtils.isEmpty(mContact.getUserContactAlias())) {
            // 无备注，展示昵称
            mAliasEt.setText(mContact.getUserNickName());
        } else {
            // 有备注，展示备注
            mAliasEt.setText(mContact.getUserContactAlias());
        }

        mDescEt.setText(mContact.getUserContactDesc());

        if (Constant.IS_NOT_FRIEND.equals(isFriend)) {
            // 非好友不能添加电话
            mMobilesRl.setVisibility(View.GONE);
            mMobilesContainerRl.setVisibility(View.GONE);
        }

        mMobileEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String mobile = s.toString();
                if (!TextUtils.isEmpty(mobile)) {
                    if (mMobileLl.getChildCount() <= 1) {
                        addMobileView("");
                    }
                    mClearMobileIv.setVisibility(View.VISIBLE);
                } else {
                    mClearMobileIv.setVisibility(View.GONE);
                }
            }
        });
        renderMobile();

    }

    public void back(View view) {
        finish();
    }

    /**
     * 设置联系人
     *
     * @param userId    用户ID
     * @param contactId 联系人ID
     * @param alias     联系人备注名
     * @param mobiles   联系人电话(可以多个号码,json格式)
     * @param desc      联系人描述
     */
    private void editContact(final String userId, final String contactId, final String alias,
                             final String mobiles, final String desc) {
        String url = Constant.BASE_URL + "users/" + userId + "/contacts/" + contactId;

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("contactAlias", alias);
        paramMap.put("contactMobiles", mobiles);
        paramMap.put("contactDesc", desc);

        mVolleyUtil.httpPutRequest(url, paramMap, response -> {
            mDialog.dismiss();
            User user = mUserDao.getUserById(contactId);
            user.setUserContactAlias(alias);
            user.setUserContactMobiles(mobiles);
            user.setUserContactDesc(desc);
            mUserDao.saveUser(user);
            finish();
        }, volleyError -> {
            mDialog.dismiss();
            showNoTitleAlertDialog(EditContactActivity.this, "设置联系人失败", "确定");
        });
    }

    @OnClick({R.id.rl_add_tag, R.id.iv_clear_mobile, R.id.tv_save})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_add_tag:
                Intent intent = new Intent(EditContactActivity.this, AddTagActivity.class);
                intent.putExtra("contactId", mContactId);
                startActivity(intent);
                break;
            case R.id.iv_clear_mobile:
                mMobileEt.setText("");
                break;
            case R.id.tv_save:
                String alias = mAliasEt.getText().toString();
                if (alias.length() > 16) {
                    showAlertDialog(EditContactActivity.this, "提示",
                            "你输入的备注过长", "确定", true);
                    return;
                }
                List<String> mobileList = getMobileList();
                String mobiles = JSON.toJSONString(mobileList);
                String desc = mDescEt.getText().toString();

                mDialog.setMessage("设置中...");
                mDialog.setCanceledOnTouchOutside(false);
                mDialog.show();
                editContact(mUser.getUserId(), mContactId, alias, mobiles, desc);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTags();
    }

    /**
     * 创建一个正常状态的标签
     *
     * @param label
     * @return
     */
    private TextView getTag(String label) {
        TextView textView = new TextView(getApplicationContext());
        textView.setTextSize(14);
        textView.setBackgroundResource(R.drawable.label_normal);
        textView.setTextColor(getColor(R.color.register_btn_bg_enable));
        textView.setText(label);
        textView.setLayoutParams(mParams);
        return textView;
    }

    private void loadTags() {
        mContact = mUserDao.getUserById(mContactId);
        List<String> selectedTagList = mContact.getUserContactTagList();

        if (null != selectedTagList && selectedTagList.size() > 0) {
            mAddTagTv.setVisibility(View.GONE);
            mAddTagFl.setVisibility(View.VISIBLE);
            mAddTagFl.removeAllViews();
            for (String selectedTag : selectedTagList) {
                // 添加标签
                final TextView temp = getTag(selectedTag);
                mAddTagFl.addView(temp);
            }
        } else {
            mAddTagTv.setVisibility(View.VISIBLE);
            mAddTagFl.setVisibility(View.GONE);
        }
    }

    private void addMobileView(String mobile) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                DensityUtil.dip2px(this, 58));
        View view = LayoutInflater.from(this).inflate(R.layout.item_contact_mobile, null);
        view.setLayoutParams(lp);
        EditText mobileEt = view.findViewById(R.id.et_mobile);
        ImageView clearMobileIv = view.findViewById(R.id.iv_clear_mobile);
        if (!TextUtils.isEmpty(mobile)) {
            mobileEt.setText(mobile);
            clearMobileIv.setVisibility(View.VISIBLE);
        } else {
            clearMobileIv.setVisibility(View.GONE);
        }
        mobileEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String mobile = s.toString();
                if (!TextUtils.isEmpty(mobile)) {
                    if (view == mMobileLl.getChildAt(mMobileLl.getChildCount() - 1)) {
                        addMobileView("");
                    }
                    clearMobileIv.setVisibility(View.VISIBLE);
                } else {
                    mMobileLl.removeView(view);
                    View lastView = mMobileLl.getChildAt(mMobileLl.getChildCount() - 1);
                    EditText lastMobileEt = lastView.findViewById(R.id.et_mobile);
                    lastMobileEt.requestFocus();
                    clearMobileIv.setVisibility(View.GONE);
                }
            }
        });
        clearMobileIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mobileEt.setText("");
            }
        });
        mMobileLl.addView(view);
    }

    /**
     * 获取所有的电话号码
     *
     * @return 所有的电话号码
     */
    private List<String> getMobileList() {
        List<String> mobileList = new ArrayList<>();
        int childCount = mMobileLl.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = mMobileLl.getChildAt(i);
            EditText mobileEt = view.findViewById(R.id.et_mobile);
            String mobile = mobileEt.getText().toString();
            if (!TextUtils.isEmpty(mobile)) {
                mobileList.add(mobile);
            }
        }
        return mobileList;
    }

    /**
     * 渲染电话
     */
    private void renderMobile() {
        List<String> mobileList;
        try {
            mobileList = JSON.parseArray(mContact.getUserContactMobiles(), String.class);
            if (null == mobileList) {
                mobileList = new ArrayList<>();
            }
        } catch (Exception e) {
            mobileList = new ArrayList<>();
        }
        if (mobileList.size() >= 1) {
            mMobileEt.setText(mobileList.get(0));
            // 渲染默认电话输入框会触发默认输入框的textChange事件,多出一个新的电话输入框,需清除
            if (mMobileLl.getChildCount() == 2) {
                mMobileLl.removeViewAt(1);
            }
            for (int i = 1; i < mobileList.size(); i++) {
                addMobileView(mobileList.get(i));
            }
            addMobileView("");
        }
    }
}
