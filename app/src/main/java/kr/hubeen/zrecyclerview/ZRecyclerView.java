package kr.hubeen.zrecyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class ZRecyclerView extends RecyclerView {

    private boolean enableTouchAlways = false;

    public ZRecyclerView(Context context) {
        this(context, null);
    }

    public ZRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ZRecyclerView);
            enableTouchAlways = a.getBoolean(R.styleable.ZRecyclerView_enableTouchAlways, false);
            a.recycle();
        }
    }

    public void setEnableTouchAlways(boolean enableTouchAlways) {
        this.enableTouchAlways = enableTouchAlways;
    }

    public boolean isEnableTouchAlways() {
        return enableTouchAlways;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            View openItem = findOpenItem();
            if (openItem != null && openItem != getTouchItem(x, y)) {
                ZRecyclerItemLayout ZRecyclerItemLayout = findSwipeItemLayout(openItem);
                if (ZRecyclerItemLayout != null) {
                    ZRecyclerItemLayout.close();
                    if (!enableTouchAlways) {
                        return false;
                    }
                }
            }
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Nullable
    private View getTouchItem(int x, int y) {
        Rect frame = new Rect();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return child;
                }
            }
        }
        return null;
    }

    @Nullable
    private View findOpenItem() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ZRecyclerItemLayout ZRecyclerItemLayout = findSwipeItemLayout(getChildAt(i));
            if (ZRecyclerItemLayout != null && ZRecyclerItemLayout.isOpen()) {
                return getChildAt(i);
            }
        }
        return null;
    }

    @Nullable
    private ZRecyclerItemLayout findSwipeItemLayout(View view) {
        if (view instanceof ZRecyclerItemLayout) {
            return (ZRecyclerItemLayout) view;
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                ZRecyclerItemLayout swipeLayout = findSwipeItemLayout(group.getChildAt(i));
                if (swipeLayout != null) {
                    return swipeLayout;
                }
            }
        }
        return null;
    }
}
