package com.bc.wechat.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bc.wechat.R;
import com.bc.wechat.entity.User;
import com.bc.wechat.utils.PreferencesUtil;
import com.bc.wechat.widget.ConfirmDialog;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 绑定手机号
 *
 * @author zhou
 */
public class PhoneLinkActivity extends BaseActivity2 {
    private static final int REQUEST_CODE_CONTACTS = 0;

    @BindView(R.id.tv_title)
    TextView mTitleTv;

    @BindView(R.id.tv_phone)
    TextView mPhoneTv;

    @BindView(R.id.btn_mobile_contacts)
    Button mMobileContactsBtn;

    @BindView(R.id.btn_change_mobile)
    Button mChangeMobileBtn;

    User mUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_phone);
        ButterKnife.bind(this);
        initStatusBar();

        mUser = PreferencesUtil.getInstance().getUser();
        initView();
    }

    private void initView() {
        setTitleStrokeWidth(mTitleTv);
        mPhoneTv.setText("绑定的手机号：" + mUser.getUserPhone());
    }

    public void back(View view) {
        finish();
    }

    @OnClick({R.id.btn_mobile_contacts, R.id.btn_change_mobile})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_mobile_contacts:
                String[] permissions = new String[]{"android.permission.READ_CONTACTS"};
                requestPermissions(PhoneLinkActivity.this, permissions, REQUEST_CODE_CONTACTS);
                break;
            case R.id.btn_change_mobile:
                break;
        }
    }

    /**
     * 动态权限
     */
    public void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        // Android 6.0开始的动态权限，这里进行版本判断
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> mPermissionList = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(activity, permissions[i])
                        != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (mPermissionList.isEmpty()) {
                // 非初次进入App且已授权
                switch (requestCode) {
                    case REQUEST_CODE_CONTACTS:
                        showPhoneContact();
                        break;
                }

            } else {
                // 请求权限方法
                String[] requestPermissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                // 这个触发下面onRequestPermissionsResult这个回调
                ActivityCompat.requestPermissions(activity, requestPermissions, requestCode);
            }
        }
    }

    /**
     * requestPermissions的回调
     * 一个或多个权限请求结果回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasAllGranted = true;
        // 判断是否拒绝  拒绝后要怎么处理 以及取消再次提示的处理
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                hasAllGranted = false;
                break;
            }
        }
        if (hasAllGranted) {
            switch (requestCode) {
                case REQUEST_CODE_CONTACTS:
                    // 同意通讯录权限,开启通讯录服务
                    showPhoneContact();
                    break;
            }
        } else {
            // 拒绝授权做的处理，弹出弹框提示用户授权
            handleRejectPermission(PhoneLinkActivity.this, permissions[0], requestCode);
        }
    }

    public void handleRejectPermission(final Activity context, String permission, int requestCode) {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
            String content = "";
            switch (requestCode) {
                case REQUEST_CODE_CONTACTS:
                    content = getString(R.string.request_permission_contacts);
                    break;
            }
            final ConfirmDialog mConfirmDialog = new ConfirmDialog(PhoneLinkActivity.this, getString(R.string.request_permission),
                    content,
                    getString(R.string.go_setting), getString(R.string.cancel), getColor(R.color.navy_blue));
            mConfirmDialog.setOnDialogClickListener(new ConfirmDialog.OnDialogClickListener() {
                @Override
                public void onOkClick() {
                    mConfirmDialog.dismiss();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", context.getApplicationContext().getPackageName(), null);
                    intent.setData(uri);
                    context.startActivity(intent);
                }

                @Override
                public void onCancelClick() {
                    mConfirmDialog.dismiss();
                }
            });
            // 点击空白处消失
            mConfirmDialog.setCancelable(false);
            mConfirmDialog.show();
        }
    }

    /**
     * 查看手机通讯录
     */
    private void showPhoneContact() {
        startActivity(new Intent(PhoneLinkActivity.this, MobileContactsActivity.class));
    }

}
