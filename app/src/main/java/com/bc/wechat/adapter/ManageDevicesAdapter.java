package com.bc.wechat.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bc.wechat.R;
import com.bc.wechat.cons.Constant;
import com.bc.wechat.dao.DeviceInfoDao;
import com.bc.wechat.entity.DeviceInfo;
import com.bc.wechat.entity.User;
import com.bc.wechat.observer.UpdateUiInterface;
import com.bc.wechat.utils.PreferencesUtil;
import com.bc.wechat.utils.VolleyUtil;
import com.bc.wechat.widget.NoTitleConfirmDialog;

import java.util.List;

/**
 * 登录设备管理
 *
 * @author zhou
 */
public class ManageDevicesAdapter extends BaseAdapter {

    private Context mContext;
    private List<DeviceInfo> mLoginDeviceList;
    VolleyUtil mVolleyUtil;
    User mUser;
    DeviceInfoDao mDeviceInfoDao;
    UpdateUiInterface mUpdateUiInterface;

    public ManageDevicesAdapter(Context context, List<DeviceInfo> loginDeviceList, UpdateUiInterface updateUiInterface) {
        this.mContext = context;
        this.mLoginDeviceList = loginDeviceList;
        this.mUpdateUiInterface = updateUiInterface;
        mVolleyUtil = VolleyUtil.getInstance(mContext);
        mUser = PreferencesUtil.getInstance().getUser();
        mDeviceInfoDao = new DeviceInfoDao();
    }

    public void setData(List<DeviceInfo> dataList) {
        this.mLoginDeviceList = dataList;
    }

    public void updateChangedItem(int position, DeviceInfo newDeviceInfo) {
        mLoginDeviceList.set(position, newDeviceInfo);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mLoginDeviceList.size();
    }

    @Override
    public DeviceInfo getItem(int position) {
        return mLoginDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        DeviceInfo deviceInfo = mLoginDeviceList.get(position);

        ViewHolder viewHolder;
        if (null == convertView) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_devices, null);
            viewHolder.mRootRl = convertView.findViewById(R.id.rl_root);
            viewHolder.mPhoneModelTv = convertView.findViewById(R.id.tv_phone_model);
            viewHolder.mLoginTimeTv = convertView.findViewById(R.id.tv_login_time);
            viewHolder.mMoreIv = convertView.findViewById(R.id.iv_more);
            viewHolder.mDeleteTv = convertView.findViewById(R.id.tv_delete);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (TextUtils.isEmpty(deviceInfo.getPhoneModelAlias())) {
            viewHolder.mPhoneModelTv.setText(deviceInfo.getPhoneBrand() + "-" + deviceInfo.getPhoneModel());
        } else {
            viewHolder.mPhoneModelTv.setText(deviceInfo.getPhoneModelAlias());
        }

        if (!TextUtils.isEmpty(deviceInfo.getLoginTime())) {
            viewHolder.mLoginTimeTv.setText(deviceInfo.getLoginTime().substring(2));
        }

        if (deviceInfo.isEdit()) {
            viewHolder.mMoreIv.setVisibility(View.GONE);
            viewHolder.mDeleteTv.setVisibility(View.VISIBLE);
            // ???
            viewHolder.mRootRl.setClickable(true);
        } else {
            viewHolder.mMoreIv.setVisibility(View.VISIBLE);
            viewHolder.mDeleteTv.setVisibility(View.GONE);
            // ???
            viewHolder.mRootRl.setClickable(false);
        }

        viewHolder.mDeleteTv.setOnClickListener(view -> {
            String content = "确定要将 “" + deviceInfo.getPhoneBrand() + "-" + deviceInfo.getPhoneModel() + "” 从常用设备中删除？";
            final NoTitleConfirmDialog mNoTitleConfirmDialog = new NoTitleConfirmDialog(mContext,
                    content,
                    mContext.getString(R.string.delete), mContext.getString(R.string.cancel), mContext.getColor(R.color.navy_blue));
            mNoTitleConfirmDialog.setOnDialogClickListener(new NoTitleConfirmDialog.OnDialogClickListener() {
                @Override
                public void onOkClick() {
                    mNoTitleConfirmDialog.dismiss();
                    deleteDeviceByDeviceId(mUser.getUserId(), deviceInfo.getDeviceId());
                }

                @Override
                public void onCancelClick() {
                    mNoTitleConfirmDialog.dismiss();
                }
            });
            // 点击空白处消失
            mNoTitleConfirmDialog.setCancelable(true);
            mNoTitleConfirmDialog.show();
        });

        return convertView;
    }

    class ViewHolder {
        RelativeLayout mRootRl;
        TextView mPhoneModelTv;
        TextView mLoginTimeTv;
        ImageView mMoreIv;
        TextView mDeleteTv;
    }

    private void deleteDeviceByDeviceId(String userId, String deviceId) {
        String url = Constant.BASE_URL + "users/" + userId + "/devices/" + deviceId;
        mVolleyUtil.httpDeleteRequest(url, response -> {
            mDeviceInfoDao.deleteDeviceInfoByDeviceId(deviceId);
            mUpdateUiInterface.updateUi();
        }, volleyError -> {
        });
    }


}