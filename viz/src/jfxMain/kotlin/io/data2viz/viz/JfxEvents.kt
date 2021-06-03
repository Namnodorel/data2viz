/*
 * Copyright (c) 2018-2019. data2viz sàrl.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.data2viz.viz


import io.data2viz.geom.Point
import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.input.ZoomEvent

private val emptyDisposable = object : Disposable { override fun dispose() {} }


public actual class KTouch {
    public actual companion object TouchEventListener : KEventListener<KTouchEvent> {
        override fun addNativeListener(target: Any, listener: (KTouchEvent) -> Unit): Disposable = emptyDisposable
    }
}

public actual class KTouchStart {
    public actual companion object TouchStartEventListener : KEventListener<KPointerEvent> {
        override fun addNativeListener(target: Any, listener: (KPointerEvent) -> Unit) = emptyDisposable
    }
}

public actual class KTouchEnd {
    public actual companion object TouchEndEventListener : KEventListener<KPointerEvent> {
        override fun addNativeListener(target: Any, listener: (KPointerEvent) -> Unit) = emptyDisposable
    }
}

public actual class KTouchMove {
    public actual companion object TouchMoveEventListener : KEventListener<KPointerEvent> {
        override fun addNativeListener(target: Any, listener: (KPointerEvent) -> Unit) = emptyDisposable
    }
}


public actual class KMouseDown {
    public actual companion object PointerDownEventListener : KEventListener<KMouseEvent> {
        override fun addNativeListener(target: Any, listener: (KMouseEvent) -> Unit): Disposable =
            createSimpleJvmEventHandle(listener, target, MouseEvent.MOUSE_PRESSED)
    }
}

public actual class KMouseUp {
    public actual companion object PointerUpEventListener : KEventListener<KMouseEvent> {
        override fun addNativeListener(target: Any, listener: (KMouseEvent) -> Unit): Disposable =
            createSimpleJvmEventHandle(listener, target, MouseEvent.MOUSE_RELEASED)
    }
}

public actual class KMouseMove {
    public actual companion object PointerMoveEventListener : KEventListener<KMouseEvent> {
        override fun addNativeListener(target: Any, listener: (KMouseEvent) -> Unit): Disposable {

            // Add listeners for both events MOVED & DRAGGED, because MOVED not fires when any button pressed
            // but JS behaviour is different
            val jfxEvents = listOf(MouseEvent.MOUSE_MOVED, MouseEvent.MOUSE_DRAGGED)
            return createSimpleJvmEventHandle(listener, target, jfxEvents)
        }
    }
}

public actual class KPointerEnter {
    public actual companion object PointerEnterEventListener : KEventListener<KPointerEvent> {
        override fun addNativeListener(target: Any, listener: (KPointerEvent) -> Unit): Disposable =
            createSimpleJvmEventHandle(listener, target, MouseEvent.MOUSE_ENTERED)
    }
}

public actual class KPointerLeave {
    public actual companion object PointerLeaveEventListener : KEventListener<KPointerEvent> {
        override fun addNativeListener(target: Any, listener: (KPointerEvent) -> Unit): Disposable =
            createSimpleJvmEventHandle(listener, target, MouseEvent.MOUSE_EXITED)
    }
}


public actual class KPointerDoubleClick {
    public actual companion object PointerDoubleClickEventListener : KEventListener<KPointerEvent> {
        override fun addNativeListener(target: Any, listener: (KPointerEvent) -> Unit): Disposable =
            createJvmClickEventHandle(target, listener, eventClickCount = 2)
    }
}

public actual class KPointerClick {
    public actual companion object PointerClickEventListener : KEventListener<KPointerEvent> {
        override fun addNativeListener(target: Any, listener: (KPointerEvent) -> Unit): Disposable =
            createSimpleJvmEventHandle(listener, target, MouseEvent.MOUSE_CLICKED)
    }
}

@ExperimentalKEvent
public actual class KZoom {
    public actual companion object ZoomEventListener : KEventListener<KZoomEvent> {
        const val minGestureZoomDeltaValue = 0.8
        const val maxGestureZoomDeltaValue = 1.2

        const val minWheelZoomDeltaValue = -100.0
        const val maxWheelZoomDeltaValue = 100.0

        var lastZoomTime: Long? = null
        lateinit var zoomStartPoint: Point

        override fun addNativeListener(target: Any, listener: (KZoomEvent) -> Unit): Disposable {

            val canvas = target as Canvas

            val zoomHandler = EventHandler<ZoomEvent> { event ->
                val currentDelta = event.zoomFactor
                listener(
                    onZoom(
                        event.x, event.y,
                        currentDelta,
                        minGestureZoomDeltaValue,
                        maxGestureZoomDeltaValue
                    )
                )
            }


            val scrollHandler = EventHandler<ScrollEvent> { event ->
                if (event.isControlDown) {
                    val currentDelta = event.deltaY
                    listener(
                        onZoom(
                            event.x, event.y,
                            currentDelta,
                            minWheelZoomDeltaValue,
                            maxWheelZoomDeltaValue
                        )
                    )
                }
            }

            return JvmZoomHandle(canvas, scrollHandler, zoomHandler).also { it.init() }
        }

        private fun onZoom(
            x:Double, y:Double,
            currentDelta: Double, minDelta: Double,
            maxDelta: Double
        ): KZoomEvent {

            val currentTime = System.currentTimeMillis()
            if (KZoomEvent.isNewZoom(currentTime, lastZoomTime)) {
                zoomStartPoint = Point(x, y)
            }
            lastZoomTime = currentTime
            return KZoomEvent(
                zoomStartPoint,
                KZoomEvent.scaleDelta(
                    currentDelta,
                    minDelta,
                    maxDelta
                )
            )
        }
    }
}


private fun createSimpleJvmEventHandle(
    listener: (KMouseEvent) -> Unit,
    target: Any,
    jfxEvent: EventType<MouseEvent>
): JvmEventHandle<MouseEvent> =
    createSimpleJvmEventHandle(listener, target, listOf(jfxEvent))

private fun createSimpleJvmEventHandle(
    listener: (KMouseEvent) -> Unit,
    target: Any,
    jfxEvents: List<EventType<MouseEvent>>
): JvmEventHandle<MouseEvent> {

    val eventHandler = EventHandler<MouseEvent> { event ->
        val kMouseEvent = event.toKMouseEvent()
        listener(kMouseEvent)
    }
    val canvas = target as Canvas
    val jvmEventHandle = JvmEventHandle(canvas, jfxEvents, eventHandler)
    jvmEventHandle.init()
    return jvmEventHandle
}

data class JvmEventHandle<T : Event?>(
    val canvas: Canvas,
    val types: List<EventType<T>>,
    val eventHandler: EventHandler<T>
) : Disposable {

    constructor(canvas: Canvas, type: EventType<T>, eventHandler: EventHandler<T>) : this(
        canvas,
        listOf(type),
        eventHandler
    )

    fun init() {
        types.forEach { jfxEvent: EventType<T> ->
            canvas.addEventHandler(jfxEvent, eventHandler)
        }
    }

    override fun dispose() {

        types.forEach { jfxEvent: EventType<T> ->

            canvas.removeEventHandler(jfxEvent, eventHandler)
        }
    }
}

data class JvmZoomHandle(
    val canvas: Canvas,
    val scrollHandler: EventHandler<ScrollEvent>,
    val zoomHandler: EventHandler<ZoomEvent>
) : Disposable {

    fun init() {
        canvas.onScroll = scrollHandler
        canvas.onZoom = zoomHandler
    }

    override fun dispose() {
        canvas.onScroll = null
        canvas.onZoom = null
    }
}

private fun createJvmClickEventHandle(
    target: Any,
    listener: (KPointerEvent) -> Unit,
    eventClickCount: Int
): JvmEventHandle<MouseEvent> {
    val jfxEvent = MouseEvent.MOUSE_CLICKED

    val eventHandler = EventHandler<MouseEvent> { event ->
        if (event.clickCount == eventClickCount) {
            val kevent = event.toKMouseEvent()
            listener(kevent)
        }
    }
    val canvas = target as Canvas
    return JvmEventHandle(canvas, jfxEvent, eventHandler).also {
        it.init()
    }
}


internal actual fun <T> VizRenderer.addNativeEventListenerFromHandle(handle: KEventHandle<T>): Disposable where T : KEvent {
    return (this as? JFxVizRenderer)?.let {
        handle.eventListener.addNativeListener(it.canvas, handle.listener)
    } ?: object : Disposable { // for the test
            override fun dispose() {
        }
    }
}


private fun MouseEvent.toKMouseEvent(): KMouseEvent = KMouseEvent(
    Point(x, y),
    isAltDown,
    isControlDown,
    isShiftDown,
    isMetaDown
)

