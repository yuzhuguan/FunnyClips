package com.victor.clips.ui.widget

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.victor.clips.R
import com.victor.clips.util.AnimUtil
import com.victor.clips.util.ColorUtil
import com.victor.clips.util.ViewUtils
import java.util.ArrayList

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: ElasticDragDismissFrameLayout
 * Author: Victor
 * Date: 2019/11/22 11:41
 * Description: 
 * -----------------------------------------------------------------
 */

class ElasticDragDismissFrameLayout: FrameLayout {

    // configurable attribs
    private var dragDismissDistance = java.lang.Float.MAX_VALUE
    private var dragDismissFraction = -1f
    private var dragDismissScale = 1f
    private var shouldScale = false
    private var dragElacticity = 0.8f

    // state
    private var totalDrag: Float = 0.toFloat()
    private var draggingDown = false
    private var draggingUp = false
    private var mLastActionEvent: Int = 0

    private var callbacks: MutableList<ElasticDragDismissCallback>? = null

    constructor(context: Context) : this(context,null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs,0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initAttr(context,attrs)
    }

    fun initAttr (context: Context,attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.ElasticDragDismissFrameLayout, 0, 0)

        if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragDismissDistance)) {
            dragDismissDistance = a.getDimensionPixelSize(R.styleable
                    .ElasticDragDismissFrameLayout_dragDismissDistance, 0).toFloat()
        } else if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragDismissFraction)) {
            dragDismissFraction = a.getFloat(R.styleable
                    .ElasticDragDismissFrameLayout_dragDismissFraction, dragDismissFraction)
        }
        if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragDismissScale)) {
            dragDismissScale = a.getFloat(R.styleable
                    .ElasticDragDismissFrameLayout_dragDismissScale, dragDismissScale)
            shouldScale = dragDismissScale != 1f
        }
        if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragElasticity)) {
            dragElacticity = a.getFloat(R.styleable.ElasticDragDismissFrameLayout_dragElasticity,
                    dragElacticity)
        }
        a.recycle()
    }

    interface  ElasticDragDismissCallback {

        /**
         * Called for each drag event.
         *
         * @param elasticOffset       Indicating the drag offset with elasticity applied i.e. may
         * exceed 1.
         * @param elasticOffsetPixels The elastically scaled drag distance in pixels.
         * @param rawOffset           Value from [0, 1] indicating the raw drag offset i.e.
         * without elasticity applied. A value of 1 indicates that the
         * dismiss distance has been reached.
         * @param rawOffsetPixels     The raw distance the user has dragged
         */
        fun onDrag(elasticOffset: Float, elasticOffsetPixels: Float,
                            rawOffset: Float, rawOffsetPixels: Float) {
        }

        /**
         * Called when dragging is released and has exceeded the threshold dismiss distance.
         */
        fun onDragDismissed() {}

    }

    override fun onStartNestedScroll(child: View?, target: View?, nestedScrollAxes: Int): Boolean {
        return nestedScrollAxes and View.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedPreScroll(target: View?, dx: Int, dy: Int, consumed: IntArray) {
        // if we're in a drag gesture and the user reverses up the we should take those events
        if (draggingDown && dy > 0 || draggingUp && dy < 0) {
            dragScale(dy)
            consumed[1] = dy
        }
    }

    override fun onNestedScroll(target: View?, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
        dragScale(dyUnconsumed)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        mLastActionEvent = ev?.action!!.toInt()
        return super.onInterceptTouchEvent(ev)
    }

    override fun onStopNestedScroll(child: View?) {
        if (Math.abs(totalDrag) >= dragDismissDistance) {
            dispatchDismissCallback()
        } else { // settle back to natural position
            if (mLastActionEvent == MotionEvent.ACTION_DOWN) {
                // this is a 'defensive cleanup for new gestures',
                // don't animate here
                // see also https://github.com/nickbutcher/plaid/issues/185
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
            } else {
                animate()
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200L)
                        .setInterpolator(AnimUtil.getFastOutSlowInInterpolator(context))
                        .setListener(null)
                        .start()
            }
            totalDrag = 0f
            draggingUp = false
            draggingDown = draggingUp
            dispatchDragCallback(0f, 0f, 0f, 0f)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (dragDismissFraction > 0f) {
            dragDismissDistance = h * dragDismissFraction
        }
    }

    fun addListener(listener: ElasticDragDismissCallback) {
        if (callbacks == null) {
            callbacks = ArrayList()
        }
        callbacks?.add(listener)
    }

    fun removeListener(listener: ElasticDragDismissCallback) {
        if (callbacks != null && callbacks?.size!! > 0) {
            callbacks?.remove(listener)
        }
    }

    private fun dragScale(scroll: Int) {
        if (scroll == 0) return

        totalDrag += scroll.toFloat()

        // track the direction & set the pivot point for scaling
        // don't double track i.e. if start dragging down and then reverse, keep tracking as
        // dragging down until they reach the 'natural' position
        if (scroll < 0 && !draggingUp && !draggingDown) {
            draggingDown = true
            if (shouldScale) pivotY = height.toFloat()
        } else if (scroll > 0 && !draggingDown && !draggingUp) {
            draggingUp = true
            if (shouldScale) pivotY = 0f
        }
        // how far have we dragged relative to the distance to perform a dismiss
        // (0–1 where 1 = dismiss distance). Decreasing logarithmically as we approach the limit
        var dragFraction = Math.log10((1 + Math.abs(totalDrag) / dragDismissDistance).toDouble()).toFloat()

        // calculate the desired translation given the drag fraction
        var dragTo = dragFraction * dragDismissDistance * dragElacticity

        if (draggingUp) {
            // as we use the absolute magnitude when calculating the drag fraction, need to
            // re-apply the drag direction
            dragTo *= -1f
        }
        translationY = dragTo

        if (shouldScale) {
            val scale = 1 - (1 - dragDismissScale) * dragFraction
            scaleX = scale
            scaleY = scale
        }

        // if we've reversed direction and gone past the settle point then clear the flags to
        // allow the list to get the scroll events & reset any transforms
        if (draggingDown && totalDrag >= 0 || draggingUp && totalDrag <= 0) {
            dragFraction = 0f
            dragTo = dragFraction
            totalDrag = dragTo
            draggingUp = false
            draggingDown = draggingUp
            translationY = 0f
            scaleX = 1f
            scaleY = 1f
        }
        dispatchDragCallback(dragFraction, dragTo,
                Math.min(1f, Math.abs(totalDrag) / dragDismissDistance), totalDrag)
    }

    private fun dispatchDragCallback(elasticOffset: Float, elasticOffsetPixels: Float,
                                     rawOffset: Float, rawOffsetPixels: Float) {
        if (callbacks != null && !callbacks!!.isEmpty()) {
            for (callback in callbacks!!) {
                callback.onDrag(elasticOffset, elasticOffsetPixels,
                        rawOffset, rawOffsetPixels)
            }
        }
    }

    private fun dispatchDismissCallback() {
        if (callbacks != null && !callbacks!!.isEmpty()) {
            for (callback in callbacks!!) {
                callback.onDragDismissed()
            }
        }
    }

    /**
     * An [ElasticDragDismissCallback] which fades system chrome (i.e. status bar and
     * navigation bar) whilst elastic drags are performed and
     * [finishes][Activity.finishAfterTransition] the activity when drag dismissed.
     */
    class SystemChromeFader(private val activity: Activity) : ElasticDragDismissCallback {
        private val statusBarAlpha: Int
        private val navBarAlpha: Int
        private val fadeNavBar: Boolean

        init {
            statusBarAlpha = Color.alpha(activity.window.statusBarColor)
            navBarAlpha = Color.alpha(activity.window.navigationBarColor)
            fadeNavBar = ViewUtils.isNavBarOnBottom(activity)
        }

        override fun onDrag(elasticOffset: Float, elasticOffsetPixels: Float,
                                   rawOffset: Float, rawOffsetPixels: Float) {
            if (elasticOffsetPixels > 0) {
                // dragging downward, fade the status bar in proportion
                activity.window.statusBarColor = ColorUtil.modifyAlpha(activity.window
                        .statusBarColor, ((1f - rawOffset) * statusBarAlpha).toInt())
            } else if (elasticOffsetPixels == 0f) {
                // reset
                activity.window.statusBarColor = ColorUtil.modifyAlpha(
                        activity.window.statusBarColor, statusBarAlpha)
                activity.window.navigationBarColor = ColorUtil.modifyAlpha(
                        activity.window.navigationBarColor, navBarAlpha)
            } else if (fadeNavBar) {
                // dragging upward, fade the navigation bar in proportion
                activity.window.navigationBarColor = ColorUtil.modifyAlpha(activity.window.navigationBarColor,
                        ((1f - rawOffset) * navBarAlpha).toInt())
            }
        }

        override fun onDragDismissed() {
            activity.finishAfterTransition()
        }
    }

}