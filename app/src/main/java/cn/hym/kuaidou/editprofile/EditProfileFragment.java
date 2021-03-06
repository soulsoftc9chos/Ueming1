package cn.hym.kuaidou.editprofile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.tencent.TIMCallBack;
import com.tencent.TIMFriendGenderType;
import com.tencent.TIMFriendshipManager;
import com.tencent.TIMUserProfile;
import com.tencent.TIMValueCallBack;

import java.util.Map;

import cn.hym.kuaidou.R;
import cn.hym.kuaidou.main.MainActivity;
import cn.hym.kuaidou.utils.ImgUtils;
import cn.hym.kuaidou.utils.PicChooserHelper;

/**
 * Created by Administrator on 2018/4/25.
 */

public class EditProfileFragment extends Fragment {
    private static final int FROM_CAMERA = 2;
    private static final int FROM_ALBUM = 1;
    private static final int CROP = 0;
    private TIMUserProfile mUserProfile;

    private Toolbar mTitlebar;
    private View mAvatarView;
    private ImageView mAvatarImg;
    private ProfileEdit mNickNameEdt;
    private ProfileEdit mGenderEdt;
    private ProfileEdit mSignEdt;
    private ProfileEdit mRenzhengEdt;
    private ProfileEdit mLocationEdt;

    private ProfileTextView mIdView;
    private ProfileTextView mLevelView;
    private ProfileTextView mGetNumsView;
    private ProfileTextView mSendNumsView;

    private Button mCompleteBtn;

    private PicChooserHelper mPicChooserHelper;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        findAllViews(mainView);
        setListeners();
        setTitleBar();
        setIconKey();//???????????????icon
        getSelfInfo();

        return mainView;
    }
    private void setTitleBar() {
        mTitlebar.setTitle("??????????????????");
        mTitlebar.setTitleTextColor(Color.WHITE);
        Activity activity = getActivity();
        if (activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).setSupportActionBar(mTitlebar);
        }
    }

    private void setIconKey() {
        mNickNameEdt.set(R.mipmap.ic_info_nickname, "??????", "");
        mGenderEdt.set(R.mipmap.ic_info_gender, "??????", "");
        mSignEdt.set(R.mipmap.ic_info_sign, "??????", "???");
        mRenzhengEdt.set(R.mipmap.ic_info_renzhen, "??????", "??????");
        mLocationEdt.set(R.mipmap.ic_info_location, "??????", "??????");
        mIdView.set(R.mipmap.ic_info_id, "ID", "");
        mLevelView.set(R.mipmap.ic_info_level, "??????", "0");
        mGetNumsView.set(R.mipmap.ic_info_get, "????????????", "0");
        mSendNumsView.set(R.mipmap.ic_info_send, "????????????", "0");
    }

    private void findAllViews(View view) {
        mTitlebar = (Toolbar) view.findViewById(R.id.title_bar);

        mAvatarView = view.findViewById(R.id.avatar);
        mAvatarImg = (ImageView) view.findViewById(R.id.avatar_img);
        mNickNameEdt = (ProfileEdit) view.findViewById(R.id.nick_name);
        mGenderEdt = (ProfileEdit) view.findViewById(R.id.gender);
        mSignEdt = (ProfileEdit) view.findViewById(R.id.sign);
        mRenzhengEdt = (ProfileEdit) view.findViewById(R.id.renzheng);
        mLocationEdt = (ProfileEdit) view.findViewById(R.id.location);

        mIdView = (ProfileTextView) view.findViewById(R.id.id);
        mLevelView = (ProfileTextView) view.findViewById(R.id.level);
        mGetNumsView = (ProfileTextView) view.findViewById(R.id.get_nums);
        mSendNumsView = (ProfileTextView) view.findViewById(R.id.send_nums);

        mCompleteBtn = (Button) view.findViewById(R.id.complete);
    }
    private void setListeners() {
        mAvatarView.setOnClickListener(clickListener);
        mNickNameEdt.setOnClickListener(clickListener);
        mGenderEdt.setOnClickListener(clickListener);
        mSignEdt.setOnClickListener(clickListener);
        mRenzhengEdt.setOnClickListener(clickListener);
        mLocationEdt.setOnClickListener(clickListener);
        mCompleteBtn.setOnClickListener(clickListener);
    }
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.avatar) {
                //????????????
                choosePic();
            } else if (id == R.id.nick_name) {
                //????????????
                showEditNickNameDialog();
            } else if (id == R.id.gender) {
                //????????????
                showEditGenderDialog();
            } else if (id == R.id.sign) {
                //????????????
                showEditSignDialog();
            } else if (id == R.id.renzheng) {
                //????????????
                showEditRenzhengDialog();
            } else if (id == R.id.location) {
                //????????????
                showEditLocationDialog();
            } else if (id == R.id.complete) {
                //?????????????????????????????????
                Intent intent = new Intent();
                intent.setClass(getActivity(), MainActivity.class);
                startActivity(intent);
            }
        }
    };
    private void choosePic() {
        if (mPicChooserHelper == null) {
            mPicChooserHelper = new PicChooserHelper(this, PicChooserHelper.PicType.Avatar);
            mPicChooserHelper.setOnChooseResultListener(new PicChooserHelper.OnChooseResultListener() {
                @Override
                public void onSuccess(String url) {
                    updateAvatar(url);
                }

                @Override
                public void onFail(String msg) {
                    Toast.makeText(getActivity(), "???????????????" + msg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        mPicChooserHelper.showPicChooserDialog();
    }
    private void updateAvatar(String url) {
        TIMFriendshipManager.getInstance().setFaceUrl(url, new TIMCallBack() {

            @Override
            public void onError(int i, String s) {
                Toast.makeText(getActivity(), "?????????????????????" + s, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess() {
                //??????????????????
                getSelfInfo();
            }
        });
    }
    private void getSelfInfo() {
        TIMFriendshipManager.getInstance().getSelfProfile(new TIMValueCallBack<TIMUserProfile>() {
            @Override
            public void onError(int i, String s) {
                Toast.makeText(getActivity(), "?????????????????????" + s, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(TIMUserProfile timUserProfile) {
                //????????????????????????
                mUserProfile = timUserProfile;
                updateViews(timUserProfile);
            }
        });
    }
    private void updateViews(TIMUserProfile timUserProfile) {
        //????????????
        String faceUrl = timUserProfile.getFaceUrl();
        if (TextUtils.isEmpty(faceUrl)) {
            ImgUtils.loadRound(R.mipmap.default_avatar, mAvatarImg);
        } else {
            ImgUtils.loadRound(faceUrl, mAvatarImg);
        }
        mNickNameEdt.updateValue(timUserProfile.getNickName());
        long genderValue = timUserProfile.getGender().getValue();
        String genderStr = genderValue == 1 ? "???" : "???";
        mGenderEdt.updateValue(genderStr);
        mSignEdt.updateValue(timUserProfile.getSelfSignature());
        mLocationEdt.updateValue(timUserProfile.getLocation());
        mIdView.updateValue(timUserProfile.getIdentifier());

        Map<String, byte[]> customInfo = timUserProfile.getCustomInfo();
        mRenzhengEdt.updateValue(getValue(customInfo, CustomProfile.CUSTOM_RENZHENG, "??????"));
        mLevelView.updateValue(getValue(customInfo, CustomProfile.CUSTOM_LEVEL, "0"));
        mGetNumsView.updateValue(getValue(customInfo, CustomProfile.CUSTOM_GET, "0"));
        mSendNumsView.updateValue(getValue(customInfo, CustomProfile.CUSTOM_SEND, "0"));
    }

    private String getValue(Map<String, byte[]> customInfo, String key, String defaultValue) {
        if (customInfo != null) {
            byte[] valueBytes = customInfo.get(key);
            if (valueBytes != null) {
                return new String(valueBytes);
            }
        }
        return defaultValue;
    }
    private void showEditNickNameDialog() {
        EditStrProfileDialog dialog = new EditStrProfileDialog(getActivity());
        dialog.setOnOKListener(new EditStrProfileDialog.OnOKListener() {
            @Override
            public void onOk(String title, final String content) {
                TIMFriendshipManager.getInstance().setNickName(content, new TIMCallBack() {
                    @Override
                    public void onError(int i, String s) {
                        Toast.makeText(getActivity(), "?????????????????????" + s, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess() {
                        //????????????
                        getSelfInfo();
                    }
                });
            }
        });
        dialog.show("??????", R.mipmap.ic_info_nickname, mNickNameEdt.getValue());
    }
    private void showEditGenderDialog() {
        EditGenderDialog dialog = new EditGenderDialog(getActivity());
        dialog.setOnChangeGenderListener(new EditGenderDialog.OnChangeGenderListener() {
            @Override
            public void onChangeGender(boolean isMale) {
                TIMFriendGenderType gender = isMale ? TIMFriendGenderType.Male : TIMFriendGenderType.Female;
                TIMFriendshipManager.getInstance().setGender(gender, new TIMCallBack() {

                    @Override
                    public void onError(int i, String s) {
                        Toast.makeText(getActivity(), "?????????????????????" + s, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess() {
                        //????????????
                        getSelfInfo();
                    }
                });
            }
        });
        dialog.show(mGenderEdt.getValue().equals("???"));
    }

    private void showEditSignDialog() {
        EditStrProfileDialog dialog = new EditStrProfileDialog(getActivity());
        dialog.setOnOKListener(new EditStrProfileDialog.OnOKListener() {
            @Override
            public void onOk(String title, final String content) {
                TIMFriendshipManager.getInstance().setSelfSignature(content, new TIMCallBack() {
                    @Override
                    public void onError(int i, String s) {
                        Toast.makeText(getActivity(), "?????????????????????" + s, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess() {
                        //????????????
                        getSelfInfo();
                    }
                });
            }
        });
        dialog.show("??????", R.mipmap.ic_info_sign, mSignEdt.getValue());
    }
    private void showEditRenzhengDialog() {
        EditStrProfileDialog dialog = new EditStrProfileDialog(getActivity());
        dialog.setOnOKListener(new EditStrProfileDialog.OnOKListener() {
            @Override
            public void onOk(String title, final String content) {
                TIMFriendshipManager.getInstance().setCustomInfo(CustomProfile.CUSTOM_RENZHENG, content.getBytes(), new TIMCallBack() {
                    @Override
                    public void onError(int i, String s) {
                        Toast.makeText(getActivity(), "?????????????????????" + s, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess() {
                        //????????????
                        getSelfInfo();
                    }
                });
            }
        });
        dialog.show("??????", R.mipmap.ic_info_renzhen, mRenzhengEdt.getValue());
    }
    private void showEditLocationDialog() {
        EditStrProfileDialog dialog = new EditStrProfileDialog(getActivity());
        dialog.setOnOKListener(new EditStrProfileDialog.OnOKListener() {
            @Override
            public void onOk(String title, final String content) {
                TIMFriendshipManager.getInstance().setLocation(content, new TIMCallBack() {
                    @Override
                    public void onError(int i, String s) {
                        Toast.makeText(getActivity(), "?????????????????????" + s, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess() {
                        //????????????
                        getSelfInfo();
                    }
                });
            }
        });
        dialog.show("??????", R.mipmap.ic_info_location, mLocationEdt.getValue());
    }
}
