package com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.analytics;

import android.content.res.Resources;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.contacts.manager.ai.smartdialer.voicemessages.calling.callapp.R;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

public final class ClickTracker {

    private final Supplier<String> source;
    private final int[] location = new int[2];
    private WeakReference<View> target;
    private float downX;
    private float downY;
    private int touchSlop = -1;

    public ClickTracker(@NonNull Supplier<String> source) {
        this.source = source;
    }

    public void onTouch(@NonNull View root, @NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (touchSlop < 0) {
                    touchSlop = ViewConfiguration.get(root.getContext()).getScaledTouchSlop();
                }
                downX = event.getRawX();
                downY = event.getRawY();
                View hit = findClickable(root, (int) downX, (int) downY);
                target = hit == null ? null : new WeakReference<>(hit);
                break;
            case MotionEvent.ACTION_MOVE:
                if (target != null && (Math.abs(event.getRawX() - downX) > touchSlop
                        || Math.abs(event.getRawY() - downY) > touchSlop)) {
                    target = null;
                }
                break;
            case MotionEvent.ACTION_UP:
                View view = target == null ? null : target.get();
                target = null;
                if (view != null && view.isEnabled() && view.isAttachedToWindow()) {
                    boolean longPress = view.isLongClickable()
                            && event.getEventTime() - event.getDownTime() >= ViewConfiguration.getLongPressTimeout();
                    String element = elementName(view) + (longPress ? "_long_press" : "");
                    Analytics.logClick(source.get(), element, listPosition(view));
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                target = null;
                break;
            default:
                break;
        }
    }

    @Nullable
    private View findClickable(View view, int x, int y) {
        if (view.getVisibility() != View.VISIBLE || !contains(view, x, y)) {
            return null;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View hit = findClickable(group.getChildAt(i), x, y);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return view.isClickable() || view.isLongClickable() ? view : null;
    }

    private boolean contains(View view, int x, int y) {
        view.getLocationOnScreen(location);
        return x >= location[0] && x < location[0] + view.getWidth()
                && y >= location[1] && y < location[1] + view.getHeight();
    }

    static String elementName(View view) {
        String own = ownName(view);
        if (own != null) {
            return own;
        }
        for (ViewParent parent = view.getParent(); parent instanceof View; parent = parent.getParent()) {
            String name = ownName((View) parent);
            if (name != null) {
                return name + "_item";
            }
        }
        return Analytics.snakeCase(view.getClass().getSimpleName());
    }

    @Nullable
    private static String ownName(View view) {
        Object tag = view.getTag(R.id.analytics_name);
        if (tag != null) {
            return tag.toString();
        }
        int id = view.getId();
        if (id == View.NO_ID || id == android.R.id.content) {
            return null;
        }
        try {
            Resources resources = view.getResources();
            String entry = resources.getResourceEntryName(id);
            if ("android".equals(resources.getResourcePackageName(id))) {
                switch (entry) {
                    case "button1":
                        return "positive_button";
                    case "button2":
                        return "negative_button";
                    case "button3":
                        return "neutral_button";
                    default:
                        return null;
                }
            }
            return Analytics.snakeCase(entry);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    private static int listPosition(View view) {
        View child = view;
        for (ViewParent parent = view.getParent(); parent instanceof View; parent = parent.getParent()) {
            if (parent instanceof RecyclerView) {
                return ((RecyclerView) parent).getChildAdapterPosition(child);
            }
            child = (View) parent;
        }
        return RecyclerView.NO_POSITION;
    }
}
