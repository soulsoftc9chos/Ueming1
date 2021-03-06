package cn.hym.kuaidou.watcher;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.tencent.TIMFriendshipManager;
import com.tencent.TIMMessage;
import com.tencent.TIMUserProfile;
import com.tencent.TIMValueCallBack;
import com.tencent.av.sdk.AVRoomMulti;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.view.AVRootView;
import com.tencent.livesdk.ILVCustomCmd;
import com.tencent.livesdk.ILVLiveConfig;
import com.tencent.livesdk.ILVLiveConstants;
import com.tencent.livesdk.ILVLiveManager;
import com.tencent.livesdk.ILVLiveRoomOption;
import com.tencent.livesdk.ILVText;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cn.hym.kuaidou.BaseActivity;
import cn.hym.kuaidou.KuaiDouApplication;
import cn.hym.kuaidou.R;
import cn.hym.kuaidou.hostlive.HeartBeatRequest;
import cn.hym.kuaidou.hostlive.QuitRoomRequest;
import cn.hym.kuaidou.model.ChatMsgInfo;
import cn.hym.kuaidou.model.Constants;
import cn.hym.kuaidou.model.GiftCmdInfo;
import cn.hym.kuaidou.model.GiftInfo;
import cn.hym.kuaidou.utils.request.BaseRequest;
import cn.hym.kuaidou.view.BottomControlView;
import cn.hym.kuaidou.view.ChatMsgListView;
import cn.hym.kuaidou.view.ChatView;
import cn.hym.kuaidou.view.DanmuView;
import cn.hym.kuaidou.view.GiftFullView;
import cn.hym.kuaidou.view.GiftRepeatView;
import cn.hym.kuaidou.view.TitleView;
import cn.hym.kuaidou.view.VipEnterView;
import cn.hym.kuaidou.widget.GiftSelectDialog;
import cn.hym.kuaidou.widget.SizeChangeRelativeLayout;
import tyrantgit.widget.HeartLayout;

public class WatcherActivity extends BaseActivity {
    private static final String TAG = "gift";
    private SizeChangeRelativeLayout mSizeChangeLayout;
    private TitleView titleView;
    private AVRootView mLiveView;
    private BottomControlView mControlView;
    private ChatView mChatView;
    private ChatMsgListView mChatListView;
    private VipEnterView mVipEnterView;
    private DanmuView mDanmuView;
    private GiftSelectDialog giftSelectDialog;

    private Timer heartTimer = new Timer();
    private Random heartRandom = new Random();
    private HeartLayout heartLayout;
    private GiftRepeatView giftRepeatView;
    private GiftFullView giftFullView;

    private String hostId;
    private int mRoomId;

    private HeartBeatRequest mHeartBeatRequest = null;
    private Timer heartBeatTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watcher);
        findAllViews();
        joinRoom();
        initStatusBar(R.color.mainTranslucent);
    }

    private void joinRoom() {
        mRoomId = getIntent().getIntExtra("roomId", -1);
        hostId = getIntent().getStringExtra("hostId");
        if (mRoomId < 0 || TextUtils.isEmpty(hostId)) {
            return;
        }

        ILVLiveConfig liveConfig = KuaiDouApplication.getApplication().getLiveConfig();
        liveConfig.setLiveMsgListener(new ILVLiveConfig.ILVLiveMsgListener() {
            @Override
            public void onNewTextMsg(ILVText text, String SenderId, TIMUserProfile userProfile) {
                //?????????????????????
            }
            @Override
            public void onNewCustomMsg(ILVCustomCmd cmd, String id, TIMUserProfile userProfile) {
                //????????????????????????
                if (cmd.getCmd() == Constants.CMD_CHAT_MSG_LIST) {
                    String content = cmd.getParam();
                    ChatMsgInfo info = ChatMsgInfo.createListInfo(content, id, userProfile.getFaceUrl());
                    mChatListView.addMsgInfo(info);
                } else if (cmd.getCmd() == Constants.CMD_CHAT_MSG_DANMU) {
                    String content = cmd.getParam();
                    ChatMsgInfo info = ChatMsgInfo.createListInfo(content, id, userProfile.getFaceUrl());
                    mChatListView.addMsgInfo(info);

                    String name = userProfile.getNickName();
                    if (TextUtils.isEmpty(name)) {
                        name = userProfile.getIdentifier();
                    }
                    ChatMsgInfo danmuInfo = ChatMsgInfo.createDanmuInfo(content, id, userProfile.getFaceUrl(), name);
                    mDanmuView.addMsgInfo(danmuInfo);
                }//??????
                else if (cmd.getCmd() == Constants.CMD_CHAT_GIFT) {
                    //???????????????????????????
                    GiftCmdInfo giftCmdInfo = new Gson().fromJson(cmd.getParam(), GiftCmdInfo.class);
                    int giftId = giftCmdInfo.giftId;
                    String repeatId = giftCmdInfo.repeatId;
                    GiftInfo giftInfo = GiftInfo.getGiftById(giftId);
                    if (giftInfo == null) {
                        return;
                    }
                    if(giftInfo.giftId == GiftInfo.Gift_Heart.giftId){ //????????????
                        heartLayout.addHeart(getRandomColor());
                    }
                    else if (giftInfo.type == GiftInfo.Type.ContinueGift) {
                        giftRepeatView.showGift(giftInfo, repeatId, userProfile);
                    } else if (giftInfo.type == GiftInfo.Type.FullScreenGift) {
                        //????????????
                        giftFullView.showGift(giftInfo, userProfile);
                    }
                }
                else if (cmd.getCmd() == ILVLiveConstants.ILVLIVE_CMD_LEAVE) {
                    //??????????????????
                    if (hostId.equals(userProfile.getIdentifier())) {
                        //?????????????????????
                        quitRoom();
                    } else {
                        //??????????????????
                        titleView.removeWatcher(userProfile);
                    }
                } else if (cmd.getCmd() == ILVLiveConstants.ILVLIVE_CMD_ENTER) {
                    titleView.addWatcher(userProfile);
                    mVipEnterView.showVipEnter(userProfile);
                }
            }

            @Override
            public void onNewOtherMsg(TIMMessage message) {
                //?????????????????????
            }
        });

        //?????????????????????
        ILVLiveRoomOption memberOption = new ILVLiveRoomOption(hostId)
                .autoCamera(false) //???????????????????????????
                .controlRole("Guest") //????????????
                .authBits(AVRoomMulti.AUTH_BITS_JOIN_ROOM | AVRoomMulti.AUTH_BITS_RECV_AUDIO | AVRoomMulti.AUTH_BITS_RECV_CAMERA_VIDEO | AVRoomMulti.AUTH_BITS_RECV_SCREEN_VIDEO) //????????????
                .videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO) //???????????????????????????
                .autoMic(false);//??????????????????mic
        //????????????
        ILVLiveManager.getInstance().joinRoom(mRoomId, memberOption, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                //??????????????????
                startHeartAnim();
                //????????????????????????????????????
                sendEnterRoomMsg();
                //?????????????????????
                updateTitleView();
                //???????????????
                startHeartBeat();
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                Toast.makeText(WatcherActivity.this,"???????????????",Toast.LENGTH_SHORT).show();
                quitRoom();
            }
        });
    }

    private void startHeartBeat() {
        heartBeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //???????????????
                if (mHeartBeatRequest == null) {
                    mHeartBeatRequest = new HeartBeatRequest();
                }
                String roomId = mRoomId + "";
                String userId = KuaiDouApplication.getApplication().getSelfProfile().getIdentifier();
                String url = mHeartBeatRequest.getUrl(roomId, userId);
                mHeartBeatRequest.request(url);
            }
        }, 0, 4000); //4?????? ???????????????10????????????????????????
    }

    private void updateTitleView() {
        List<String> list = new ArrayList<String>();
        list.add(hostId);
        TIMFriendshipManager.getInstance().getUsersProfile(list, new TIMValueCallBack<List<TIMUserProfile>>() {
            @Override
            public void onError(int i, String s) {
                //?????????
                titleView.setHost(null);
            }

            @Override
            public void onSuccess(List<TIMUserProfile> timUserProfiles) {
                //???????????????????????????
                titleView.setHost(timUserProfiles.get(0));
            }
        });
        // ????????????????????????titleView??????
        titleView.addWatcher(KuaiDouApplication.getApplication().getSelfProfile());

        //???????????????????????????????????????
        GetWatcherRequest watcherRequest = new GetWatcherRequest();
        watcherRequest.setOnResultListener(new BaseRequest.OnResultListener<Set<String>>() {
            @Override
            public void onFail(int code, String msg) {

            }

            @Override
            public void onSuccess(Set<String> watchers) {
                if (watchers == null) {
                    return;
                }

                List<String> watcherList = new ArrayList<String>();
                watcherList.addAll(watchers);
                TIMFriendshipManager.getInstance().getUsersProfile(watcherList, new TIMValueCallBack<List<TIMUserProfile>>() {
                    @Override
                    public void onError(int i, String s) {
                        //?????????

                    }

                    @Override
                    public void onSuccess(List<TIMUserProfile> timUserProfiles) {
                        //????????????????????????????????????
                        titleView.addWatchers(timUserProfiles);
                    }
                });
            }
        });

        String watcherRequestUrl = watcherRequest.getUrl(mRoomId + "");
        watcherRequest.request(watcherRequestUrl);

    }

    private void sendEnterRoomMsg() {
        ILVCustomCmd customCmd = new ILVCustomCmd();
        customCmd.setType(ILVText.ILVTextType.eGroupMsg);
        customCmd.setCmd(ILVLiveConstants.ILVLIVE_CMD_ENTER);
        customCmd.setDestId(ILiveRoomManager.getInstance().getIMGroupId());
        ILVLiveManager.getInstance().sendCustomCmd(customCmd, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {

            }

            @Override
            public void onError(String module, int errCode, String errMsg) {

            }
        });
    }

    private void startHeartAnim() {
        heartTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                heartLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        heartLayout.addHeart(getRandomColor());
                    }
                });
            }
        }, 0, 1000); //1??????
    }

    private int getRandomColor() {
        return Color.rgb(heartRandom.nextInt(255), heartRandom.nextInt(255), heartRandom.nextInt(255));
    }

    private void findAllViews() {
        mSizeChangeLayout = (SizeChangeRelativeLayout) findViewById(R.id.size_change_layout);
        mSizeChangeLayout.setOnSizeChangeListener(new SizeChangeRelativeLayout.OnSizeChangeListener() {
            @Override
            public void onLarge() {
                //????????????
                mChatView.setVisibility(View.INVISIBLE);
                mControlView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSmall() {
                //????????????
            }
        });

        titleView = (TitleView) findViewById(R.id.title_view);

        mLiveView = (AVRootView) findViewById(R.id.live_view);
        ILVLiveManager.getInstance().setAvVideoView(mLiveView);

        mControlView = (BottomControlView) findViewById(R.id.control_view);
        mControlView.setIsHost(false);
        mControlView.setOnControlListener(new BottomControlView.OnControlListener() {
            @Override
            public void onChatClick() {
                //?????????????????????????????????????????????
                mChatView.setVisibility(View.VISIBLE);
                mControlView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCloseClick() {
                // ????????????????????????????????????
                quitRoom();
            }

            @Override
            public void onGiftClick() {
                //?????????????????????
                if (giftSelectDialog == null) {
                    giftSelectDialog = new GiftSelectDialog(WatcherActivity.this);

                    giftSelectDialog.setGiftSendListener(giftSendListener);
                }
                giftSelectDialog.show();
            }

            @Override
            public void onOptionClick(View view) {
                //????????????????????????????????????
            }
        });

        mChatView = (ChatView) findViewById(R.id.chat_view);
        mChatView.setOnChatSendListener(new ChatView.OnChatSendListener() {
            @Override
            public void onChatSend(final ILVCustomCmd customCmd) {
                //????????????
                customCmd.setDestId(ILiveRoomManager.getInstance().getIMGroupId());
                ILVLiveManager.getInstance().sendCustomCmd(customCmd, new ILiveCallBack<TIMMessage>() {
                    @Override
                    public void onSuccess(TIMMessage data) {
                        if (customCmd.getCmd() == Constants.CMD_CHAT_MSG_LIST) {
                            //??????????????????????????????????????????????????????
                            String chatContent = customCmd.getParam();
                            String userId = KuaiDouApplication.getApplication().getSelfProfile().getIdentifier();
                            String avatar = KuaiDouApplication.getApplication().getSelfProfile().getFaceUrl();
                            ChatMsgInfo info = ChatMsgInfo.createListInfo(chatContent, userId, avatar);
                            mChatListView.addMsgInfo(info);
                        } else if (customCmd.getCmd() == Constants.CMD_CHAT_MSG_DANMU) {
                            String chatContent = customCmd.getParam();
                            String userId = KuaiDouApplication.getApplication().getSelfProfile().getIdentifier();
                            String avatar = KuaiDouApplication.getApplication().getSelfProfile().getFaceUrl();
                            ChatMsgInfo info = ChatMsgInfo.createListInfo(chatContent, userId, avatar);
                            mChatListView.addMsgInfo(info);

                            String name = KuaiDouApplication.getApplication().getSelfProfile().getNickName();
                            if (TextUtils.isEmpty(name)) {
                                name = userId;
                            }
                            ChatMsgInfo danmuInfo = ChatMsgInfo.createDanmuInfo(chatContent, userId, avatar, name);
                            mDanmuView.addMsgInfo(danmuInfo);
                        }
                    }

                    @Override
                    public void onError(String module, int errCode, String errMsg) {
                    }

                });
            }
        });

        mControlView.setVisibility(View.VISIBLE);
        mChatView.setVisibility(View.INVISIBLE);

        mChatListView = (ChatMsgListView) findViewById(R.id.chat_list);
        mVipEnterView = (VipEnterView) findViewById(R.id.vip_enter);
        mDanmuView = (DanmuView) findViewById(R.id.danmu_view);

        heartLayout = (HeartLayout) findViewById(R.id.heart_layout);
        giftRepeatView = (GiftRepeatView) findViewById(R.id.gift_repeat_view);
        giftFullView = (GiftFullView) findViewById(R.id.gift_full_view);
        heartLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ILVCustomCmd customCmd = new ILVCustomCmd();
                customCmd.setCmd(Constants.CMD_CHAT_GIFT);
                customCmd.setType(ILVText.ILVTextType.eGroupMsg);
                customCmd.setDestId(ILiveRoomManager.getInstance().getIMGroupId());
                GiftCmdInfo giftCmdInfo = new GiftCmdInfo();
                giftCmdInfo.giftId = GiftInfo.Gift_Heart.giftId;
                customCmd.setParam(new Gson().toJson(giftCmdInfo));
                sendGiftMsg(customCmd);
            }
        });
    }

    private GiftSelectDialog.OnGiftSendListener giftSendListener = new GiftSelectDialog.OnGiftSendListener() {
        @Override
        public void onGiftSendClick(final ILVCustomCmd customCmd) {
            sendGiftMsg(customCmd);
        }
    };
    private void sendGiftMsg(final ILVCustomCmd customCmd){
        customCmd.setDestId(ILiveRoomManager.getInstance().getIMGroupId());

        ILVLiveManager.getInstance().sendCustomCmd(customCmd, new ILiveCallBack<TIMMessage>() {
            @Override
            public void onSuccess(TIMMessage data) {
                if (customCmd.getCmd() == Constants.CMD_CHAT_GIFT) {
                    //???????????????????????????
                    GiftCmdInfo giftCmdInfo = new Gson().fromJson(customCmd.getParam(), GiftCmdInfo.class);
                    int giftId = giftCmdInfo.giftId;
                    String repeatId = giftCmdInfo.repeatId;
                    GiftInfo giftInfo = GiftInfo.getGiftById(giftId);
                    if (giftInfo == null) {
                        return;
                    }
                    if(giftInfo.giftId == GiftInfo.Gift_Heart.giftId){ //????????????
                        heartLayout.addHeart(getRandomColor());
                    }
                    else if (giftInfo.type == GiftInfo.Type.ContinueGift) {
                        giftRepeatView.showGift(giftInfo, repeatId, KuaiDouApplication.getApplication().getSelfProfile());
                    } else if (giftInfo.type == GiftInfo.Type.FullScreenGift) {
                        //????????????
                        giftFullView.showGift(giftInfo, KuaiDouApplication.getApplication().getSelfProfile());
                    }
                }
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
            }

        });
    }
    @Override
    protected void onPause() {
        super.onPause();
        ILVLiveManager.getInstance().onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ILVLiveManager.getInstance().onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        heartTimer.cancel();
        heartBeatTimer.cancel();
    }

    @Override
    public void onBackPressed() {
        quitRoom();
    }

    private void quitRoom() {
        ILVCustomCmd customCmd = new ILVCustomCmd();
        customCmd.setType(ILVText.ILVTextType.eGroupMsg);
        customCmd.setCmd(ILVLiveConstants.ILVLIVE_CMD_LEAVE);
        customCmd.setDestId(ILiveRoomManager.getInstance().getIMGroupId());
        ILVLiveManager.getInstance().sendCustomCmd(customCmd, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                ILiveRoomManager.getInstance().quitRoom(new ILiveCallBack() {
                    @Override
                    public void onSuccess(Object data) {

                    }

                    @Override
                    public void onError(String module, int errCode, String errMsg) {
                        logout();
                    }
                });
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {

            }
        });

        //??????????????????????????????
        QuitRoomRequest request = new QuitRoomRequest();
        String roomId = mRoomId + "";
        String userId = KuaiDouApplication.getApplication().getSelfProfile().getIdentifier();
        String url = request.getUrl(roomId, userId);
        request.request(url);

        logout();
    }

    private void logout() {
//        ILiveLoginManager.getInstance().iLiveLogout(null);
        finish();
    }


}
