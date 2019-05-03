package net.feheren_fekete.applist.applistpage.viewmodel

abstract class BaseItem(val id: Long, val name: String) {

    var isEnabled = true

    var isDraggedOverLeft = false
        private set

    var isDraggedOverRight = false
        private set

    var isHighlighted = false

    fun setDraggedOverState(state: DraggedOverState) {
        when (state) {
            DraggedOverState.Left -> {
                isDraggedOverLeft = true
                isDraggedOverRight = false
            }
            DraggedOverState.Right -> {
                isDraggedOverLeft = false
                isDraggedOverRight = true
            }
            DraggedOverState.None -> {
                isDraggedOverLeft = false
                isDraggedOverRight = false
            }
        }
    }

    fun getDraggedOverState(): DraggedOverState {
        when {
            isDraggedOverLeft && !isDraggedOverRight -> return DraggedOverState.Left
            isDraggedOverRight && !isDraggedOverLeft -> return DraggedOverState.Right
            else -> return DraggedOverState.None
        }
    }

    enum class DraggedOverState {
        None,
        Left,
        Right
    }

}
