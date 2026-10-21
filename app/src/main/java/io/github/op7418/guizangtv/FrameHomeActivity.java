package io.github.op7418.guizangtv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.text.Collator;

/** Quiet, remote-first home inspired by CleanFrame; native Android implementation. */
public final class FrameHomeActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<TextView> rows = new ArrayList<>();
    private final List<TextView> controls = new ArrayList<>();
    private TextView clock;
    private TextView wallpaperButton;
    private FrameWallpapers wallpapers;
    private AlertDialog library;
    private int selected;
    private static final String PREFS = "frame-home";
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            clock.setText(new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date()));
            handler.postDelayed(this, 60000 - System.currentTimeMillis() % 60000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        selected = Math.max(0, Math.min(2, getSharedPreferences(PREFS, MODE_PRIVATE).getInt("selected", 0)));
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        ImageView photo = new ImageView(this);
        photo.setBackgroundColor(0xFF192D38);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(photo, new FrameLayout.LayoutParams(-1, -1));
        ImageView transition = new ImageView(this);
        transition.setScaleType(ImageView.ScaleType.CENTER_CROP);
        transition.setAlpha(0f);
        root.addView(transition, new FrameLayout.LayoutParams(-1, -1));
        wallpapers = new FrameWallpapers(this, photo, transition);
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0x990C0C0C, 0x360C0C0C, 0x700C0C0C}));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams listParams = new FrameLayout.LayoutParams(dp(590), -2, Gravity.START | Gravity.CENTER_VERTICAL);
        listParams.leftMargin = dp(64);
        root.addView(list, listParams);
        addRow(list, "Apple TV", "HDMI 2", () -> hdmi(2));
        addRow(list, "哔哩哔哩", null, () -> openPackage("com.xiaodianshi.tv.yst"));
        addRow(list, "小米投屏", null, () -> openPackage("com.xiaomi.mitv.smartshare"));

        clock = text("", 26);
        clock.setGravity(Gravity.END);
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(dp(160), dp(45), Gravity.TOP | Gravity.END);
        clockParams.topMargin = dp(45);
        clockParams.rightMargin = dp(64);
        root.addView(clock, clockParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setGravity(Gravity.END);
        FrameLayout.LayoutParams actionsParams = new FrameLayout.LayoutParams(dp(180), -2, Gravity.END | Gravity.BOTTOM);
        actionsParams.rightMargin = dp(64);
        actionsParams.bottomMargin = dp(46);
        root.addView(actions, actionsParams);
        addControl(actions, "壁纸设置", () -> wallpapers.show(wallpaperButton));
        wallpaperButton = controls.get(0);
        addControl(actions, "全部应用", this::showLibrary);
        addControl(actions, "其他信号源", () -> {
            if (!XiaomiControls.showInputs(this)) message("暂时无法打开信号源");
        });
        addControl(actions, "设置", () -> BootStart.settings(this, () -> {
            Intent intent = XiaomiControls.settings(this, Settings.ACTION_SETTINGS);
            launch(intent, "暂时无法打开设置");
        }));
        for (int i = 0; i < rows.size(); i++) {
            TextView row = rows.get(i);
            row.setNextFocusUpId(rows.get(Math.max(0, i - 1)).getId());
            row.setNextFocusDownId(rows.get(Math.min(rows.size() - 1, i + 1)).getId());
            row.setNextFocusLeftId(row.getId());
            row.setNextFocusRightId(controls.get(0).getId());
        }
        for (int i = 0; i < controls.size(); i++) {
            TextView control = controls.get(i);
            control.setNextFocusUpId(controls.get(Math.max(0, i - 1)).getId());
            control.setNextFocusDownId(controls.get(Math.min(controls.size() - 1, i + 1)).getId());
            control.setNextFocusRightId(control.getId());
        }
        setContentView(root);
        rows.get(selected).requestFocus();
    }

    private void addRow(LinearLayout list, String name, String port, Runnable action) {
        int index = rows.size();
        TextView label = text(name, 38);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setPivotX(0);
        label.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        label.setContentDescription(port == null ? name : name + "，" + port);
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setClipChildren(false);
        line.setClipToPadding(false);
        list.setClipChildren(false);
        line.addView(label, new LinearLayout.LayoutParams(-2, dp(65)));
        TextView tag = text(port == null ? "" : port, 12);
        tag.setTextColor(0xBFFFFFFF);
        LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(-2, -2);
        tagParams.leftMargin = dp(24);
        line.addView(tag, tagParams);
        list.addView(line);
        rows.add(label);
        wire(label, action);
        label.setOnFocusChangeListener((v, focused) -> {
            if (focused) {
                selected = index;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("selected", index).apply();
                for (TextView control : controls) control.setNextFocusLeftId(label.getId());
            }
            label.animate().alpha(focused ? 1f : .55f)
                    .scaleX(focused ? 1.035f : 1f).scaleY(focused ? 1.035f : 1f).setDuration(160).start();
            tag.animate().alpha(focused ? 1f : .65f).setDuration(160).start();
        });
        label.setAlpha(.55f);
    }

    private void addControl(LinearLayout list, String name, Runnable action) {
        TextView control = text(name, 16);
        control.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        control.setAlpha(.65f);
        wire(control, action);
        control.setOnFocusChangeListener((v, focused) -> {
            control.animate().alpha(focused ? 1f : .65f).setDuration(140).start();
            control.setTypeface(Typeface.create("sans-serif", focused ? Typeface.BOLD : Typeface.NORMAL));
        });
        controls.add(control);
        list.addView(control, new LinearLayout.LayoutParams(-1, dp(36)));
    }

    private void wire(TextView view, Runnable action) {
        view.setId(View.generateViewId());
        view.setFocusable(true);
        view.setClickable(true);
        view.setOnClickListener(v -> action.run());
        // Long presses must not launch repeatedly. Android generates click on key-up.
        view.setOnKeyListener((v, code, event) ->
                (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER)
                        && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() > 0);
    }

    private void hdmi(int port) {
        if (!XiaomiControls.openHdmi(this, port)) message("暂时无法切换 HDMI " + port + "，可使用其他信号源");
    }

    private void openPackage(String pkg) {
        Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = getPackageManager().getLaunchIntentForPackage(pkg);
        launch(intent, "应用暂时不可用");
    }

    private void launch(Intent intent, String error) {
        if (intent == null) { message(error); return; }
        try { startActivity(intent); }
        catch (RuntimeException unavailable) { message(error); }
    }

    private void showLibrary() {
        Map<String, String> apps = new LinkedHashMap<>();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            for (ResolveInfo app : getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
                String pkg = app.activityInfo.packageName;
                if (getPackageName().equals(pkg) || !app.activityInfo.exported || !app.activityInfo.enabled) continue;
                apps.put(pkg, XiaomiControls.label(pkg, app.loadLabel(getPackageManager()).toString()));
            }
        }
        List<String> packages = new ArrayList<>(apps.keySet());
        Collator collator = Collator.getInstance(Locale.CHINA);
        Collections.sort(packages, (a, b) -> collator.compare(apps.get(a), apps.get(b)));
        String[] names = new String[packages.size()];
        for (int i = 0; i < names.length; i++) names[i] = apps.get(packages.get(i));
        library = new AlertDialog.Builder(this).setTitle("全部应用")
                .setItems(names, (dialog, which) -> openPackage(packages.get(which)))
                .setNegativeButton("返回", null).create();
        library.setOnDismissListener(dialog -> controls.get(1).requestFocus());
        library.show();
    }

    @Override protected void onResume() {
        super.onResume();
        BootStart.confirm(this);
        if (clock != null) { handler.removeCallbacks(tick); tick.run(); }
        if (wallpapers != null) wallpapers.resume();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(tick);
        if (wallpapers != null) wallpapers.pause();
        if (library != null) library.dismiss();
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (wallpapers != null) wallpapers.destroy();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (!rows.isEmpty() && controls.contains(getCurrentFocus())) rows.get(selected).requestFocus();
    }

    @Override public void dump(String prefix, java.io.FileDescriptor fd, java.io.PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        if (wallpapers != null) wallpapers.dump(writer);
        BootStart.dump(this, writer);
    }

    private TextView text(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setFontFeatureSettings("tnum");
        view.setIncludeFontPadding(false);
        view.setShadowLayer(2, 0, 1, 0x33000000);
        return view;
    }

    private void message(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
