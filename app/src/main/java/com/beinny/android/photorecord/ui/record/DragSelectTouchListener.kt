package com.beinny.android.photorecord.ui.record

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max

class DragSelectTouchListener(
    private val isActive: () -> Boolean,
    private val onLongPress: (idx: Int) -> Unit,
    private val onDragStart: (startIdx: Int) -> Unit,
    private val onDragRange: (start: Int, end: Int) -> Unit
) : RecyclerView.OnItemTouchListener {

    private var touchSlop = 0
    private var horizontalDragSlop = 0  // 드래그 활성화 기준 (한 칸의 절반 ≈ rv.width/6)
    private var maxFlingVelocity = 0
    private var minFlingVelocity = 0

    private var isDragging = false
    private var isScrolling = false   // 세로 스크롤 직접 처리 (아이템 onClick 차단)
    private var startIndex = RecyclerView.NO_POSITION
    private var lastIndex = RecyclerView.NO_POSITION
    private var initialX = 0f
    private var initialY = 0f
    private var lastScrollY = 0f
    private var velocityTracker: VelocityTracker? = null

    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressFired = false
    private val longPressRunnable = Runnable {
        if (startIndex != RecyclerView.NO_POSITION) {
            longPressFired = true
            onLongPress(startIndex)
        }
    }

    private val choreographer = Choreographer.getInstance()
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activeRv: RecyclerView? = null

    private val autoScrollCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val rv = activeRv ?: return
            val delta = calcAutoScrollDelta(lastTouchY, rv)
            if (delta != 0 && isDragging) {
                rv.scrollBy(0, delta)
                updateDragSelection(rv)
                choreographer.postFrameCallback(this)
            }
        }
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (touchSlop == 0) {
            val vc = ViewConfiguration.get(rv.context)
            touchSlop = vc.scaledTouchSlop
            horizontalDragSlop = maxOf(rv.width / 12, touchSlop * 4)
            maxFlingVelocity = vc.scaledMaximumFlingVelocity
            minFlingVelocity = vc.scaledMinimumFlingVelocity
        }

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                reset(rv)
                longPressFired = false
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(e)
                val child = rv.findChildViewUnder(e.x, e.y) ?: return false
                val pos = rv.getChildAdapterPosition(child)
                if (pos == RecyclerView.NO_POSITION) return false
                startIndex = pos
                lastIndex = pos
                initialX = e.x
                initialY = e.y
                lastScrollY = e.y
                // 선택 모드에서는 롱클릭 타이머 예약 안 함 — 기존 선택 리셋 방지
                if (!isActive()) {
                    longPressHandler.postDelayed(
                        longPressRunnable,
                        ViewConfiguration.getLongPressTimeout().toLong()
                    )
                }
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(e)
                val dx = abs(e.x - initialX)
                val dy = abs(e.y - initialY)

                // longPressFired=true: 방향에 따라 drag 또는 scroll 결정
                if (longPressFired && startIndex != RecyclerView.NO_POSITION) {
                    // 롱클릭 직후 → 방향 무관, touchSlop 초과 시 드래그 시작
                    if (dx > touchSlop || dy > touchSlop) {
                        longPressFired = false
                        isDragging = true
                        activeRv = rv
                        lastTouchX = e.x; lastTouchY = e.y
                        rv.parent?.requestDisallowInterceptTouchEvent(true)
                        onDragStart(startIndex)
                        updateDragSelection(rv)
                        scheduleAutoScroll(rv, e.y)
                        return true
                    }
                    return false
                }

                if (!isActive() || startIndex == RecyclerView.NO_POSITION) return false

                if (!isDragging && !isScrolling) {
                    when {
                        dx > horizontalDragSlop -> {
                            // 가로 이동 → drag select
                            isDragging = true
                            activeRv = rv
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                            onDragStart(startIndex)
                        }
                        dy > touchSlop -> {
                            // 세로 이동 → 인터셉트해서 아이템 onClick 차단 + 수동 스크롤
                            isScrolling = true
                            activeRv = rv
                            lastScrollY = e.y
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                            return true
                        }
                    }
                }
                if (isDragging) {
                    lastTouchX = e.x; lastTouchY = e.y; activeRv = rv
                    updateDragSelection(rv)
                    scheduleAutoScroll(rv, e.y)
                }
            }

            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(e)
                longPressHandler.removeCallbacks(longPressRunnable)
                if (longPressFired) {
                    longPressFired = false
                    reset(rv)
                    return true
                }
                if (isScrolling) fling(rv)
                reset(rv)
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                if (!isActive()) {
                    longPressFired = false
                    reset(rv)
                } else {
                    isDragging = false
                    isScrolling = false
                    val child = rv.findChildViewUnder(e.x, e.y)
                    val pos = child?.let { rv.getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
                    if (pos != RecyclerView.NO_POSITION) {
                        startIndex = pos
                        lastIndex = pos
                        initialX = e.x
                        initialY = e.y
                        lastScrollY = e.y
                    } else {
                        longPressFired = false
                        reset(rv)
                    }
                }
            }
        }
        return isDragging || isScrolling
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        velocityTracker?.addMovement(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                when {
                    isDragging -> {
                        lastTouchX = e.x; lastTouchY = e.y; activeRv = rv
                        updateDragSelection(rv)
                        scheduleAutoScroll(rv, e.y)
                    }
                    isScrolling -> {
                        // 손가락 내리면(e.y 증가) → lastScrollY - e.y = 음수 → scrollBy 음수 → 아래로 스크롤
                        rv.scrollBy(0, (lastScrollY - e.y).toInt())
                        lastScrollY = e.y
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isScrolling) fling(rv)
                reset(rv)
            }
            MotionEvent.ACTION_CANCEL -> reset(rv)
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    private fun fling(rv: RecyclerView) {
        velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
        val vy = velocityTracker?.yVelocity?.toInt() ?: 0
        // 손가락 내리면 vy > 0 → -vy = 음수 → fling 음수 → 아래로 계속 스크롤
        if (abs(vy) > minFlingVelocity) rv.fling(0, -vy)
    }

    private fun updateDragSelection(rv: RecyclerView) {
        val child = rv.findChildViewUnder(lastTouchX, lastTouchY) ?: return
        val pos = rv.getChildAdapterPosition(child)
        if (pos != RecyclerView.NO_POSITION && pos != lastIndex) {
            lastIndex = pos
            onDragRange(startIndex, pos)
        }
    }

    private fun scheduleAutoScroll(rv: RecyclerView, touchY: Float) {
        choreographer.removeFrameCallback(autoScrollCallback)
        if (calcAutoScrollDelta(touchY, rv) != 0) {
            choreographer.postFrameCallback(autoScrollCallback)
        }
    }

    private fun calcAutoScrollDelta(touchY: Float, rv: RecyclerView): Int {
        val h = rv.height.toFloat()
        val zone = h * 0.20f
        val maxSpeed = 18
        return when {
            touchY < zone     -> -max(1, ((zone - touchY) / zone * maxSpeed).toInt())
            touchY > h - zone ->  max(1, ((touchY - h + zone) / zone * maxSpeed).toInt())
            else              -> 0
        }
    }

    private fun reset(rv: RecyclerView? = null) {
        longPressHandler.removeCallbacks(longPressRunnable)
        choreographer.removeFrameCallback(autoScrollCallback)
        activeRv = null
        if (isDragging || isScrolling) rv?.parent?.requestDisallowInterceptTouchEvent(false)
        isDragging = false
        isScrolling = false
        startIndex = RecyclerView.NO_POSITION
        lastIndex = RecyclerView.NO_POSITION
        initialX = 0f
        initialY = 0f
        lastScrollY = 0f
        velocityTracker?.recycle()
        velocityTracker = null
    }
}
