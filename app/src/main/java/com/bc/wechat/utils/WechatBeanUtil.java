package com.bc.wechat.utils;

import com.bc.wechat.entity.Friend;
import com.bc.wechat.entity.User;

public class WechatBeanUtil {
    public static Friend transferUserToFriend(User user) {
        Friend friend = new Friend();
        friend.setUserId(user.getUserId());
        friend.setUserNickName(user.getUserNickName());
        friend.setUserWxId(user.getUserWxId());
        friend.setUserAvatar(user.getUserAvatar());
        friend.setUserHeader(CommonUtil.setUserHeader(user.getUserNickName()));
        friend.setUserSex(user.getUserSex());
        friend.setUserLastestCirclePhotos(user.getUserLastestCirclePhotos());

        friend.setUserFriendRemark(user.getUserFriendRemark());
        friend.setUserFriendPhone(user.getUserFriendPhone());
        friend.setUserFriendDesc(user.getUserFriendDesc());

        return friend;
    }
}
