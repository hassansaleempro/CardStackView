package com.yuyakaido.android.cardstackview.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

@Composable
fun <T> CardStackView(
    items: List<T>,
    modifier: Modifier = Modifier,
    setting: CardStackSetting = CardStackSetting(),
    contentKey: (T) -> Any? = { it },
    controller: CardStackViewControllerType<T> = cardStackViewController(items, setting, contentKey),
    onDrag: (T, Float) -> Unit = { _, _ -> },
    onDragStart: (T, Offset) -> Unit = { _, _ -> },
    onDragEnd: (T) -> Unit = {},
    onDragCancel: (T) -> Unit = {},
    onEmpty: () -> Unit = {},
    onCardAppeared: (T) -> Unit = {},
    onSwiped: (T, Direction) -> Unit = { _, _ -> },
    content: @Composable (T) -> Unit
) {
    val visibleContents = items.filter {
        !controller.currentCardController(it).isCardSwiped()
    }.take(setting.visibleCount)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val visibleSize = visibleContents.size
        visibleContents.forEachIndexed { index, item ->
            val zIndex = visibleSize - index - 1
            val cardController = controller.currentCardController(item)
            key(contentKey(item)) {
                val padding =
                    PaddingBetweenCards.get(setting.translationInterval, setting.stackFrom)
                val paddingX by animateFloatAsState(targetValue = (zIndex * padding.paddingX))
                val paddingY by animateFloatAsState(targetValue = (zIndex * padding.paddingY))
                Box(
                    modifier = modifier
                        .zIndex(zIndex.toFloat())
                        .graphicsLayer(
                            translationX = cardController.cardX + paddingX,
                            translationY = cardController.cardY + paddingY,
                            rotationZ = cardController.rotation,
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    onDragStart(item, it)
                                },
                                onDragEnd = {
                                    cardController.onDragEnd()
                                    onDragEnd(item)
                                },
                                onDragCancel = {
                                    cardController.onDragCancel()
                                    onDragCancel(item)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    cardController.onDrag(dragAmount)
                                    onDrag(item, cardController.ratio)
                                }
                            )
                        },
                ) {
                    content(item)
                }
                LaunchedEffect(cardController.direction) {
                    cardController.direction?.let {
                        onSwiped(item, it)
                    }
                }
                LaunchedEffect(controller.displayedItem(item)) {
                    val displayedItem = controller.displayedItem(item)
                    if (displayedItem != null) {
                        onCardAppeared(displayedItem)
                    }
                }
            }
        }
        LaunchedEffect(controller.isEmpty()) {
            if (controller.isEmpty()) {
                onEmpty()
            }
        }
    }
}
