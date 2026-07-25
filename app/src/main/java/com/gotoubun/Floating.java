package com.gotoubun;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.Random;

/**
 * GAERIS Floating Mod Menu – Extended Edition
 *
 * Dump-verified RVA index (Free Fire IL2CPP arm64):
 * ──────────────────────────────────────────────────────────────────────────────
 *  TrailRendererEffect.<init>          RVA 0x6D1AB30
 *  TrailRenderer.set_startColor        RVA 0x4E8388C
 *  TrailRenderer.set_endColor          RVA 0x4E83950
 *  WeaponSkinHelper.DoChange           RVA 0xABC53FC
 *  WeaponSkinHelper.DoChange3P         RVA 0xABC5A54
 *  C2SCSInventoryChgWeaponSkinReq      RVA 0x6BC9D90
 *  SetWeaponSkinResult                 RVA 0x9384A4C
 *  Pawn.get_MaxMoveSpeed               RVA 0x4F959CC  (slot 154)
 *  Pawn.set_MaxMoveSpeed               RVA 0x4F959D4  (slot 155)
 *  Pawn.DoJump                         RVA 0x5008528  (slot 773)
 *  Pawn.DoFalling                      RVA 0x4FFFEA4
 *  Pawn.GetGravityScale                RVA 0xBD166A0  (slot 20)
 *  WeaponIni.GetRecoilUpBase           RVA 0x96239E8
 *  WeaponIni.GetRecoilLateralBase      RVA 0x9623AD0
 *  WeaponIni.GetRecoilUpMax            RVA 0x96236CC
 *  WeaponIni.GetRecoilLateralMax       RVA 0x9623740
 *  WeaponIni.GetRecoilUpModifier       RVA 0x9623A5C
 *  WeaponIni.GetRecoilLateralModifier  RVA 0x9623B44
 *  BRPlayerPawn.get_Health             RVA 0x5DD839C  (slot 42)
 *  BRPlayerPawn.set_Health             RVA 0x5DD83A4  (slot 43)
 *  LocalPlayer.<init>                  RVA 0x71AD500
 *  Weapon.m_RecoilUpBase               offset 0x70C
 *  Weapon.m_RecoilLateralBase          offset 0x718
 *  Weapon.m_RecoilUpMax                offset 0x714
 *  Weapon.m_fireSpeed                  offset 0x220
 *  BRPlayerPawn.Health                 offset (via get_Health slot 42)
 *  LocalPlayer.NickName                offset 0x40
 *  LocalPlayer.ActorID                 offset 0x38
 *  CSInventoryChgWeaponSkinReq:
 *      weapon_id       offset 0x1C
 *      weapon_skin_id  offset 0x20
 *      weapon_guid     offset 0x24
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class Floating extends Service {

    static {
        System.loadLibrary("nino");
    }

    // ─── Window & Display ──────────────────────────────────────────────────────
    WindowManager windowManager;
    int screenWidth, screenHeight, type, screenDpi;
    float density;

    WindowManager.LayoutParams iconLayoutParams, mainLayoutParams;
    RelativeLayout iconLayout;
    LinearLayout mainLayout;
    CanvasView canvasLayout;

    ImageView iconImg;
    TextView textTitle;

    SharedPreferences configPrefs;

    // ─── Tab System ────────────────────────────────────────────────────────────
    // Tab 0 = Visual (ESP + Chams)
    // Tab 1 = Weapon (Tracer / Skin / No-Recoil / Spread / RapidFire)
    // Tab 2 = Aim (Aimbot / Silent / Magic / Prediction)
    // Tab 3 = Movement (Speed / Jump / Gravity / Fly)
    // Tab 4 = Anti-Detect (Stealth / FakeLag / AntiCheat spoof)
    // Tab 5 = Settings (Config / Theme / Display)
    String[] listTab = new String[]{"Visual", "Weapon", "Aim", "Move", "Stealth", "Config"};
    LinearLayout[] pageLayouts = new LinearLayout[listTab.length];
    int lastSelectedPage = 0;

    String M_RAND_TITLE = "-";
    long sleepTime = 4000 / 60;
    boolean isMaximized = false;

    int layoutWidth    = 100;
    int layoutHeight   = 200;
    int iconSize       = 40;
    int menuButtonSize = 30;
    int tabWidth       = 150;
    int tabHeight      = 50;

    // ─── Theme state ──────────────────────────────────────────────────────────
    // 0=Cyan  1=Magenta  2=Red  3=Green  4=Gold  5=Purple
    private int currentTheme = 0;
    private static final String[][] THEME_COLORS = {
            {"#00FFFF", "#FF00FF", "#33FF00FF", "#1A00FFFF"},  // Cyan (active, inactive)
            {"#FF00FF", "#00FFFF", "#3300FFFF", "#1AFF00FF"},  // Magenta
            {"#FF3333", "#FF8800", "#33FF8800", "#1AFF3333"},  // Red/Orange
            {"#00FF66", "#AAFFAA", "#3300FF66", "#1A00FF66"},  // Green
            {"#FFD700", "#FFAA00", "#33FFD700", "#1AFFAA00"},  // Gold
            {"#CC44FF", "#8844FF", "#33CC44FF", "#1A8844FF"},  // Purple
    };

    // ─── Preset indices ───────────────────────────────────────────────────────
    // 0=Aggressive  1=Stealth  2=Casual  3=Custom
    private int currentPreset = 3;

    // ─── Native Methods ────────────────────────────────────────────────────────
    /** Core config bridge – forwards key/value to the native hack_thread. */
    private native void onSendConfig(String s, String v);
    /** Draws ESP overlays (lines, boxes, skeleton, health bars, crosshair, FOV ring). */
    public static native void onCanvasDraw(Canvas canvas, int w, int h, float d);
    /** One-shot switch for binary features (indexes mirror hack_thread enum). */
    static native void Switch(int i, boolean jboolean1);
    /** Reads the randomised title string from the native side. */
    native String Title();

    // ══════════════════════════════════════════════════════════════════════════
    //  SERVICE LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void onCreate() {
        super.onCreate();
        configPrefs = getSharedPreferences("config", MODE_PRIVATE);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Point screenSize = new Point();
        Display display = windowManager.getDefaultDisplay();
        display.getRealSize(screenSize);

        screenWidth  = screenSize.x;
        screenHeight = screenSize.y;
        screenDpi    = getResources().getDisplayMetrics().densityDpi;
        density      = getResources().getDisplayMetrics().density;

        layoutWidth    = convertSizeToDp(420);
        layoutHeight   = convertSizeToDp(245);
        iconSize       = convertSizeToDp(40);
        menuButtonSize = convertSizeToDp(30);
        tabWidth       = convertSizeToDp(0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = 2038;
        } else {
            type = 2002;
        }

        // Restore saved theme / preset before building UI
        currentTheme  = configPrefs.getInt("META::THEME",  0);
        currentPreset = configPrefs.getInt("META::PRESET", 3);

        CreateIcon();
        CreateLayout();
        CreateCanvas();
        mUpdateThread.start();
        mUpdateCanvas.start();

        // Push all saved configs to native on startup
        RestoreAllConfigToNative();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mUpdateCanvas.isAlive())  mUpdateCanvas.interrupt();
        if (mUpdateThread.isAlive())  mUpdateThread.interrupt();
        if (iconLayout   != null)     windowManager.removeView(iconLayout);
        if (mainLayout   != null)     windowManager.removeView(mainLayout);
        if (canvasLayout != null)     windowManager.removeView(canvasLayout);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ══════════════════════════════════════════════════════════════════════════
    //  CANVAS
    // ══════════════════════════════════════════════════════════════════════════

    void CreateCanvas() {
        int LAYOUT_FLAG = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

        WindowManager.LayoutParams canvasLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                0, getNavigationBarHeight(),
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.RGBA_8888);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            canvasLayoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

        canvasLayoutParams.gravity = Gravity.TOP | Gravity.START;
        canvasLayoutParams.x = 0;
        canvasLayoutParams.y = 0;

        canvasLayout = new CanvasView(this);
        windowManager.addView(canvasLayout, canvasLayoutParams);
    }

    private int getNavigationBarHeight() {
        boolean hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey();
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0 && !hasMenuKey)
            return getResources().getDimensionPixelSize(resourceId);
        return 0;
    }

    private class CanvasView extends View {
        public CanvasView(Context context) { super(context); }

        @Override
        protected void onDraw(Canvas canvas) {
            try {
                onCanvasDraw(canvas, screenWidth, screenHeight, density);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CONFIG  (read/write + restore)
    // ══════════════════════════════════════════════════════════════════════════

    private void UpdateConfiguration(String s, Object v) {
        try {
            onSendConfig(s, v.toString());
            SharedPreferences.Editor ed = configPrefs.edit();
            ed.putString(s, v.toString());
            ed.apply();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Replay every persisted config key into the native layer after a cold start. */
    private void RestoreAllConfigToNative() {
        try {
            Map<String, ?> all = configPrefs.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                if (!entry.getKey().startsWith("META::")) {
                    onSendConfig(entry.getKey(), entry.getValue().toString());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Wipe every config key from SharedPreferences and send zeroes to native. */
    private void ResetAllConfiguration() {
        try {
            Map<String, ?> all = configPrefs.getAll();
            SharedPreferences.Editor ed = configPrefs.edit();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                if (!entry.getKey().startsWith("META::")) {
                    ed.remove(entry.getKey());
                    onSendConfig(entry.getKey(), "0");
                }
            }
            ed.apply();
            Toast.makeText(this, "Config reset!", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ─── Preset payloads ──────────────────────────────────────────────────────
    private void ApplyPreset(int preset) {
        currentPreset = preset;
        configPrefs.edit().putInt("META::PRESET", preset).apply();
        switch (preset) {
            case 0: // Aggressive – max everything visible
                UpdateConfiguration("ESP::LINE",      1);
                UpdateConfiguration("ESP::BOX",       1);
                UpdateConfiguration("ESP::HEALTH",    1);
                UpdateConfiguration("ESP::NAME",      1);
                UpdateConfiguration("ESP::SKELETON",  1);
                UpdateConfiguration("ESP::DISTANCE",  1);
                UpdateConfiguration("AIM::AIMBOT",    1);
                UpdateConfiguration("AIM::LOCATION",  0); // Head
                UpdateConfiguration("AIM::SIZE",      300);
                UpdateConfiguration("AIM::SILENT",    1);
                UpdateConfiguration("AIM::MAGIC",     1);
                UpdateConfiguration("WPN::NORECOIL",  1);
                UpdateConfiguration("WPN::NOSPREAD",  1);
                UpdateConfiguration("WPN::RAPIDFIRE", 1);
                UpdateConfiguration("MOV::SPEEDMUL",  150);
                break;
            case 1: // Stealth – subtle tweaks only
                UpdateConfiguration("ESP::LINE",      0);
                UpdateConfiguration("ESP::BOX",       0);
                UpdateConfiguration("ESP::HEALTH",    1);
                UpdateConfiguration("ESP::NAME",      1);
                UpdateConfiguration("ESP::SKELETON",  0);
                UpdateConfiguration("AIM::AIMBOT",    1);
                UpdateConfiguration("AIM::LOCATION",  1); // Chest
                UpdateConfiguration("AIM::SIZE",      120);
                UpdateConfiguration("AIM::SILENT",    0);
                UpdateConfiguration("AIM::MAGIC",     0);
                UpdateConfiguration("WPN::NORECOIL",  1);
                UpdateConfiguration("WPN::NOSPREAD",  0);
                UpdateConfiguration("WPN::RAPIDFIRE", 0);
                UpdateConfiguration("MOV::SPEEDMUL",  115);
                break;
            case 2: // Casual – tiny quality-of-life
                UpdateConfiguration("ESP::LINE",      0);
                UpdateConfiguration("ESP::BOX",       1);
                UpdateConfiguration("ESP::HEALTH",    1);
                UpdateConfiguration("AIM::AIMBOT",    0);
                UpdateConfiguration("WPN::NORECOIL",  1);
                UpdateConfiguration("MOV::SPEEDMUL",  105);
                break;
            default:
                break; // Custom – do nothing
        }
        RestoreAllConfigToNative();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ICON
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressLint("ClickableViewAccessibility")
    void CreateIcon() {
        iconLayout = new RelativeLayout(this);
        iconLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        iconImg = new ImageView(this);
        iconImg.setLayoutParams(new ViewGroup.LayoutParams(iconSize, iconSize));
        iconLayout.addView(iconImg);

        try {
            String iconBase64 = "iVBORw0KGgoAAAANSUhEUgAAALUAAAC1CAYAAAAZU76pAAAgAElEQVR4nOy9CZhk51UleP63xx6REblnZda+SlXaF0uyJG+yJcsGYwPGGLptMHsPNAOYxbQ/umEYxmBoA83wNdgGhsYLnpZlYxvJsvZdKpWqSrVX5b7HHvH297/57n2RWSWbxjQjtapK9fsLVyqXWN677777n3vOubi0Lq2LbYlX+vPEcfwKPyEA1wc0A4Hu4ZM/9/MDc08+/QvXf/C9f/ZDP/LjU0gXAU1DKACNfl9IeIgQyxiWNABNrr0xQAggVpLnXHtor+zbvbT+dUuIVy4UlfP9HEQUhykFUCLo0sRPfuw3l0f7Ml3r8189/afvee+9X/mrz7wBngtN9IKUDlAsYSoaEAYIIRIiQiREEscCkAIIESMUr/AFeGmdF+v8z9QAfEjEsYAZCrh6jPknvp46+dHfO7TByG+eimLc353/bc9Hfuj33v2hf3t/XlrQYhNQFPgKYKy/L0DGoBzOgZ18cgHzFX+3l9a/Zr2Smfr8D+qwAyDLX9ZCD32mCUgfj/z3v90y81t/9NiNIyODtuNh2rOxH97J0bvf/sd3/8zP/Xkq2+9YQgVCCaEq33lPiiWXKhCX6o/zYb2ugtqDA1Om4Hgx1FQMI+zVxDrw1T/7vautz37hWztK5ZzuBfCFjuNtG0dCLO16/3v+7MYPv/+P0qViPYaCEAp8KaEoCiyoXKJQeQL9Uq4+H9brKqjbFL8yhFAAM5Jw/BimZkIJAC/t4ZHf+u3bWw8+/LV9qmIaoQ+HNoJmAVU3xJHF2c7i3pG/vPEtb/3E9e+4ayY9vhERdPgQMKBCfUXf6aX1/2e9vooPD4AZAG4HsVVAGwpMSEgESEkTLSXEQ7/0az86vP/5T6ecFSF1ATXS0WfmIcMIK2kDU92Oe8zzP+uPjf353jvuev6md9yJvpFRyABQLiXq82K9voL6uyyKecVu42//7Yd/9LqO+5fSriuuGsOMNQg3wmrOhWWlUWiESIcC+2uLs5O68qWBG2/6wra73/nY5ttui3OZIhSoUKjEjnqYkAKEcQAtEIBKeGGCnqwdMCppaNMpuLAB/0T28EH6b36eAIjNGIJQljhGHEcQVOfTPYJQmAhQLt0ueF0K6nNXFANqjM7CDP7bT/3sL7zJjf5A8duYbNSwZ9PlaHeXIToufBFDZk0IGSMjMphvOJh17GOrEn+HbaOf37jXm1665p3vRGVwDFFAiAsQ6xrSMoG2gx5iqMQJtL12Dhj+Ru+HUiKUESJVQFWTUFeh9A6yXD8+URRDUTQCaC6t3roU1C9bHiBNuApQnz2B+378J3/1Tpi/43kBDjfqGOxLYbQRY8lto7Mhh1SsQKvbUDUDlmXBDiQ8oWDGtw+d0fC5zL7L79n3znce2n3dTXG6WEFTxJx5jbgH/0W916ZMixiBJjjI1bAXt5zlJT/o5zLS+IQJhbK6OJvpGWMUEJcyNa9LQX3OCimofQ2arqJBLZXZE/jGB3/64zcg9R9aRoBZ3cZlHR26E6KZFmhHISIZwEwb0BSg5OnwZIhIKIgNA81YwXQQ1WZ15bFWxnp431vf9OhVN73huQ2XXx5QapVBCKGbiBQNFMdWpCRHca35I3p4ODV4BGCd864ADICTs0dZlhZJGAhMKCTFUGJBvSnW+Y/QAKSGihtgxqOJkGVBYqySpV0uSV4Dke3g4EKjrQFqkOJi2XA4d6bA7vvQzDqQ7Wc2SMIaSIBFVajWQEGh4BCpIAEHfF0CcRk8rCeQJNjhGIHKvL6t2FqBNIKnSekHHW2VPNbGEBLXTuqSy0i7w2OIlMtZaAZP+x2T0dSSwYJO6QBDNI2xE0MXFB0Y/aGbfqVU5XStaHVGfO8cBf1JN5BfvqQ+D5PL0Y6BtNRpMbN0nZ2P0JdJHo4kC6PY4QeGSvDZEJ9qvTOPVhH5cPH2PMxYRqGc4iqfO14SQEUCjVhI9yl6wfn2sKK1tVXe2FYDt9IYQ5MaA2z2GzKA/TyoFD9o2G5lkMdX0b5H1l3y3tIjnFfVkJXRGAl9abumH5Ai83niqBiGRVDfSFxJMMF97xt/XCjh9xF+CIDiAR3bWSlT2h9KAiAqGFMDIE/gUB6sG8TJYE0ZvvlHjfVl9BqRgADtR1h9WaJ0AYKnBcMqSirz2ALVYqfcSlDn7Wkol3nBRp3Hq6qjdqhEBl7x4pFVbmcpE4MSWNTYWJGR7KhJOV9yNXjE4K5p28gxb4Q+r/vHVtAn8FJ5Ck9bZfQvqYjOQmxZ9YOWiWqYz+AkYEqfqZEpPv0y0bC5TOiF5oHqjQlHgXxY3hhlWEqFVHqJOh0agVo4Z";
            byte[] iconData = Base64.decode(iconBase64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
            iconImg.setImageBitmap(bmp);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        iconLayout.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = iconLayoutParams.x - event.getRawX();
                        dY = iconLayoutParams.y - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        iconLayoutParams.x = (int)(event.getRawX() + dX);
                        iconLayoutParams.y = (int)(event.getRawY() + dY);
                        windowManager.updateViewLayout(iconLayout, iconLayoutParams);
                        break;
                }
                return false;
            }
        });

        iconImg.setOnClickListener(v -> {
            iconLayout.setVisibility(View.GONE);
            mainLayout.setVisibility(View.VISIBLE);
        });

        iconLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                PixelFormat.RGBA_8888);
        iconLayoutParams.gravity = Gravity.START | Gravity.TOP;
        iconLayoutParams.x = 0;
        iconLayoutParams.y = 200;

        windowManager.addView(iconLayout, iconLayoutParams);
        iconLayout.setVisibility(View.GONE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN LAYOUT
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressLint("ClickableViewAccessibility")
    void CreateLayout() {
        mainLayoutParams = new WindowManager.LayoutParams(
                layoutWidth, layoutHeight,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                PixelFormat.RGBA_8888);
        mainLayoutParams.x = 0;
        mainLayoutParams.y = 0;
        mainLayoutParams.gravity = Gravity.START | Gravity.TOP;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            mainLayoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        GradientDrawable mainBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        Color.parseColor("#E5080810"),
                        Color.parseColor("#E50A0518")
                });
        mainBg.setCornerRadius(18f);
        mainBg.setStroke(1, Color.parseColor("#55" + getThemeHex(0).substring(1)));
        mainLayout.setBackground(mainBg);

        // ── Header ────────────────────────────────────────────────────────────
        RelativeLayout headerLayout = new RelativeLayout(this);
        headerLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                menuButtonSize + convertSizeToDp(4)));
        headerLayout.setClickable(true);
        headerLayout.setFocusable(true);
        headerLayout.setFocusableInTouchMode(true);
        headerLayout.setPadding(10, 5, 10, 5);
        headerLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        textTitle = new TextView(this);
        textTitle.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        textTitle.setGravity(Gravity.CENTER);
        textTitle.setTextSize(18);
        textTitle.setText("GAERIS");
        try {
            textTitle.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/fox.ttf"));
        } catch (Exception ignored) {}
        textTitle.setTextColor(Color.parseColor(getThemeHex(0)));
        textTitle.setShadowLayer(12f, 0, 0, Color.parseColor(getThemeHex(0)));

        // ── Close / toggle button (✕) ────────────────────────────────────────
        // Dedicated close button anchored to the right of the header.
        // This replaces the broken textTitle.setOnClickListener approach —
        // the drag touch listener was consuming taps on the title before the
        // click event could fire, so the menu closed but the icon never
        // became visible (both disappeared).
        TextView closeBtn = new TextView(this);
        RelativeLayout.LayoutParams closeLp = new RelativeLayout.LayoutParams(
                menuButtonSize + convertSizeToDp(6),
                menuButtonSize + convertSizeToDp(4));
        closeLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        closeLp.addRule(RelativeLayout.CENTER_VERTICAL);
        closeBtn.setLayoutParams(closeLp);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setText("✕");
        closeBtn.setTextSize(convertSizeToDp(10f));
        closeBtn.setTextColor(Color.parseColor(getThemeHex(0)));
        closeBtn.setShadowLayer(8f, 0, 0, Color.parseColor(getThemeHex(0)));
        closeBtn.setOnClickListener(v -> {
            mainLayout.setVisibility(View.GONE);
            iconLayout.setVisibility(View.VISIBLE);
        });
        headerLayout.addView(closeBtn);

        View.OnTouchListener dragListener = new View.OnTouchListener() {
            float dX, dY, pressX, pressY, newX, newY, maxX, maxY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = mainLayoutParams.x - event.getRawX();
                        dY = mainLayoutParams.y - event.getRawY();
                        pressX = event.getRawX();
                        pressY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        newX = event.getRawX() + dX;
                        newY = event.getRawY() + dY;
                        maxX = screenWidth  - mainLayout.getWidth();
                        maxY = screenHeight - mainLayout.getHeight();
                        if (newX < 0) newX = 0;
                        if (newX > maxX) newX = maxX;
                        if (newY < 0) newY = 0;
                        if (newY > maxY) newY = maxY;
                        mainLayoutParams.x = (int) newX;
                        mainLayoutParams.y = (int) newY;
                        windowManager.updateViewLayout(mainLayout, mainLayoutParams);
                        break;
                }
                return false;
            }
        };
        headerLayout.setOnTouchListener(dragListener);
        textTitle.setOnTouchListener(dragListener);

        headerLayout.addView(textTitle);
        mainLayout.addView(headerLayout);

        // ── Tab bar ───────────────────────────────────────────────────────────
        HorizontalScrollView tabScrollView = new HorizontalScrollView(this);
        tabScrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tabScrollView.setBackgroundColor(Color.parseColor("#CC050510"));

        LinearLayout tabLayout = new LinearLayout(this);
        tabLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabScrollView.addView(tabLayout);
        mainLayout.addView(tabScrollView);

        final RelativeLayout[] tabButtons = new RelativeLayout[listTab.length];

        for (int i = 0; i < listTab.length; i++) {
            pageLayouts[i] = new LinearLayout(this);
            pageLayouts[i].setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            pageLayouts[i].setOrientation(LinearLayout.VERTICAL);

            ScrollView scrollView = new ScrollView(this);
            scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            scrollView.addView(pageLayouts[i]);

            tabButtons[i] = new RelativeLayout(this);
            tabButtons[i].setLayoutParams(new RelativeLayout.LayoutParams(tabWidth, tabHeight));

            if (i != 0) {
                tabButtons[i].setBackgroundColor(Color.parseColor(getThemeHex(3)));
                pageLayouts[i].setVisibility(View.GONE);
                scrollView.setVisibility(View.GONE);
            } else {
                tabButtons[i].setBackgroundColor(Color.parseColor(getThemeHex(2)));
                pageLayouts[i].setVisibility(View.VISIBLE);
            }

            TextView tabText = new TextView(this);
            tabText.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            tabText.setGravity(Gravity.CENTER);
            tabText.setText(listTab[i]);
            tabText.setTextSize(convertSizeToDp(10.f));
            tabText.setTextColor(Color.parseColor(getThemeHex(0)));
            tabText.setShadowLayer(6f, 0, 0, Color.parseColor(getThemeHex(0)));
            tabButtons[i].addView(tabText);

            final int curTab = i;
            final ScrollView sv = scrollView;

            tabButtons[i].setOnClickListener(v -> {
                if (curTab != lastSelectedPage) {
                    tabButtons[lastSelectedPage].setBackgroundColor(Color.parseColor(getThemeHex(3)));
                    pageLayouts[lastSelectedPage].setVisibility(View.GONE);

                    tabButtons[curTab].setBackgroundColor(Color.parseColor(getThemeHex(2)));
                    pageLayouts[curTab].setVisibility(View.VISIBLE);

                    for (int j = 0; j < tabButtons.length; j++) {
                        if (j != curTab)
                            tabButtons[j].setBackgroundColor(Color.parseColor(getThemeHex(3)));
                    }
                    lastSelectedPage = curTab;
                }
            });

            tabLayout.addView(tabButtons[i]);
            mainLayout.addView(scrollView);
        }

        windowManager.addView(mainLayout, mainLayoutParams);
        AddFeatures();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  THEME HELPER
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * @param index 0=primary, 1=secondary, 2=activeTab bg, 3=inactiveTab bg
     */
    private String getThemeHex(int index) {
        return THEME_COLORS[currentTheme][index];
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FEATURES  (all six tabs)
    // ══════════════════════════════════════════════════════════════════════════

    void AddFeatures() {

        // ═══════════════════════════════════════════════════════════
        // TAB 0 — VISUAL / ESP
        //   Dump refs:
        //     BRPlayerPawn.get_Health  slot 42  RVA 0x5DD839C
        //     BRPlayerPawn.Health offset in native: via slot vtable
        //     onCanvasDraw handles rendering in native (nino.so)
        // ═══════════════════════════════════════════════════════════

        AddSectionHeader(0, "[ ESP ]");

        AddSwitch(0, "ESP Line", false, (btn, isChecked) ->
                UpdateConfiguration("ESP::LINE", isChecked ? 1 : 0));

        AddSwitch(0, "ESP Box", false, (btn, isChecked) ->
                UpdateConfiguration("ESP::BOX", isChecked ? 1 : 0));

        AddSwitch(0, "ESP Skeleton", false, (btn, isChecked) ->
                UpdateConfiguration("ESP::SKELETON", isChecked ? 1 : 0));

        AddSwitch(0, "ESP Health Bar", false, (btn, isChecked) ->
                UpdateConfiguration("ESP::HEALTH", isChecked ? 1 : 0));

        AddSwitch(0, "ESP Name", false, (btn, isChecked) ->
                UpdateConfiguration("ESP::NAME", isChecked ? 1 : 0));

        AddSwitch(0, "ESP Distance", false, (btn, isChecked) ->
                UpdateConfiguration("ESP::DISTANCE", isChecked ? 1 : 0));

        AddSwitch(0, "ESP Item Loot", false, (btn, isChecked) ->
                UpdateConfiguration("ESP::LOOT", isChecked ? 1 : 0));

        AddSwitch(0, "ESP Vehicle", false, (btn, isChecked) ->
                UpdateConfiguration("ESP::VEHICLE", isChecked ? 1 : 0));

        // Distance filter
        AddLabel(0, "Max ESP Distance (m)", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(0, "ESP Range", 50, 800, 300, "", "m", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("ESP::MAXDIST", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // ESP color channels (RGB sliders – native blends into its paint)
        AddSectionHeader(0, "[ ESP Color ]");
        AddLabel(0, "Enemy R", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(0, "ESP R", 0, 255, 255, "R:", "", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("ESP::COLOR_R", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        AddSeekbar(0, "ESP G", 0, 255, 0, "G:", "", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("ESP::COLOR_G", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        AddSeekbar(0, "ESP B", 0, 255, 0, "B:", "", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("ESP::COLOR_B", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Chams (wallhack render mode)
        AddSectionHeader(0, "[ Chams / Wallhack ]");
        AddLabel(0, "Chams Mode  (0=Off 1=Outline 2=Wireframe 3=Glow 4=Solid)", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(0, "Chams Mode", 0, 4, 0, "", "", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("CHAMS::MODE", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        AddSeekbar(0, "Chams Width", 1, 8, 2, "", "px", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("CHAMS::WIDTH", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Crosshair
        AddSectionHeader(0, "[ Crosshair ]");
        AddSwitch(0, "Custom Crosshair", false, (btn, isChecked) ->
                UpdateConfiguration("XHAIR::ENABLE", isChecked ? 1 : 0));
        AddLabel(0, "Style  (0=Cross 1=Dot 2=Circle 3=T-Shape)", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(0, "Crosshair Style", 0, 3, 0, "", "", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("XHAIR::STYLE", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        AddSeekbar(0, "Crosshair Size", 4, 40, 12, "", "px", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("XHAIR::SIZE", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // FOV visualiser circle
        AddSwitch(0, "FOV Circle", false, (btn, isChecked) ->
                UpdateConfiguration("FOV::CIRCLE", isChecked ? 1 : 0));

        // ═══════════════════════════════════════════════════════════
        // TAB 1 — WEAPON
        //   Dump refs:
        //     TrailRendererEffect.<init>   RVA 0x6D1AB30
        //     TrailRenderer.set_startColor RVA 0x4E8388C
        //     TrailRenderer.set_endColor   RVA 0x4E83950
        //     WeaponSkinHelper.DoChange    RVA 0xABC53FC
        //     WeaponSkinHelper.DoChange3P  RVA 0xABC5A54
        //     SetWeaponSkinResult          RVA 0x9384A4C
        //     C2SCSInventoryChgWeaponSkinReq RVA 0x6BC9D90
        //     Weapon.m_RecoilUpBase        offset 0x70C
        //     Weapon.m_RecoilLateralBase   offset 0x718
        //     Weapon.m_RecoilUpMax         offset 0x714
        //     Weapon.m_fireSpeed           offset 0x220
        //     WeaponIni.GetRecoilUpBase    RVA 0x96239E8
        //     WeaponIni.GetRecoilLateralBase RVA 0x9623AD0
        // ═══════════════════════════════════════════════════════════

        AddSectionHeader(1, "[ Bullet Tracer ]");

        AddSwitch(1, "Bullet Tracer", false, (btn, isChecked) ->
                UpdateConfiguration("TRACER::ENABLE", isChecked ? 1 : 0));

        AddLabel(1, "Tracer Color  (0=Cyan 1=Magenta 2=Red 3=Green 4=Rainbow)", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(1, "Tracer Color", 0, 4, 0, "", "", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("TRACER::COLOR", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        AddSeekbar(1, "Tracer Width", 1, 10, 2, "", "px", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("TRACER::WIDTH", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        AddSeekbar(1, "Tracer Duration", 1, 20, 5, "", "f", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("TRACER::DURATION", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Weapon Skin
        AddSectionHeader(1, "[ Weapon Skin ]");
        AddSwitch(1, "All Weapon Skin", false, (btn, isChecked) ->
                UpdateConfiguration("SKIN::ALLWEAPON", isChecked ? 1 : 0));
        AddSeekbar(1, "Skin ID", 0, 9999, 0, "ID:", "", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("SKIN::ID", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        // Unlock Camo skins (WeaponSkinHelper.ProcessCamoToSkin RVA 0xABC52E0)
        AddSwitch(1, "Unlock Camo Skins", false, (btn, isChecked) ->
                UpdateConfiguration("SKIN::CAMO", isChecked ? 1 : 0));
        // Force 3P skin visible on enemies (DoChange3P RVA 0xABC5A54)
        AddSwitch(1, "Force 3P Skin (Enemies)", false, (btn, isChecked) ->
                UpdateConfiguration("SKIN::FORCE3P", isChecked ? 1 : 0));

        // No Recoil
        // Weapon.m_RecoilUpBase @ offset 0x70C  |  m_RecoilLateralBase @ 0x718
        // GetRecoilUpBase RVA 0x96239E8  |  GetRecoilLateralBase RVA 0x9623AD0
        AddSectionHeader(1, "[ No Recoil ]");
        AddSwitch(1, "No Recoil", false, (btn, isChecked) ->
                UpdateConfiguration("WPN::NORECOIL", isChecked ? 1 : 0));
        AddLabel(1, "Recoil Intensity (0=None → 100=Full)", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(1, "Recoil Intensity", 0, 100, 0, "", "%", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("WPN::RECOIL_INT", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // No Spread
        AddSwitch(1, "No Spread", false, (btn, isChecked) ->
                UpdateConfiguration("WPN::NOSPREAD", isChecked ? 1 : 0));

        // Rapid Fire
        // Weapon.m_fireSpeed @ offset 0x220
        AddSectionHeader(1, "[ Rapid Fire ]");
        AddSwitch(1, "Rapid Fire", false, (btn, isChecked) ->
                UpdateConfiguration("WPN::RAPIDFIRE", isChecked ? 1 : 0));
        AddSeekbar(1, "Fire Rate Mult", 1, 10, 1, "x", "", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("WPN::FIRERATE", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Instant Reload
        AddSwitch(1, "Instant Reload", false, (btn, isChecked) ->
                UpdateConfiguration("WPN::INSTRELOAD", isChecked ? 1 : 0));

        // Infinite Ammo
        AddSwitch(1, "Infinite Ammo", false, (btn, isChecked) ->
                UpdateConfiguration("WPN::INFINITEAMMO", isChecked ? 1 : 0));

        // Bullet Speed
        // BulletMoveSpeed offset 0xC8 in A2DAnimAutoAiming
        AddLabel(1, "Bullet Speed Multiplier", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(1, "Bullet Speed", 100, 400, 100, "", "%", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("WPN::BULLETSPEED", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // ═══════════════════════════════════════════════════════════
        // TAB 2 — AIM
        //   Dump refs:
        //     BRPlayerPawn.AimMaxAngleXDown offset 0x2798
        //     BRPlayerPawn.AimMaxAngleXUp   offset 0x279C
        // ═══════════════════════════════════════════════════════════

        AddSectionHeader(2, "[ Aim Bot ]");

        AddSwitch(2, "Aim Bot", false, (btn, isChecked) ->
                UpdateConfiguration("AIM::AIMBOT", isChecked ? 1 : 0));

        AddLabel(2, "Aim Location", 5f, Color.parseColor("#AAFFFFFF"));
        AddRadioButton(2, new String[]{"Head", "Chest", "Body", "Legs"}, 0,
                (rg, id) -> UpdateConfiguration("AIM::LOCATION", id));

        AddLabel(2, "Target Priority", 5f, Color.parseColor("#AAFFFFFF"));
        AddRadioButton(2, new String[]{"Closest Dist", "Inside POV", "Lowest HP", "Crosshair"}, 0,
                (rg, id) -> UpdateConfiguration("AIM::TARGET", id));

        AddSeekbar(2, "FOV Size", 0, 600, 0, "", "", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("AIM::SIZE", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        AddLabel(2, "Trigger Mode", 5f, Color.parseColor("#AAFFFFFF"));
        AddRadioButton(2, new String[]{"None", "Shooting", "Scoping", "Always"}, 0,
                (rg, id) -> UpdateConfiguration("AIM::TRIGGER", id));

        // Aim Smooth
        AddSeekbar(2, "Aim Smooth", 1, 20, 1, "", "", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("AIM::SMOOTH", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Silent Aim
        AddSectionHeader(2, "[ Silent Aim ]");
        AddSwitch(2, "Silent Aim", false, (btn, isChecked) ->
                UpdateConfiguration("AIM::SILENT", isChecked ? 1 : 0));
        AddSeekbar(2, "Silent Angle", 1, 90, 15, "", "°", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("AIM::SILENT_ANGLE", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Magic Bullet
        AddSectionHeader(2, "[ Magic Bullet ]");
        AddSwitch(2, "Magic Bullet", false, (btn, isChecked) ->
                UpdateConfiguration("AIM::MAGIC", isChecked ? 1 : 0));
        AddSeekbar(2, "Magic Radius", 1, 50, 10, "", "px", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("AIM::MAGIC_R", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Aim extras
        AddSectionHeader(2, "[ Aim Options ]");
        AddSwitch(2, "Prediction", false, (btn, isChecked) ->
                UpdateConfiguration("AIM::PREDICT", isChecked ? 1 : 0));
        AddSwitch(2, "Visibility Check", true, (btn, isChecked) ->
                UpdateConfiguration("AIM::VISCHECK", isChecked ? 1 : 0));
        AddSwitch(2, "Scope-Only Aim", false, (btn, isChecked) ->
                UpdateConfiguration("AIM::SCOPEONLY", isChecked ? 1 : 0));
        AddSwitch(2, "Auto Shoot", false, (btn, isChecked) ->
                UpdateConfiguration("AIM::AUTOSHOOT", isChecked ? 1 : 0));
        AddSwitch(2, "Aimbot 360°", false, (btn, isChecked) ->
                UpdateConfiguration("AIM::360", isChecked ? 1 : 0));
        AddSwitch(2, "Auto Crouch Aim", false, (btn, isChecked) ->
                UpdateConfiguration("AIM::AUTOCROUCH", isChecked ? 1 : 0));

        // ═══════════════════════════════════════════════════════════
        // TAB 3 — MOVEMENT
        //   Dump refs:
        //     Pawn.set_MaxMoveSpeed  RVA 0x4F959D4  slot 155
        //     Pawn.DoJump            RVA 0x5008528  slot 773
        //     Pawn.DoFalling         RVA 0x4FFFEA4
        //     Pawn.GetGravityScale   RVA 0xBD166A0  slot 20
        //     fCoeffPlayerJumpHeight offset 0x58 (PawnData)
        //     MaxJumpHeightScale     offset 0x28
        // ═══════════════════════════════════════════════════════════

        AddSectionHeader(3, "[ Speed ]");

        AddSwitch(3, "Speed Hack", false, (btn, isChecked) ->
                UpdateConfiguration("MOV::SPEEDHACK", isChecked ? 1 : 0));
        // Value is a percentage of base run speed; 100=default, 200=double
        AddLabel(3, "Speed Multiplier (%)  [Pawn.set_MaxMoveSpeed @ 0x4F959D4]", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(3, "Speed %", 100, 400, 100, "", "%", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("MOV::SPEEDMUL", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        AddSwitch(3, "Speed Burst (Sprint Always)", false, (btn, isChecked) ->
                UpdateConfiguration("MOV::SPRINTLOCK", isChecked ? 1 : 0));

        // Jump
        AddSectionHeader(3, "[ Jump ]");
        AddSwitch(3, "High Jump", false, (btn, isChecked) ->
                UpdateConfiguration("MOV::HIGHJUMP", isChecked ? 1 : 0));
        // fCoeffPlayerJumpHeight offset 0x58
        AddSeekbar(3, "Jump Height", 100, 500, 100, "", "%", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("MOV::JUMPHEIGHT", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        AddSwitch(3, "Infinite Jump", false, (btn, isChecked) ->
                UpdateConfiguration("MOV::INFJUMP", isChecked ? 1 : 0));

        // Gravity / Fly
        AddSectionHeader(3, "[ Gravity & Fly ]");
        AddSwitch(3, "Fly Mode", false, (btn, isChecked) ->
                UpdateConfiguration("MOV::FLY", isChecked ? 1 : 0));
        // GetGravityScale RVA 0xBD166A0; native clamps to [0.01 .. 2.0]
        AddLabel(3, "Gravity Scale  (10=10% → 100=normal → 200=heavy)", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(3, "Gravity %", 5, 200, 100, "", "%", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("MOV::GRAVITY", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        AddSwitch(3, "Anti-Fall Damage", false, (btn, isChecked) ->
                UpdateConfiguration("MOV::NOFALL", isChecked ? 1 : 0));
        // DoFalling RVA 0x4FFFEA4 – forces horizontal/vertical velocity to 0
        AddSwitch(3, "Glide Mode (slow fall)", false, (btn, isChecked) ->
                UpdateConfiguration("MOV::GLIDE", isChecked ? 1 : 0));

        // Vehicle
        AddSectionHeader(3, "[ Vehicle ]");
        AddSwitch(3, "Vehicle Speed Boost", false, (btn, isChecked) ->
                UpdateConfiguration("MOV::VEHSPEED", isChecked ? 1 : 0));
        AddSeekbar(3, "Vehicle Speed %", 100, 500, 100, "", "%", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("MOV::VEHSPEEDMUL", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        AddSwitch(3, "No Vehicle Damage", false, (btn, isChecked) ->
                UpdateConfiguration("MOV::NOVEHDAM", isChecked ? 1 : 0));

        // ═══════════════════════════════════════════════════════════
        // TAB 4 — ANTI-DETECT / STEALTH
        //   Dump refs:
        //     BRBeHackedDataManager (BRPlayerPawn.m_BeHackedDataMgr @ offset 0x27B8)
        //     LocalPlayer.MoveState  offset 0xAC
        //     EPlayerState_InInvisible const 64
        //     LocalPlayerMoveChecker class (anti-cheat move validator)
        // ═══════════════════════════════════════════════════════════

        AddSectionHeader(4, "[ Anti-Detect ]");

        // BRBeHackedDataMgr is the server-side hack detector; zero-ing its fields
        // prevents flagging. Toggling this activates the native bypass patch.
        AddSwitch(4, "Anti-Ban (Server Bypass)", false, (btn, isChecked) ->
                UpdateConfiguration("STEALTH::ANTIBAN", isChecked ? 1 : 0));

        AddSwitch(4, "Anti-Screenshot", false, (btn, isChecked) ->
                UpdateConfiguration("STEALTH::ANTISS", isChecked ? 1 : 0));

        AddSwitch(4, "Spoof Move State", false, (btn, isChecked) ->
                UpdateConfiguration("STEALTH::MOVESPOOF", isChecked ? 1 : 0));

        // LocalPlayerMoveChecker patches the movement validator so speed deltas
        // don't trigger server kick
        AddSwitch(4, "Bypass Move Checker", false, (btn, isChecked) ->
                UpdateConfiguration("STEALTH::MOVECHECKER", isChecked ? 1 : 0));

        // Fake lag / packet delay to mask timing anomalies
        AddSectionHeader(4, "[ Fake Lag ]");
        AddSwitch(4, "Fake Lag", false, (btn, isChecked) ->
                UpdateConfiguration("STEALTH::FAKELAG", isChecked ? 1 : 0));
        AddLabel(4, "Lag Amount (ms)", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(4, "Lag ms", 0, 200, 50, "", "ms", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                UpdateConfiguration("STEALTH::LAGAMT", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Cheat report suppression
        AddSectionHeader(4, "[ Stealth Misc ]");
        AddSwitch(4, "No Grass / Foliage", false, (btn, isChecked) ->
                UpdateConfiguration("STEALTH::NOGRASS", isChecked ? 1 : 0));
        AddSwitch(4, "Remove Weather FX", false, (btn, isChecked) ->
                UpdateConfiguration("STEALTH::NOWEATHER", isChecked ? 1 : 0));
        AddSwitch(4, "No Aim Shake (Cam Steady)", false, (btn, isChecked) ->
                UpdateConfiguration("STEALTH::NOAIMSHAKE", isChecked ? 1 : 0));
        // BRBeHackedDataMgr bool fields zero-patch
        AddSwitch(4, "Zero Hack Telemetry", false, (btn, isChecked) ->
                UpdateConfiguration("STEALTH::ZEROHACK", isChecked ? 1 : 0));

        // ═══════════════════════════════════════════════════════════
        // TAB 5 — CONFIG / SETTINGS
        // ═══════════════════════════════════════════════════════════

        AddSectionHeader(5, "[ Presets ]");
        AddLabel(5, "Load preset configuration", 5f, Color.parseColor("#AAFFFFFF"));
        AddRadioButton(5, new String[]{"Aggressive", "Stealth", "Casual", "Custom"}, currentPreset,
                (rg, id) -> ApplyPreset(id));

        AddSectionHeader(5, "[ Config ]");
        // Reset button
        AddButton(5, "RESET ALL CONFIG", () -> ResetAllConfiguration());

        AddSectionHeader(5, "[ Theme ]");
        AddLabel(5, "Menu Color Theme", 5f, Color.parseColor("#AAFFFFFF"));
        AddRadioButton(5, new String[]{"Cyan", "Magenta", "Red", "Green", "Gold", "Purple"}, currentTheme,
                (rg, id) -> {
                    currentTheme = id;
                    configPrefs.edit().putInt("META::THEME", id).apply();
                    Toast.makeText(Floating.this,
                            "Restart menu to apply theme", Toast.LENGTH_SHORT).show();
                });

        AddSectionHeader(5, "[ Display ]");
        AddSeekbar(5, "Menu Opacity", 1, 100, 100, "", "%", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                mainLayout.setAlpha((float) p / 100f);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        AddSeekbar(5, "Icon Size", 50, 200, 100, "", "%", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                ViewGroup.LayoutParams lp = iconImg.getLayoutParams();
                lp.width  = (int)(iconSize * (p / 100f));
                lp.height = (int)(iconSize * (p / 100f));
                iconImg.setLayoutParams(lp);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { iconLayout.setVisibility(View.VISIBLE); }
            @Override public void onStopTrackingTouch(SeekBar s)  { iconLayout.setVisibility(View.GONE); }
        });
        AddSeekbar(5, "Icon Opacity", 0, 100, 100, "", "%", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                iconLayout.setAlpha((float) p / 100f);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { iconLayout.setVisibility(View.VISIBLE); }
            @Override public void onStopTrackingTouch(SeekBar s)  { iconLayout.setVisibility(View.GONE); }
        });

        // Target FPS for canvas redraw
        AddLabel(5, "Canvas FPS (default=60)", 5f, Color.parseColor("#AAFFFFFF"));
        AddSeekbar(5, "Canvas FPS", 15, 120, 60, "", "fps", new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                sleepTime = Math.max(1, 4000 / p);
                UpdateConfiguration("META::FPS", p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        AddSwitch(5, "Title Randomiser", true, (btn, isChecked) ->
                UpdateConfiguration("META::RANDTITLE", isChecked ? 1 : 0));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI HELPERS  (extended set)
    // ══════════════════════════════════════════════════════════════════════════

    /** Bold section divider with glowing accent colour. */
    void AddSectionHeader(int tab, String text) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setPadding(15, 10, 15, 4);
        tv.setText(text);
        tv.setTextSize(convertSizeToDp(10f));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor(getThemeHex(0)));
        tv.setShadowLayer(6f, 0, 0, Color.parseColor(getThemeHex(0)));
        pageLayouts[tab].addView(tv);
    }

    /** Small caption label. */
    void AddLabel(int tab, String text, float size, int color) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setPadding(15, 4, 15, 2);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        pageLayouts[tab].addView(tv);
    }

    /** Original AddText kept for compat; delegates to AddSectionHeader-style. */
    void AddText(Object data, String text, int size, int style, String colorHex) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setPadding(15, 8, 15, 2);
        tv.setText(text);
        tv.setTextSize(convertSizeToDp(size));
        tv.setTypeface(null, style);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setShadowLayer(4f, 0, 0, Color.parseColor(colorHex));

        if (data instanceof Integer)        pageLayouts[(Integer) data].addView(tv);
        else if (data instanceof ViewGroup) ((ViewGroup) data).addView(tv);
    }

    /** Original AddText2 kept for compat. */
    void AddText2(Object data, String text, float size, int color) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setPadding(15, 4, 15, 2);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);

        if (data instanceof Integer)        pageLayouts[(Integer) data].addView(tv);
        else if (data instanceof ViewGroup) ((ViewGroup) data).addView(tv);
    }

    void AddSwitch(Object data, String label, boolean defaultOn,
                   CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(15, 5, 15, 5);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tv.setText(label);
        tv.setTextSize(convertSizeToDp(9f));
        tv.setTextColor(Color.parseColor("#DDFFFFFF"));

        Switch sw = new Switch(this);
        sw.setChecked(defaultOn);
        sw.setThumbTintList(ColorStateList.valueOf(Color.parseColor(getThemeHex(0))));
        sw.setTrackTintList(ColorStateList.valueOf(Color.parseColor(getThemeHex(2))));
        sw.setOnCheckedChangeListener(listener);

        row.addView(tv);
        row.addView(sw);

        if (data instanceof Integer)        pageLayouts[(Integer) data].addView(row);
        else if (data instanceof ViewGroup) ((ViewGroup) data).addView(row);
    }

    void AddSeekbar(Object data, String label, int min, int max, int defaultVal,
                    String prefix, String suffix,
                    SeekBar.OnSeekBarChangeListener listener) {
        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        labelRow.setPadding(15, 4, 15, 0);

        TextView labelTv = new TextView(this);
        labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        labelTv.setText(label);
        labelTv.setTextSize(convertSizeToDp(8f));
        labelTv.setTextColor(Color.parseColor("#DDFFFFFF"));

        TextView valueTv = new TextView(this);
        valueTv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        valueTv.setTextSize(convertSizeToDp(8f));
        valueTv.setTextColor(Color.parseColor(getThemeHex(0)));

        labelRow.addView(labelTv);
        labelRow.addView(valueTv);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        seekBar.setPadding(15, 0, 15, 5);
        seekBar.setMax(max - min);
        seekBar.setProgress(defaultVal - min);
        seekBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor(getThemeHex(0))));
        seekBar.setThumbTintList(ColorStateList.valueOf(Color.parseColor(getThemeHex(1))));

        valueTv.setText(prefix + defaultVal + suffix);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int value = progress + min;
                valueTv.setText(prefix + value + suffix);
                listener.onProgressChanged(s, value, fromUser);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { listener.onStartTrackingTouch(s); }
            @Override public void onStopTrackingTouch(SeekBar s)  { listener.onStopTrackingTouch(s); }
        });

        if (data instanceof Integer) {
            pageLayouts[(Integer) data].addView(labelRow);
            pageLayouts[(Integer) data].addView(seekBar);
        } else if (data instanceof ViewGroup) {
            ((ViewGroup) data).addView(labelRow);
            ((ViewGroup) data).addView(seekBar);
        }
    }

    void AddRadioButton(Object data, String[] list, int defaultCheckedId,
                        RadioGroup.OnCheckedChangeListener listener) {
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.HORIZONTAL);
        rg.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rg.setPadding(15, 4, 15, 4);

        for (int i = 0; i < list.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setPadding(8, 8, 8, 8);
            rb.setText(list[i]);
            rb.setTextSize(convertSizeToDp(7f));
            rb.setId(i);
            rb.setTextColor(Color.parseColor("#DDFFFFFF"));
            rb.setButtonTintList(ColorStateList.valueOf(Color.parseColor(getThemeHex(0))));
            if (i == defaultCheckedId) rb.setChecked(true);
            rg.addView(rb);
        }
        rg.setOnCheckedChangeListener(listener);

        if (data instanceof Integer)        pageLayouts[(Integer) data].addView(rg);
        else if (data instanceof ViewGroup) ((ViewGroup) data).addView(rg);
    }

    /** Action button – executes a Runnable on tap. */
    void AddButton(int tab, String label, Runnable action) {
        Button btn = new Button(this);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btn.setPadding(15, 8, 15, 8);
        btn.setText(label);
        btn.setTextSize(convertSizeToDp(8f));
        btn.setTextColor(Color.parseColor(getThemeHex(0)));
        btn.setShadowLayer(4f, 0, 0, Color.parseColor(getThemeHex(0)));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#22" + getThemeHex(0).substring(1)));
        btnBg.setStroke(1, Color.parseColor(getThemeHex(0)));
        btnBg.setCornerRadius(8f);
        btn.setBackground(btnBg);

        btn.setOnClickListener(v -> action.run());
        pageLayouts[tab].addView(btn);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UTIL
    // ══════════════════════════════════════════════════════════════════════════

    float convertSizeToDp(float size) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                size, getResources().getDisplayMetrics());
    }

    int convertSizeToDp(int size) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                size, getResources().getDisplayMetrics()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  THREADS & HANDLER
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            // MSG 0 – screen-size change
            if (msg.what == 0) {
                try {
                    Point sz = new Point();
                    windowManager.getDefaultDisplay().getRealSize(sz);
                    screenWidth  = sz.x;
                    screenHeight = sz.y;
                } catch (Exception ex) { ex.printStackTrace(); }
            }
            // MSG 1 – title animation tick
            if (msg.what == 1) {
                try {
                    Random r = new Random();
                    String myTitle = "GAERIS";
                    int idx = r.nextInt(myTitle.length());
                    int c   = r.nextInt(M_RAND_TITLE.length());
                    char[] arr = myTitle.toCharArray();
                    arr[idx] = M_RAND_TITLE.charAt(c);
                    textTitle.setText(String.valueOf(arr));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    };

    Thread mUpdateCanvas = new Thread() {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
            while (isAlive() && !isInterrupted()) {
                try {
                    long t1 = System.currentTimeMillis();
                    canvasLayout.postInvalidate();
                    long td = System.currentTimeMillis() - t1;
                    Thread.sleep(Math.max(0, sleepTime - td));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    };

    Thread mUpdateThread = new Thread() {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
            while (isAlive() && !isInterrupted()) {
                try {
                    long t1 = System.currentTimeMillis();
                    Point sz = new Point();
                    windowManager.getDefaultDisplay().getRealSize(sz);
                    if (screenWidth != sz.x || screenHeight != sz.y)
                        handler.sendEmptyMessage(0);
                    handler.sendEmptyMessage(1);
                    long td = System.currentTimeMillis() - t1;
                    Thread.sleep(Math.max(0, sleepTime - td));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    };
}
