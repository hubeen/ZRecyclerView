package kr.hubeen.zrecyclerview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@SuppressLint("RtlHardcoded")
public class ZRecyclerItemLayout extends FrameLayout {

    public static final String TAG = "ZRecyclerItemLayout";
    public static final int PREVIEW_UNSPECIFIED = -1;
    public static final int PREVIEW_LEFT = 0;
    public static final int PREVIEW_RIGHT = 1;

    private int preview = PREVIEW_UNSPECIFIED;
    private ViewDragHelper mDragHelper;
    private int mTouchSlop;
    private int mVelocity;

    private float mDownX;
    private float mDownY;
    private boolean mIsDragged;
    private boolean mSwipeEnable = true;
    private View mCurrentMenu;
    private boolean mIsOpen;

    private LinkedHashMap<Integer, View> mMenus = new LinkedHashMap<>();

    private List<SwipeListener> mListeners;

    public ZRecyclerItemLayout(Context context) {
        this(context, null);
    }

    public ZRecyclerItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZRecyclerItemLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setClickable(true);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
        mDragHelper = ViewDragHelper.create(this, new DragCallBack());

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ZRecyclerItemLayout);
            preview = a.getInt(R.styleable.ZRecyclerItemLayout_preview, PREVIEW_UNSPECIFIED);
            a.recycle();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (isInEditMode()) {
            if (preview == PREVIEW_LEFT) {
                View menu = mMenus.get(Gravity.LEFT);
                View content = getContentView();
                if (menu != null && menu != content) {
                    content.layout(
                            menu.getMeasuredWidth(),
                            content.getTop(),
                            content.getRight() + menu.getMeasuredWidth(),
                            content.getBottom());
                }
            } else if (preview == PREVIEW_RIGHT) {
                View menu = mMenus.get(Gravity.RIGHT);
                View content = getContentView();
                if (menu != null && menu != content) {
                    content.layout(
                            -menu.getMeasuredWidth(),
                            content.getTop(),
                            content.getRight() - menu.getMeasuredWidth(),
                            content.getBottom());
                }
            }
            return;
        }
        updateMenu();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (isCloseAnimating()) {
                return false;
            }

            if (mIsOpen && isTouchContent(((int) ev.getX()), ((int) ev.getY()))) {
                close();
                return false;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mSwipeEnable) {
            return false;
        }

        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsDragged = false;
                mDownX = ev.getX();
                mDownY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                checkCanDragged(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsDragged) {
                    mDragHelper.processTouchEvent(ev);
                    mIsDragged = false;
                }
                break;
            default:
                if (mIsDragged) {
                    mDragHelper.processTouchEvent(ev);
                }
                break;
        }
        return mIsDragged || super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mSwipeEnable) {
            return super.onTouchEvent(ev);
        }

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsDragged = false;
                mDownX = ev.getX();
                mDownY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                boolean beforeCheckDrag = mIsDragged;
                checkCanDragged(ev);
                if (mIsDragged) {
                    mDragHelper.processTouchEvent(ev);
                }

                if (!beforeCheckDrag && mIsDragged) {
                    MotionEvent obtain = MotionEvent.obtain(ev);
                    obtain.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(obtain);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsDragged || mIsOpen) {
                    mDragHelper.processTouchEvent(ev);
                    ev.setAction(MotionEvent.ACTION_CANCEL);
                    mIsDragged = false;
                }
                break;
            default:
                if (mIsDragged) {
                    mDragHelper.processTouchEvent(ev);
                }
                break;
        }
        return mIsDragged || super.onTouchEvent(ev);
//                || (!isClickable() && mMenus.size() > 0);
    }

    private void checkCanDragged(MotionEvent ev) {
        if (mIsDragged) {
            return;
        }

        float dx = ev.getX() - mDownX;
        float dy = ev.getY() - mDownY;
        boolean isRightDrag = dx > mTouchSlop && dx > Math.abs(dy);
        boolean isLeftDrag = dx < -mTouchSlop && Math.abs(dx) > Math.abs(dy);

        if (mIsOpen) {
            int downX = (int) mDownX;
            int downY = (int) mDownY;
            if (isTouchContent(downX, downY)) {
                mIsDragged = true;
            } else if (isTouchMenu(downX, downY)) {
                mIsDragged = (isLeftMenu() && isLeftDrag) || (isRightMenu() && isRightDrag);
            }

        } else {
            if (isRightDrag) {
                mCurrentMenu = mMenus.get(Gravity.LEFT);
                mIsDragged = mCurrentMenu != null;
            } else if (isLeftDrag) {
                mCurrentMenu = mMenus.get(Gravity.RIGHT);
                mIsDragged = mCurrentMenu != null;
            }
        }

        if (mIsDragged) {
            MotionEvent obtain = MotionEvent.obtain(ev);
            obtain.setAction(MotionEvent.ACTION_DOWN);
            mDragHelper.processTouchEvent(obtain);
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int gravity = GravityCompat.getAbsoluteGravity(lp.gravity, ViewCompat.getLayoutDirection(child));
        switch (gravity) {
            case Gravity.RIGHT:
                mMenus.put(Gravity.RIGHT, child);
                break;
            case Gravity.LEFT:
                mMenus.put(Gravity.LEFT, child);
                break;
            default:
                break;
        }
    }

    public void setSwipeEnable(boolean enable) {
        mSwipeEnable = enable;
    }

    public boolean isSwipeEnable() {
        return mSwipeEnable;
    }

    public View getContentView() {
        return getChildAt(getChildCount() - 1);
    }

    private boolean isTouchContent(int x, int y) {
        View contentView = getContentView();
        if (contentView == null) {
            return false;
        }
        Rect rect = new Rect();
        contentView.getHitRect(rect);
        return rect.contains(x, y);
    }

    private boolean isLeftMenu() {
        return mCurrentMenu != null && mCurrentMenu == mMenus.get(Gravity.LEFT);
    }

    private boolean isRightMenu() {
        return mCurrentMenu != null && mCurrentMenu == mMenus.get(Gravity.RIGHT);
    }

    private boolean isTouchMenu(int x, int y) {
        if (mCurrentMenu == null) {
            return false;
        }

        Rect rect = new Rect();
        mCurrentMenu.getHitRect(rect);
        return rect.contains(x, y);
    }

    private boolean checkAbsoluteGravity(View menu, int checkFor) {
        final int absGravity = getAbsoluteGravity(menu);
        return (absGravity & checkFor) == checkFor;
    }

    private int getAbsoluteGravity(View menu) {
        final int gravity = ((LayoutParams) menu.getLayoutParams()).gravity;
        return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));
    }

    public void close() {
        if (mCurrentMenu == null) {
            mIsOpen = false;
            return;
        }
        mDragHelper.smoothSlideViewTo(getContentView(), getPaddingLeft(), getPaddingTop());
        mIsOpen = false;
        if (mListeners != null) {
            int listenerCount = mListeners.size();
            for (int i = listenerCount - 1; i >= 0; i--) {
                mListeners.get(i).onSwipeClose(this);
            }
        }
        invalidate();
    }

    public void open() {
        if (mCurrentMenu == null) {
            mIsOpen = false;
            return;
        }

        if (isLeftMenu()) {
            mDragHelper.smoothSlideViewTo(getContentView(), mCurrentMenu.getWidth(), getPaddingTop());
        } else if (isRightMenu()) {
            mDragHelper.smoothSlideViewTo(getContentView(), -mCurrentMenu.getWidth(), getPaddingTop());
        }
        mIsOpen = true;
        if (mListeners != null) {
            int listenerCount = mListeners.size();
            for (int i = listenerCount - 1; i >= 0; i--) {
                mListeners.get(i).onSwipeOpen(this);
            }
        }
        invalidate();
    }

    public boolean isOpen() {
        return mIsOpen;
    }

    private boolean isOpenAnimating() {
        if (mCurrentMenu != null) {
            int contentLeft = getContentView().getLeft();
            int menuWidth = mCurrentMenu.getWidth();
            return mIsOpen && ((isLeftMenu() && contentLeft < menuWidth)
                    || (isRightMenu() && -contentLeft < menuWidth));
        }
        return false;
    }

    private boolean isCloseAnimating() {
        if (mCurrentMenu != null) {
            int contentLeft = getContentView().getLeft();
            return !mIsOpen && ((isLeftMenu() && contentLeft > 0)
                    || (isRightMenu() && contentLeft < 0));
        }
        return false;
    }

    private void updateMenu() {
        View contentView = getContentView();
        if (contentView != null) {
            int contentLeft = contentView.getLeft();
            if (contentLeft == 0) {
                for (View view : mMenus.values()) {
                    if (checkAbsoluteGravity(view, Gravity.LEFT)) {
                        view.layout(-view.getWidth(), view.getTop(), 0, view.getBottom());
                    } else {
                        view.layout(getMeasuredWidth(), view.getTop(),
                                getMeasuredWidth() + view.getMeasuredWidth(), view.getBottom());
                    }
                }
            } else {
                if (mCurrentMenu != null && mCurrentMenu.getLeft() != 0) {
                    if (isLeftMenu()) {
                        mCurrentMenu.layout(0, mCurrentMenu.getTop(),
                                mCurrentMenu.getMeasuredWidth(), mCurrentMenu.getBottom());
                    } else {
                        mCurrentMenu.layout(
                                getMeasuredWidth() - mCurrentMenu.getMeasuredWidth(),
                                mCurrentMenu.getTop(),
                                getMeasuredWidth(),
                                mCurrentMenu.getBottom());
                    }
                }
            }
        }
    }

    public void addSwipeListener(SwipeListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(listener);
    }

    public void removeSwipeListener(SwipeListener listener) {
        if (listener == null || mListeners == null) {
            return;
        }
        mListeners.remove(listener);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private class DragCallBack extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child == getContentView() || mMenus.containsValue(child);
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            if (child == getContentView()) {
                if (isRightMenu()) {
                    return left > 0 ? 0 : left < -mCurrentMenu.getWidth() ? -mCurrentMenu.getWidth() : left;
                } else if (isLeftMenu()) {
                    return left > mCurrentMenu.getWidth() ? mCurrentMenu.getWidth() : Math.max(left, 0);
                }
            } else if (isRightMenu()) {
                View contentView = getContentView();
                int newLeft = contentView.getLeft() + dx;
                if (newLeft > 0) {
                    newLeft = 0;
                } else if (newLeft < -child.getWidth()) {
                    newLeft = -child.getWidth();
                }
                contentView.layout(newLeft, contentView.getTop(), newLeft + contentView.getWidth(),
                        contentView.getBottom());
                return child.getLeft();
            } else if (isLeftMenu()) {
                View contentView = getContentView();
                int newLeft = contentView.getLeft() + dx;
                if (newLeft < 0) {
                    newLeft = 0;
                } else if (newLeft > child.getWidth()) {
                    newLeft = child.getWidth();
                }
                contentView.layout(newLeft, contentView.getTop(), newLeft + contentView.getWidth(),
                        contentView.getBottom());
                return child.getLeft();
            }
            return 0;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            updateMenu();
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            Log.e(TAG, "onViewReleased: " + xvel + " ,releasedChild = " + releasedChild);
            if (isLeftMenu()) {
                if (xvel > mVelocity) {
                    open();
                } else if (xvel < -mVelocity) {
                    close();
                } else {
                    if (getContentView().getLeft() > mCurrentMenu.getWidth() / 3 * 2) {
                        open();
                    } else {
                        close();
                    }
                }
            } else if (isRightMenu()) {
                if (xvel < -mVelocity) {
                    open();
                } else if (xvel > mVelocity) {
                    close();
                } else {
                    if (getContentView().getLeft() < -mCurrentMenu.getWidth() / 3 * 2) {
                        open();
                    } else {
                        close();
                    }
                }
            }
        }

    }

    public interface SwipeListener {
        void onSwipeOpen(ZRecyclerItemLayout view);

        void onSwipeClose(ZRecyclerItemLayout view);
    }
}