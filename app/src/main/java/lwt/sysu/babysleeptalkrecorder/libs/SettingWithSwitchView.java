package lwt.sysu.babysleeptalkrecorder.libs;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.orhanobut.hawk.Hawk;

import lwt.sysu.babysleeptalkrecorder.R;

/**
 * 这个是设置页面的那个选择录音音质的那个框框的自定义View
 * 就是一个切换按钮
 * 然后切换之后框框的效果和文字的高亮处理
 * */

public class SettingWithSwitchView extends FrameLayout implements View.OnClickListener {
    private final String preferenceKey;
    @StringRes
    private final int titleRes;
    @StringRes
    private final int captionRes;
    private final boolean defaultValue;
    private TextView title;
    private TextView caption;
    private SwitchCompat toggle;
    @Nullable
    private OnClickListener clickListener;

    public SettingWithSwitchView(Context context) {
        this(context, null);
    }

    public SettingWithSwitchView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingWithSwitchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setBackgroundResource(R.drawable.ripple);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View rootView = inflater.inflate(R.layout.view_setting_switch, this);
        initViews(rootView);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SettingWithSwitchView);
        final int prefKeyRes =
                a.getResourceId(R.styleable.SettingWithSwitchView_settingPreferenceKey, 0);
        if (prefKeyRes == 0) throw new IllegalArgumentException("Invalid preference reference");
        preferenceKey = getResources().getString(prefKeyRes);
        titleRes = a.getResourceId(R.styleable.SettingWithSwitchView_settingTitle, 0);
        captionRes = a.getResourceId(R.styleable.SettingWithSwitchView_settingCaption, 0);
        defaultValue = a.getBoolean(R.styleable.SettingWithSwitchView_settingDefaultValue, false);
        int minimumApi = 0;//a.getInteger(R.styleable.SettingWithSwitchView_settingMinApi, 0);
        a.recycle();

        if (Build.VERSION.SDK_INT < minimumApi) setVisibility(GONE);
    }

    private void initViews(View rootView) {
        title = rootView.findViewById(R.id.title);
        caption = rootView.findViewById(R.id.caption);
        toggle = rootView.findViewById(R.id.toggle);
    }

    @Override
    protected void onFinishInflate() {
        title.setText(titleRes); // 设置标题
        caption.setText(captionRes); // 设置提示内容
        toggle.setChecked(isChecked()); // 设置是否开关为选中状态
        // 如果是选中状态那么高亮title,也是搞成绿色啦
        if (isChecked()) title.setTextColor(getResources().getColor(R.color.md_green_600));
        // 没有选中的情况下就是灰色的
        else title.setTextColor(getResources().getColor(R.color.md_grey_200));

        super.setOnClickListener(this);
        super.onFinishInflate();
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onClick(View view) {
        toggle();
        if (clickListener != null) clickListener.onClick(this);
    }

    // 检查开关是否处于选中状态，这个是需要保存的一个状态，使用Hawk简单的一个键值对数据库存储
    public boolean isChecked() {
        return Hawk.get(preferenceKey, defaultValue);
    }

    // 触发开关
    public boolean toggle() {
        // 触发之后将开关的反状态重新储存
        Hawk.put(preferenceKey, !isChecked());
        boolean checked = isChecked();

        // 选中状态高亮，非选中灰色
        if (isChecked()) title.setTextColor(getResources().getColor(R.color.md_green_600));
        else title.setTextColor(getResources().getColor(R.color.md_grey_200));
        // 设置开关的状态，显示出来
        toggle.setChecked(checked);
        return checked;
    }
}
