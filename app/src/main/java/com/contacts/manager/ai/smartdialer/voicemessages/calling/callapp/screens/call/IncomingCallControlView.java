package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.screens.call;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;
import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.utils.HapticUtils;

import java.util.ArrayList;
import java.util.List;

public class IncomingCallControlView extends FrameLayout {

    public interface Listener {
        void onAnswer();

        void onDecline();
    }

    public static final int STYLE_COUNT = 10;
    public static final int STYLE_CLASSIC = 1;
    public static final int STYLE_VERTICAL = 2;
    public static final int STYLE_CAPSULE = 3;
    public static final int STYLE_PULSE = 4;
    public static final int STYLE_WAVE = 5;
    public static final int STYLE_REVERSED = 6;
    public static final int STYLE_OUTLINE = 7;
    public static final int STYLE_PILL_BAR = 8;
    public static final int STYLE_GLASS = 9;
    public static final int STYLE_NEON = 10;

    private static final int NEON = 0xFF22D3EE;

    private final List<Animator> animators = new ArrayList<>();
    private Listener listener;
    private int style = STYLE_CLASSIC;
    private boolean answerLeft;
    private boolean onDark;
    private boolean interactive = true;
    private boolean fired;
    private final int answerColor;
    private final int declineColor;

    public IncomingCallControlView(Context context) {
        this(context, null);
    }

    public IncomingCallControlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        setClipToPadding(false);
        answerColor = ContextCompat.getColor(context, R.color.call_answer);
        declineColor = ContextCompat.getColor(context, R.color.call_decline);
        rebuild();
    }

    public static int styleName(int style) {
        switch (style) {
            case STYLE_VERTICAL:
                return R.string.call_style_2;
            case STYLE_CAPSULE:
                return R.string.call_style_3;
            case STYLE_PULSE:
                return R.string.call_style_4;
            case STYLE_WAVE:
                return R.string.call_style_5;
            case STYLE_REVERSED:
                return R.string.call_style_6;
            case STYLE_OUTLINE:
                return R.string.call_style_7;
            case STYLE_PILL_BAR:
                return R.string.call_style_8;
            case STYLE_GLASS:
                return R.string.call_style_9;
            case STYLE_NEON:
                return R.string.call_style_10;
            default:
                return R.string.call_style_1;
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void configure(int style, boolean answerLeft, boolean onDark) {
        if (this.style == style && this.answerLeft == answerLeft && this.onDark == onDark && getChildCount() > 0) {
            return;
        }
        this.style = style < 1 || style > STYLE_COUNT ? STYLE_CLASSIC : style;
        this.answerLeft = answerLeft;
        this.onDark = onDark;
        rebuild();
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public void reset() {
        fired = false;
        rebuild();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimations();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimations();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (!isAttachedToWindow()) {
            return;
        }
        if (visibility == VISIBLE && isShown()) {
            startAnimations();
        } else {
            stopAnimations();
        }
    }

    private void rebuild() {
        stopAnimations();
        animators.clear();
        removeAllViews();
        switch (style) {
            case STYLE_VERTICAL:
                buildVertical();
                break;
            case STYLE_CAPSULE:
                buildCapsule();
                break;
            case STYLE_PULSE:
                buildDual(false, true, false);
                break;
            case STYLE_WAVE:
                buildWave();
                break;
            case STYLE_REVERSED:
                buildDual(true, false, false);
                break;
            case STYLE_OUTLINE:
                buildDual(false, false, true);
                break;
            case STYLE_PILL_BAR:
                buildPillBar();
                break;
            case STYLE_GLASS:
                buildGlass();
                break;
            case STYLE_NEON:
                buildNeon();
                break;
            default:
                buildDual(false, false, false);
                break;
        }
        if (isAttachedToWindow() && isShown()) {
            startAnimations();
        }
    }

    private void startAnimations() {
        for (Animator animator : animators) {
            if (!animator.isStarted()) {
                animator.start();
            }
        }
    }

    private void stopAnimations() {
        for (Animator animator : animators) {
            animator.cancel();
        }
    }

    private void fire(boolean answer) {
        if (!interactive || fired || listener == null) {
            return;
        }
        fired = true;
        HapticUtils.confirm(this);
        if (answer) {
            listener.onAnswer();
        } else {
            listener.onDecline();
        }
        postDelayed(() -> fired = false, 1500);
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    private int labelColor() {
        return onDark ? Color.WHITE : ContextCompat.getColor(getContext(), R.color.text_primary);
    }

    private int surfaceColor(int lightAlpha) {
        return onDark ? ColorUtils.setAlphaComponent(Color.WHITE, lightAlpha)
                : ContextCompat.getColor(getContext(), R.color.card_strong);
    }

    private GradientDrawable oval(int fill, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fill);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private GradientDrawable rounded(int fill, float radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private ImageView circle(boolean answer, int sizeDp, boolean outline) {
        ImageView view = new ImageView(getContext());
        int color = answer ? answerColor : declineColor;
        view.setLayoutParams(new LayoutParams(dp(sizeDp), dp(sizeDp)));
        if (outline) {
            view.setBackground(oval(ColorUtils.setAlphaComponent(color, 0x1F), dp(2), color));
        } else {
            view.setBackground(oval(color, 0, 0));
            view.setElevation(dp(6));
        }
        view.setImageResource(answer ? R.drawable.ic_call_filled : R.drawable.ic_call_end_filled);
        view.setImageTintList(ColorStateList.valueOf(outline ? color : Color.WHITE));
        view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int padding = Math.round(dp(sizeDp) * 0.27f);
        view.setPadding(padding, padding, padding, padding);
        view.setContentDescription(getContext().getString(answer ? R.string.answer : R.string.decline));
        view.setOnClickListener(v -> fire(answer));
        return view;
    }

    private TextView label(CharSequence text, int color, float sizeSp, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.create("sans-serif-medium", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setMaxLines(1);
        return view;
    }

    private LinearLayout column(View top, TextView bottom) {
        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setClipChildren(false);
        column.setClipToPadding(false);
        column.addView(top);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        column.addView(bottom, params);
        return column;
    }

    private LinearLayout row(int paddingDp) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(paddingDp), dp(12), dp(paddingDp), dp(12));
        row.setClipChildren(false);
        row.setClipToPadding(false);
        return row;
    }

    private void addSides(LinearLayout row, View answer, View decline, boolean reversed) {
        boolean answerFirst = answerLeft ^ reversed;
        row.addView(answerFirst ? answer : decline);
        View spacer = new View(getContext());
        row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        row.addView(answerFirst ? decline : answer);
    }

    private void addCentered(View view) {
        addView(view, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
    }

    private View pulsing(ImageView circle, int color) {
        FrameLayout frame = new FrameLayout(getContext());
        frame.setClipChildren(false);
        int size = dp(72);
        for (int i = 0; i < 2; i++) {
            View ring = new View(getContext());
            ring.setBackground(oval(ColorUtils.setAlphaComponent(color, 0x55), 0, 0));
            frame.addView(ring, new LayoutParams(size, size, Gravity.CENTER));
            ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(ring,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.75f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.75f),
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0.9f, 0f));
            animator.setDuration(1600);
            animator.setStartDelay(i * 800L);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animators.add(animator);
        }
        frame.addView(circle, new LayoutParams(size, size, Gravity.CENTER));
        return frame;
    }

    private void buildDual(boolean reversed, boolean pulse, boolean outline) {
        LinearLayout row = row(40);
        ImageView answerCircle = circle(true, 72, outline);
        ImageView declineCircle = circle(false, 72, outline);
        View answerTop = pulse ? pulsing(answerCircle, answerColor) : answerCircle;
        View declineTop = pulse ? pulsing(declineCircle, declineColor) : declineCircle;
        int color = labelColor();
        View answer = column(answerTop, label(getContext().getString(R.string.answer), color, 14, false));
        View decline = column(declineTop, label(getContext().getString(R.string.decline), color, 14, false));
        addSides(row, answer, decline, reversed);
        if (!pulse) {
            ObjectAnimator bounce = ObjectAnimator.ofPropertyValuesHolder(answerCircle,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.08f, 1f));
            bounce.setDuration(1200);
            bounce.setRepeatCount(ValueAnimator.INFINITE);
            animators.add(bounce);
        }
        addCentered(row);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void buildVertical() {
        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setClipChildren(false);
        column.setPadding(0, dp(4), 0, dp(4));
        int color = labelColor();

        ImageView arrow = new ImageView(getContext());
        arrow.setImageResource(R.drawable.ic_arrow_up);
        arrow.setImageTintList(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 0xB0)));
        column.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(28)));
        column.addView(label(getContext().getString(R.string.swipe_up_answer), color, 14, true));

        ImageView knob = circle(true, 76, false);
        LinearLayout.LayoutParams knobParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        knobParams.topMargin = dp(18);
        knobParams.bottomMargin = dp(18);
        column.addView(knob, knobParams);

        column.addView(label(getContext().getString(R.string.swipe_down_decline), color, 14, true));
        ImageView down = new ImageView(getContext());
        down.setImageResource(R.drawable.ic_arrow_up);
        down.setRotation(180f);
        down.setImageTintList(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 0xB0)));
        column.addView(down, new LinearLayout.LayoutParams(dp(28), dp(28)));
        addCentered(column);

        ObjectAnimator hint = ObjectAnimator.ofFloat(knob, View.TRANSLATION_Y, 0f, -dp(10), 0f);
        hint.setDuration(1400);
        hint.setRepeatCount(ValueAnimator.INFINITE);
        hint.setInterpolator(new AccelerateDecelerateInterpolator());
        animators.add(hint);

        float limit = dp(72);
        knob.setOnClickListener(null);
        knob.setOnTouchListener(new OnTouchListener() {
            float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!interactive) {
                    return false;
                }
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        hint.cancel();
                        startY = event.getRawY() - v.getTranslationY();
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dy = Math.max(-limit, Math.min(limit, event.getRawY() - startY));
                        v.setTranslationY(dy);
                        boolean declining = dy > 0;
                        v.setBackground(oval(declining ? declineColor : answerColor, 0, 0));
                        ((ImageView) v).setImageResource(declining ? R.drawable.ic_call_end_filled : R.drawable.ic_call_filled);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float offset = v.getTranslationY();
                        if (offset <= -limit * 0.8f) {
                            fire(true);
                        } else if (offset >= limit * 0.8f) {
                            fire(false);
                        }
                        v.animate().translationY(0f).setDuration(220).setInterpolator(new OvershootInterpolator())
                                .withEndAction(() -> {
                                    v.setBackground(oval(answerColor, 0, 0));
                                    ((ImageView) v).setImageResource(R.drawable.ic_call_filled);
                                    if (isShown()) {
                                        hint.start();
                                    }
                                }).start();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private FrameLayout track(int heightDp, android.graphics.drawable.Drawable background) {
        FrameLayout track = new FrameLayout(getContext());
        track.setBackground(background);
        track.setClipChildren(false);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp));
        params.leftMargin = dp(28);
        params.rightMargin = dp(28);
        track.setLayoutParams(params);
        return track;
    }

    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener attachSlider(FrameLayout track, View knob, boolean twoWay, @Nullable View hintView) {
        knob.setOnClickListener(null);
        int direction = answerLeft ? -1 : 1;
        View.OnTouchListener listener = new OnTouchListener() {
            float startX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!interactive) {
                    return false;
                }
                float max = twoWay ? (track.getWidth() - v.getWidth()) / 2f - dp(6)
                        : track.getWidth() - v.getWidth() - dp(12);
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX() - v.getTranslationX();
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startX;
                        float min = twoWay ? -max : (direction > 0 ? 0 : -max);
                        float upper = twoWay ? max : (direction > 0 ? max : 0);
                        dx = Math.max(min, Math.min(upper, dx));
                        v.setTranslationX(dx);
                        if (hintView != null) {
                            hintView.setAlpha(1f - Math.min(1f, Math.abs(dx) / Math.max(1f, max)));
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float progress = max <= 0 ? 0 : v.getTranslationX() / max * direction;
                        if (progress >= 0.75f) {
                            fire(true);
                        } else if (twoWay && progress <= -0.75f) {
                            fire(false);
                        }
                        v.animate().translationX(0f).setDuration(240).setInterpolator(new OvershootInterpolator()).start();
                        if (hintView != null) {
                            hintView.animate().alpha(1f).setDuration(240).start();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        };
        knob.setOnTouchListener(listener);
        return listener;
    }

    private void buildCapsule() {
        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setClipChildren(false);

        TextView decline = label(getContext().getString(R.string.decline_call), declineColor, 16, true);
        decline.setPadding(dp(20), dp(10), dp(20), dp(10));
        decline.setBackground(rounded(ColorUtils.setAlphaComponent(declineColor, 0x22), dp(22), 0, 0));
        decline.setOnClickListener(v -> fire(false));
        column.addView(decline);

        FrameLayout track = track(72, rounded(surfaceColor(0x2E), dp(36), onDark ? dp(1) : 0,
                ColorUtils.setAlphaComponent(Color.WHITE, 0x40)));
        TextView hint = label(getContext().getString(R.string.slide_to_answer) + (answerLeft ? "  «" : "  »"),
                ColorUtils.setAlphaComponent(labelColor(), 0xCC), 16, true);
        track.addView(hint, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ImageView knob = circle(true, 60, false);
        LayoutParams knobParams = new LayoutParams(dp(60), dp(60),
                Gravity.CENTER_VERTICAL | (answerLeft ? Gravity.END : Gravity.START));
        knobParams.leftMargin = dp(6);
        knobParams.rightMargin = dp(6);
        track.addView(knob, knobParams);
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(72));
        trackParams.topMargin = dp(20);
        trackParams.leftMargin = dp(28);
        trackParams.rightMargin = dp(28);
        column.addView(track, trackParams);
        addCentered(column);

        ObjectAnimator shimmer = ObjectAnimator.ofFloat(hint, View.ALPHA, 1f, 0.35f, 1f);
        shimmer.setDuration(1800);
        shimmer.setRepeatCount(ValueAnimator.INFINITE);
        animators.add(shimmer);
        attachSlider(track, knob, false, null);
    }

    private void buildWave() {
        int start = answerLeft ? answerColor : declineColor;
        int end = answerLeft ? declineColor : answerColor;
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{
                ColorUtils.setAlphaComponent(start, 0x88), surfaceColor(0x22), ColorUtils.setAlphaComponent(end, 0x88)});
        background.setCornerRadius(dp(40));
        FrameLayout track = track(80, background);

        int color = labelColor();
        TextView left = label(answerLeft ? "« " + getContext().getString(R.string.answer)
                : "« " + getContext().getString(R.string.decline), color, 15, true);
        TextView right = label(answerLeft ? getContext().getString(R.string.decline) + " »"
                : getContext().getString(R.string.answer) + " »", color, 15, true);
        LayoutParams leftParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.START);
        leftParams.leftMargin = dp(22);
        LayoutParams rightParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.END);
        rightParams.rightMargin = dp(22);
        track.addView(left, leftParams);
        track.addView(right, rightParams);

        ImageView knob = new ImageView(getContext());
        knob.setBackground(oval(Color.WHITE, 0, 0));
        knob.setElevation(dp(6));
        knob.setImageResource(R.drawable.ic_call_filled);
        knob.setImageTintList(ColorStateList.valueOf(answerColor));
        knob.setPadding(dp(18), dp(18), dp(18), dp(18));
        knob.setContentDescription(getContext().getString(R.string.answer));
        track.addView(knob, new LayoutParams(dp(64), dp(64), Gravity.CENTER));
        addCentered(track);

        ObjectAnimator wiggle = ObjectAnimator.ofFloat(knob, View.TRANSLATION_X, 0f, dp(8), 0f, -dp(8), 0f);
        wiggle.setDuration(1800);
        wiggle.setRepeatCount(ValueAnimator.INFINITE);
        animators.add(wiggle);
        View.OnTouchListener slider = attachSlider(track, knob, true, null);
        knob.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                wiggle.cancel();
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                v.postDelayed(() -> {
                    if (isShown() && !wiggle.isStarted()) {
                        wiggle.start();
                    }
                }, 400);
            }
            return slider.onTouch(v, event);
        });
    }

    private void buildPillBar() {
        FrameLayout bar = new FrameLayout(getContext());
        bar.setBackground(rounded(surfaceColor(0x26), dp(44), onDark ? dp(1) : 0,
                ColorUtils.setAlphaComponent(Color.WHITE, 0x33)));
        bar.setClipChildren(false);
        ImageView answer = circle(true, 64, false);
        ImageView decline = circle(false, 64, false);
        LayoutParams startParams = new LayoutParams(dp(64), dp(64), Gravity.CENTER_VERTICAL | Gravity.START);
        startParams.leftMargin = dp(12);
        LayoutParams endParams = new LayoutParams(dp(64), dp(64), Gravity.CENTER_VERTICAL | Gravity.END);
        endParams.rightMargin = dp(12);
        bar.addView(answerLeft ? answer : decline, startParams);
        bar.addView(answerLeft ? decline : answer, endParams);

        LinearLayout arrows = new LinearLayout(getContext());
        arrows.setOrientation(LinearLayout.HORIZONTAL);
        arrows.setGravity(Gravity.CENTER);
        int tint = ColorUtils.setAlphaComponent(labelColor(), 0x99);
        ImageView leftArrow = new ImageView(getContext());
        leftArrow.setImageResource(R.drawable.ic_double_arrow);
        leftArrow.setRotation(180f);
        leftArrow.setImageTintList(ColorStateList.valueOf(tint));
        ImageView rightArrow = new ImageView(getContext());
        rightArrow.setImageResource(R.drawable.ic_double_arrow);
        rightArrow.setImageTintList(ColorStateList.valueOf(tint));
        arrows.addView(leftArrow, new LinearLayout.LayoutParams(dp(26), dp(26)));
        View gap = new View(getContext());
        arrows.addView(gap, new LinearLayout.LayoutParams(dp(28), 1));
        arrows.addView(rightArrow, new LinearLayout.LayoutParams(dp(26), dp(26)));
        bar.addView(arrows, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        LayoutParams barParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(88), Gravity.CENTER);
        barParams.leftMargin = dp(24);
        barParams.rightMargin = dp(24);
        addView(bar, barParams);

        ObjectAnimator leftPulse = ObjectAnimator.ofFloat(leftArrow, View.TRANSLATION_X, 0f, -dp(6), 0f);
        leftPulse.setDuration(1200);
        leftPulse.setRepeatCount(ValueAnimator.INFINITE);
        ObjectAnimator rightPulse = ObjectAnimator.ofFloat(rightArrow, View.TRANSLATION_X, 0f, dp(6), 0f);
        rightPulse.setDuration(1200);
        rightPulse.setRepeatCount(ValueAnimator.INFINITE);
        animators.add(leftPulse);
        animators.add(rightPulse);
    }

    private View glassCard(boolean answer) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10), dp(10), dp(18), dp(10));
        card.setBackground(rounded(onDark ? ColorUtils.setAlphaComponent(Color.WHITE, 0x29)
                        : ContextCompat.getColor(getContext(), R.color.surface_elevated), dp(34), dp(1),
                onDark ? ColorUtils.setAlphaComponent(Color.WHITE, 0x59)
                        : ContextCompat.getColor(getContext(), R.color.divider)));
        card.setElevation(dp(onDark ? 0 : 4));
        ImageView icon = circle(answer, 44, false);
        icon.setElevation(0);
        icon.setClickable(false);
        card.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView text = label(getContext().getString(answer ? R.string.answer : R.string.decline), labelColor(), 16, true);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.leftMargin = dp(12);
        card.addView(text, textParams);
        card.setContentDescription(text.getText());
        card.setOnClickListener(v -> fire(answer));
        return card;
    }

    private void buildGlass() {
        LinearLayout row = row(20);
        View answer = glassCard(true);
        View decline = glassCard(false);
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, dp(68), 1f);
        first.rightMargin = dp(8);
        LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, dp(68), 1f);
        second.leftMargin = dp(8);
        row.addView(answerLeft ? answer : decline, first);
        row.addView(answerLeft ? decline : answer, second);
        addCentered(row);

        ObjectAnimator floatUp = ObjectAnimator.ofFloat(answer, View.TRANSLATION_Y, 0f, -dp(5), 0f);
        floatUp.setDuration(2000);
        floatUp.setRepeatCount(ValueAnimator.INFINITE);
        animators.add(floatUp);
    }

    private void buildNeon() {
        FrameLayout frame = new FrameLayout(getContext());
        frame.setClipChildren(false);
        View glow = new View(getContext());
        glow.setBackground(rounded(ColorUtils.setAlphaComponent(NEON, 0x40), dp(46), 0, 0));
        LayoutParams glowParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(92), Gravity.CENTER);
        frame.addView(glow, glowParams);

        GradientDrawable pillBackground = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF0F172A, 0xFF1E1B4B, 0xFF0F172A});
        pillBackground.setCornerRadius(dp(40));
        pillBackground.setStroke(dp(2), NEON);
        FrameLayout pill = new FrameLayout(getContext());
        pill.setBackground(pillBackground);
        pill.setClipChildren(false);
        LayoutParams pillParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(80), Gravity.CENTER);
        pillParams.leftMargin = dp(6);
        pillParams.rightMargin = dp(6);
        frame.addView(pill, pillParams);

        ImageView answer = circle(true, 58, false);
        ImageView decline = circle(false, 58, false);
        LayoutParams startParams = new LayoutParams(dp(58), dp(58), Gravity.CENTER_VERTICAL | Gravity.START);
        startParams.leftMargin = dp(11);
        LayoutParams endParams = new LayoutParams(dp(58), dp(58), Gravity.CENTER_VERTICAL | Gravity.END);
        endParams.rightMargin = dp(11);
        pill.addView(answerLeft ? answer : decline, startParams);
        pill.addView(answerLeft ? decline : answer, endParams);
        TextView text = label(getContext().getString(R.string.tap_to_answer), NEON, 16, true);
        text.setShadowLayer(dp(8), 0, 0, NEON);
        pill.addView(text, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        LayoutParams frameParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96), Gravity.CENTER);
        frameParams.leftMargin = dp(20);
        frameParams.rightMargin = dp(20);
        addView(frame, frameParams);

        ObjectAnimator glowPulse = ObjectAnimator.ofPropertyValuesHolder(glow,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.35f, 1f, 0.35f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.98f, 1.03f, 0.98f));
        glowPulse.setDuration(1800);
        glowPulse.setRepeatCount(ValueAnimator.INFINITE);
        animators.add(glowPulse);
        ObjectAnimator ring = ObjectAnimator.ofFloat(answer, View.ROTATION, 0f, -14f, 14f, -10f, 10f, 0f, 0f, 0f);
        ring.setDuration(1600);
        ring.setRepeatCount(ValueAnimator.INFINITE);
        animators.add(ring);
    }
}
