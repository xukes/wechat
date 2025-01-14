package com.bc.wechat.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.alibaba.fastjson.JSONArray;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bc.wechat.R;
import com.bc.wechat.adapter.MomentsAdapter;
import com.bc.wechat.cons.Constant;
import com.bc.wechat.entity.Moments;
import com.bc.wechat.entity.MomentsComment;
import com.bc.wechat.entity.User;
import com.bc.wechat.moments.adapter.Utils;
import com.bc.wechat.moments.bean.ExplorePostPinglunBean;
import com.bc.wechat.listener.MomentsListener;
import com.bc.wechat.moments.utils.CustomDotIndexProvider;
import com.bc.wechat.moments.utils.CustomLoadingUIProvider;
import com.bc.wechat.moments.utils.GlideSimpleLoader;
import com.bc.wechat.moments.utils.KeyboardUtil;
import com.bc.wechat.utils.CollectionUtils;
import com.bc.wechat.utils.PreferencesUtil;
import com.bc.wechat.utils.VolleyUtil;
import com.bc.wechat.widget.LikeAndCommentPopupWindow;
import com.bc.wechat.listener.LikeOrCommentClickListener;
import com.bc.wechat.utils.StatusBarUtil;
import com.bc.wechat.views.CustomProgressDrawable;
import com.bc.wechat.views.CustomSwipeRefreshLayout;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import byc.imagewatcher.ImageWatcher;
import byc.imagewatcher.ImageWatcherHelper;

/**
 * 朋友圈
 *
 * @author zhou
 */
public class MomentsActivity extends BaseActivity2 implements MomentsListener, ImageWatcher.OnPictureLongPressListener {

    @BindView(R.id.srl_moments)
    CustomSwipeRefreshLayout mMomentsSrl;

    @BindView(R.id.rl_title)
    RelativeLayout mTitleRl;

    SimpleDraweeView mAvatarSdv;

    @BindView(R.id.rv_moments)
    RecyclerView mMomentsRv;

    private MomentsAdapter mAdapter;
    List<Moments> mList = new ArrayList<>();

    private LinearLayout llComment;
    private EditText etComment;

    @BindView(R.id.btn_send)
    Button tvSend;

    LikeAndCommentPopupWindow mLikeAndCommentPopupWindow;

    public ImageWatcherHelper iwHelper;//方式二

    VolleyUtil mVolleyUtil;
    User mUser;
    MomentsComment mMomentsComment = new MomentsComment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moments);
        ButterKnife.bind(this);
        mVolleyUtil = VolleyUtil.getInstance(this);
        mUser = PreferencesUtil.getInstance().getUser();
        StatusBarUtil.setStatusBarColor(MomentsActivity.this, R.color.color_moments_default_cover);

        llComment = findViewById(R.id.ll_comment);
        etComment = findViewById(R.id.et_comment);
        tvSend = findViewById(R.id.btn_send);
        boolean isTranslucentStatus = false;
        //        新的初始化方式二，不再需要在布局文件中加入<ImageWatcher>标签 减少布局嵌套
        iwHelper = ImageWatcherHelper.with(this, new GlideSimpleLoader()) // 一般来讲， ImageWatcher 需要占据全屏的位置
                .setTranslucentStatus(!isTranslucentStatus ? Utils.calcStatusBarHeight(this) : 0) // 如果不是透明状态栏，你需要给ImageWatcher标记 一个偏移值，以修正点击ImageView查看的启动动画的Y轴起点的不正确
                .setErrorImageRes(R.mipmap.error_picture) // 配置error图标 如果不介意使用lib自带的图标，并不一定要调用这个API
                .setOnPictureLongPressListener(this)
                .setOnStateChangedListener(new ImageWatcher.OnStateChangedListener() {
                    @Override
                    public void onStateChangeUpdate(ImageWatcher imageWatcher, ImageView clicked, int position, Uri uri, float animatedValue, int actionTag) {
                        Log.e("IW", "onStateChangeUpdate [" + position + "][" + uri + "][" + animatedValue + "][" + actionTag + "]");
                    }

                    @Override
                    public void onStateChanged(ImageWatcher imageWatcher, int position, Uri uri, int actionTag) {
                        if (actionTag == ImageWatcher.STATE_ENTER_DISPLAYING) {
                            //  Toast.makeText(getApplicationContext(), "点击了图片 [" + position + "]" + uri + "", Toast.LENGTH_SHORT).show();
                        } else if (actionTag == ImageWatcher.STATE_EXIT_HIDING) {
                            //  Toast.makeText(getApplicationContext(), "退出了查看大图", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setIndexProvider(new CustomDotIndexProvider())//自定义页码指示器（默认数字）
                .setLoadingUIProvider(new CustomLoadingUIProvider()); // 自定义LoadingUI


//        setData();

        mAdapter = new MomentsAdapter(mList, this, this);
        mAdapter.setIwHelper(iwHelper);
        mMomentsRv.setLayoutManager(new LinearLayoutManager(this));
        mMomentsRv.setAdapter(mAdapter);
        setHeader(mMomentsRv);

        mMomentsRv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideInput();
                return false;
            }
        });

        CustomProgressDrawable drawable = new CustomProgressDrawable(this, mMomentsSrl);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.moments_refresh_icon);
        drawable.setBitmap(bitmap);

        mMomentsSrl.setProgressView(drawable);
        mMomentsSrl.setBackgroundColor(Color.BLACK);
        mMomentsSrl.setProgressBackgroundColorSchemeColor(Color.TRANSPARENT);//背景设置透明
        mMomentsSrl.setOnRefreshListener(new CustomSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                final Handler handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        mMomentsSrl.setRefreshing(false);
                    }
                };
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        handler.sendEmptyMessage(0);
                    }
                }).start();

            }
        });

        getFriendMomentsListByUserId(mUser.getUserId());
    }

    @OnClick({R.id.btn_send})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send:
                addComment(mMomentsComment.getUserId(), mMomentsComment.getMomentsId(),
                        mMomentsComment.getReplyToUserId(), etComment.getText().toString(), 1);
                break;
        }
    }

    private void setHeader(RecyclerView view) {
        View header = LayoutInflater.from(this).inflate(R.layout.item_my_moments_header, view, false);
        mAvatarSdv = header.findViewById(R.id.sdv_avatar);
        mAdapter.setHeaderView(header);
    }

    //评论
    @Override
    public void onPinlunEdit(View view, int friendid, String userid, String userName) {
        showCommentWindow(view, userid, userName, null);
    }

    /**
     * 点击昵称,头像进入用户详情页
     *
     * @param userId 用户ID
     */
    @Override
    public void toUserInfo(String userId) {
        if (mUser.getUserId().equals(userId)) {
            Intent intent = new Intent(MomentsActivity.this, UserInfoMyActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(MomentsActivity.this, UserInfoActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        }
    }

    /**
     * 点击，弹出弹框，包含点赞，评论
     *
     * @param view     view
     * @param position 位置
     */
    @Override
    public void onClickLikeAndComment(View view, int position) {
        // 点赞评论弹框
        showLikeAndCommentPopupWindow(view, position);
    }

    //点击删除朋友圈按钮
    @Override
    public void deletePengyouquan(int id) {

    }

    //删除评论按钮
    @Override
    public void deleteMypinglun(int ids, ExplorePostPinglunBean id) {

    }

    //点击图片
    @Override
    public void imageOnclick() {

    }

    //点击视频
    @Override
    public void videoOnclick(String img, String httpUrl) {
        hideInput();
        Intent dynamicHaoyou = new Intent(this, ExploreVideoPlayer.class);
        dynamicHaoyou.putExtra("typeImg", img);
        dynamicHaoyou.putExtra("typeHttpUrl", httpUrl);
        startActivity(dynamicHaoyou);
    }

    public void hideInput() {
        llComment.setVisibility(View.GONE);
        KeyboardUtil.hideSoftInput(this, etComment);
    }

    /**
     * 显示点赞评论弹框
     *
     * @param view     view
     * @param position 位置
     */
    private void showLikeAndCommentPopupWindow(final View view, int position) {
        Moments moments = mList.get(position);
        //item 底部y坐标
        int isLike = 0;
        if (!CollectionUtils.isEmpty(moments.getLikeUserList())) {
            if (moments.getLikeUserList().contains(mUser)) {
                isLike = 1;
            }
        }

        final int mBottomY = getCoordinateY(view) + view.getHeight();
        if (mLikeAndCommentPopupWindow == null) {
            mLikeAndCommentPopupWindow = new LikeAndCommentPopupWindow(this, isLike);
        }
        mLikeAndCommentPopupWindow.setLikeAndCommentPopupWindow(new LikeOrCommentClickListener() {

            @Override
            public void onLikeClick(int position) {
                mLikeAndCommentPopupWindow.dismiss();
                // 调用点赞和取消点赞接口
                if (mLikeAndCommentPopupWindow.isLike() == 0) {
                    likeMoments(moments.getUserId(), moments.getMomentsId(), position);
                } else {
                    unLikeMoments(moments.getUserId(), moments.getMomentsId(), position);
                }
            }

            @Override
            public void onCommentClick(int position) {
                mLikeAndCommentPopupWindow.dismiss();
                showCommentWindow(view, null, null, moments);
            }

            @Override
            public void onClickFrendCircleTopBg() {

            }

            @Override
            public void onDeleteItem(String id, int position) {

            }

        }).setIsLike(isLike).setTextView(isLike).setCurrentPosition(position);
        if (mLikeAndCommentPopupWindow.isShowing()) {
            mLikeAndCommentPopupWindow.dismiss();
        } else {
            mLikeAndCommentPopupWindow.showPopupWindow(view);
        }
    }

    /**
     * 获取控件左上顶点Y坐标
     *
     * @param view
     * @return
     */
    private int getCoordinateY(View view) {
        int[] coordinate = new int[2];
        view.getLocationOnScreen(coordinate);
        return coordinate[1];
    }

    @Override
    public void onPictureLongPress(ImageView v, Uri uri, int pos) {

    }

    @Override
    public void onBackPressed() {
        //方式二
        if (!iwHelper.handleBackPressed()) {
            super.onBackPressed();
        }
    }

    private void showCommentWindow(View view, String userId, String userName, Moments moments) {
        //item 底部y坐标
        final int mBottomY = getCoordinateY(view) + view.getHeight();
        llComment.setVisibility(View.VISIBLE);
        etComment.requestFocus();
        if (TextUtils.isEmpty(userId)) {
            // 回复这条评论的发送人，楼主
            etComment.setHint("评论");
        } else {
            // 回复这个人
            etComment.setHint("回复:" + userName);
        }
        etComment.setText("");
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                int y = getCoordinateY(llComment);// - 20;
                //评论时滑动到对应item底部和输入框顶部对齐
                mMomentsRv.smoothScrollBy(0, mBottomY - y);
            }
        }, 300);
        mMomentsComment.setMomentsId(moments.getMomentsId());
        mMomentsComment.setReplyToUserId(userId);
        mMomentsComment.setUserId(moments.getUserId());
    }

    private void getFriendMomentsListByUserId(String userId) {
        String url = Constant.BASE_URL + "users/" + userId + "/friendMoments";
        mVolleyUtil.httpGetRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                final List<Moments> momentsList = JSONArray.parseArray(response, Moments.class);
                mList = momentsList;
                mAdapter.setData(momentsList);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });
    }

    /**
     * 朋友圈点赞
     *
     * @param userId    用户ID
     * @param momentsId 朋友圈ID
     * @param position  位置
     */
    private void likeMoments(String userId, String momentsId, int position) {
        String url = Constant.BASE_URL + "users/" + userId + "/moments/" + momentsId + "/like";
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("likeUserId", mUser.getUserId());
        mVolleyUtil.httpPostRequest(url, paramMap, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Moments moments = mList.get(position);
                List<User> likeUserList = moments.getLikeUserList();
                likeUserList.add(mUser);
                moments.setLikeUserList(likeUserList);
                mAdapter.notifyDataSetChanged();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });
    }

    /**
     * 朋友圈取消点赞
     *
     * @param userId    用户ID
     * @param momentsId 朋友圈ID
     * @param position  位置
     */
    private void unLikeMoments(String userId, String momentsId, int position) {
        String url = Constant.BASE_URL + "users/" + userId + "/moments/"
                + momentsId + "/like?likeUserId=" + mUser.getUserId();
        mVolleyUtil.httpDeleteRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Moments moments = mList.get(position);
                List<User> likeUserList = moments.getLikeUserList();
                likeUserList.remove(mUser);
                moments.setLikeUserList(likeUserList);
                mAdapter.notifyDataSetChanged();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });
    }

    /**
     * 朋友圈点赞
     *
     * @param userId    用户ID
     * @param momentsId 朋友圈ID
     * @param position  位置
     */
    private void addComment(String userId, String momentsId, String replyToUserId,
                            String content, int position) {
        String url = Constant.BASE_URL + "users/" + userId + "/moments/" + momentsId + "/comment";
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("commentUserId", mUser.getUserId());
        if (!TextUtils.isEmpty(replyToUserId)) {
            paramMap.put("replyToUserId", replyToUserId);
        }
        paramMap.put("content", content);
        mVolleyUtil.httpPostRequest(url, paramMap, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        });
    }

}