package net.feheren_fekete.applist.applistpage

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.feheren_fekete.applist.ApplistLog
import net.feheren_fekete.applist.R
import net.feheren_fekete.applist.applistpage.model.ApplistModel
import net.feheren_fekete.applist.applistpage.viewmodel.BaseItem
import net.feheren_fekete.applist.applistpage.viewmodel.SectionItem
import net.feheren_fekete.applist.applistpage.viewmodel.StartableItem
import net.feheren_fekete.applist.applistpage.viewmodel.ViewModelUtils
import net.feheren_fekete.applist.settings.SettingsUtils
import org.koin.java.KoinJavaComponent.get

class ApplistItemDragHandler(private val context: Context,
                             private val applistPagePageFragment: ApplistPagePageFragment,
                             private val touchOverlay: ViewGroup,
                             private val recyclerView: RecyclerView,
                             private val layoutManager: MyGridLayoutManager,
                             private val adapter: ApplistAdapter) : DragGestureRecognizer.Callback {

    private val settingsUtils = get(SettingsUtils::class.java)
    private val applistModel = get(ApplistModel::class.java)
    private var draggedView: View? = null
    private var draggedSectionView: View? = null
    private val draggedAppViewSize = context.resources.getDimensionPixelSize(R.dimen.appitem_icon_size)
    private val draggedSectionViewSize = context.resources.getDimensionPixelSize(R.dimen.dragged_section_item_width)
    private var draggedOverItem: BaseItem? = null
    private val recyclerViewLocation = IntArray(2)
    private val draggedOverViewLocation = IntArray(2)
    private val itemMoveThreshold = context.resources.getDimension(R.dimen.applist_fragment_item_move_threshold)

    override fun canDrag(gestureRecognizer: DragGestureRecognizer): Boolean {
        return !adapter.isFilteredByName && applistPagePageFragment.isItemMenuOpen()
    }

    override fun canStartDragging(gestureRecognizer: DragGestureRecognizer): Boolean {
        val event = gestureRecognizer.motionEvent
        val fingerDownPos = gestureRecognizer.fingerDownPos
        val a = event!!.rawX - fingerDownPos.x
        val b = event.rawY - fingerDownPos.y
        val distance = Math.sqrt((a * a + b * b).toDouble())
        return distance > itemMoveThreshold
    }

    override fun onStartDragging(gestureRecognizer: DragGestureRecognizer) {
        applistPagePageFragment.closeItemMenu()

        val draggedItem = applistPagePageFragment.getItemMenuTarget()
        if (draggedItem is StartableItem) {
            ApplistLog.getInstance().analytics(ApplistLog.START_DRAG_APP, ApplistLog.APPLIST)
            adapter.setEnabled(draggedItem, false)
        } else if (draggedItem is SectionItem) {
            ApplistLog.getInstance().analytics(ApplistLog.START_DRAG_SECTION, ApplistLog.APPLIST)
            adapter.setAllStartablesEnabled(false)
            adapter.setSectionsHighlighted(true)
        }

        addDraggedView(gestureRecognizer, draggedItem)
    }

    override fun onDragging(gestureRecognizer: DragGestureRecognizer) {
        if (draggedView != null) {
            val draggedItem = applistPagePageFragment.getItemMenuTarget()
            updateDraggedViewPosition(gestureRecognizer, draggedItem)
            updateDraggedOverItem(gestureRecognizer, draggedItem)
            scrollWhileDragging(gestureRecognizer)
        }
    }

    override fun onDrop(gestureRecognizer: DragGestureRecognizer) {
        if (draggedOverItem != null) {
            var canDrop = true
            val draggedItem = applistPagePageFragment.getItemMenuTarget()
            if (draggedItem === draggedOverItem) {
                canDrop = false
            }
            if (draggedItem is SectionItem) {
                if (draggedOverItem is SectionItem) {
                    val draggedOverItemPos = adapter.getItemPosition(draggedOverItem)
                    if (draggedOverItemPos == adapter.getNextSectionPosition(draggedItem)) {
                        canDrop = false
                    }
                }
            }

            if (canDrop) {
                val fromPosition = adapter.getItemPosition(draggedItem)
                var toPosition = adapter.getItemPosition(draggedOverItem)
                if (fromPosition < toPosition && draggedOverItem!!.isDraggedOverLeft) {
                    --toPosition
                }
                if (toPosition < fromPosition && draggedOverItem!!.isDraggedOverRight) {
                    ++toPosition
                }
                adapter.moveItem(fromPosition, toPosition)
                savePageToModel()
            }
        }
    }

    override fun onStopDragging(gestureRecognizer: DragGestureRecognizer) {
        val draggedItem = applistPagePageFragment.getItemMenuTarget()
        if (draggedItem is StartableItem) {
            adapter.setEnabled(draggedItem, true)
        } else if (draggedItem is SectionItem) {
            adapter.setAllStartablesEnabled(true)
            adapter.setSectionsHighlighted(false)
        }

        if (draggedOverItem != null) {
            draggedOverItem!!.setDraggedOver(BaseItem.NONE)
            adapter.notifyItemChanged(adapter.getItemPosition(draggedOverItem))
        }

        removeDraggedView()
    }

    private fun addDraggedView(gestureRecognizer: DragGestureRecognizer, draggedItem: BaseItem) {
        if (draggedItem is StartableItem) {
            val startableItemHolder = recyclerView.findViewHolderForItemId(draggedItem.getId()) as StartableItemHolder
                    ?: return
            val imageView = ImageView(context)
            imageView.setImageDrawable(startableItemHolder.appIcon.drawable)
            val layoutParams = FrameLayout.LayoutParams(draggedAppViewSize, draggedAppViewSize)
            imageView.layoutParams = layoutParams
            draggedView = imageView
        } else {
            if (draggedSectionView == null) {
                val inflater = LayoutInflater.from(context)
                draggedSectionView = inflater.inflate(R.layout.dragged_section_item, null, false)
                val layoutParams = FrameLayout.LayoutParams(
                        draggedSectionViewSize, ViewGroup.LayoutParams.WRAP_CONTENT)
                draggedSectionView!!.layoutParams = layoutParams
            }
            val sectionItem = draggedItem as SectionItem
            val textView = draggedSectionView!!.findViewById<View>(R.id.dragged_section_item_name) as TextView
            textView.text = sectionItem.name
            draggedView = draggedSectionView
        }
        updateDraggedViewPosition(gestureRecognizer, draggedItem)
        touchOverlay.addView(draggedView)
    }

    private fun updateDraggedViewPosition(gestureRecognizer: DragGestureRecognizer, draggedItem: BaseItem) {
        val event = gestureRecognizer.motionEvent
        val fingerRawX = event!!.rawX
        val fingerRawY = event.rawY
        val layoutParams = draggedView!!.layoutParams as FrameLayout.LayoutParams
        recyclerView.getLocationOnScreen(recyclerViewLocation)
        layoutParams.leftMargin = Math.round(fingerRawX - recyclerViewLocation[0])
        layoutParams.topMargin = Math.round(fingerRawY - recyclerViewLocation[1])
        if (draggedItem is StartableItem) {
            layoutParams.leftMargin -= draggedAppViewSize / 2
            layoutParams.topMargin -= (draggedAppViewSize * 1.5f).toInt()
        } else {
            layoutParams.leftMargin -= draggedSectionViewSize / 2
            layoutParams.topMargin -= draggedSectionViewSize
        }
        draggedView!!.layoutParams = layoutParams
    }

    private fun removeDraggedView() {
        touchOverlay.removeView(draggedView)
        draggedView = null
    }

    private fun updateDraggedOverItem(gestureRecognizer: DragGestureRecognizer, draggedItem: BaseItem) {
        if (adapter.itemCount == 0) {
            return
        }

        val event = gestureRecognizer.motionEvent
        val fingerCurrentPosX = event!!.rawX
        val fingerCurrentPosY = event.rawY
        val firstItemPos = layoutManager.findFirstVisibleItemPosition()
        val lastItemPos = layoutManager.findLastVisibleItemPosition()

        if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {

            var candidateItemPosition = RecyclerView.NO_POSITION
            var candidateItem: BaseItem? = null
            var distanceToCandidateItem = java.lang.Double.MAX_VALUE
            var candidateViewLeft = 0
            var candidateViewTop = 0
            var candidateViewRight = 0
            var candidateViewBottom = 0
            for (i in firstItemPos..lastItemPos) {
                var considerItem = true
                val item = adapter.getItem(i)
                if (draggedItem is StartableItem && item is SectionItem) {
                    if (!item.isCollapsed && !adapter.isSectionEmpty(item)) {
                        // We don't allow dragging over open and not empty (i.e. normal) section
                        // headers.
                        considerItem = false
                    }
                }
                if (draggedItem is SectionItem && item is StartableItem) {
                    // We don't allow dragging sections over app items, unless it's the very last
                    // app item.
                    considerItem = i == adapter.itemCount - 1
                }
                if (considerItem) {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as ViewHolderBase?
                    if (viewHolder != null) {
                        viewHolder.layout.getLocationOnScreen(draggedOverViewLocation)
                        val viewLeft = draggedOverViewLocation[0]
                        val viewTop = draggedOverViewLocation[1]
                        val viewRight = draggedOverViewLocation[0] + viewHolder.layout.width
                        val viewBottom = draggedOverViewLocation[1] + viewHolder.layout.height
                        val viewCenterX = viewLeft + viewHolder.layout.width / 2.0f
                        val viewCenterY = viewTop + viewHolder.layout.height / 2.0f
                        val distanceToView = distanceOfPoints(
                                viewCenterX, viewCenterY, fingerCurrentPosX, fingerCurrentPosY)
                        if (distanceToView < distanceToCandidateItem) {
                            candidateItemPosition = i
                            candidateItem = adapter.getItem(i)
                            distanceToCandidateItem = distanceToView
                            candidateViewLeft = viewLeft
                            candidateViewRight = viewRight
                            candidateViewTop = viewTop
                            candidateViewBottom = viewBottom
                        }
                    }
                }
            }

            if (candidateItem == null) {
                cleanDraggedOverItem()
            } else {
                cleanDraggedOverItem()
                draggedOverItem = candidateItem
                if (draggedItem is StartableItem) {
                    if (draggedOverItem is StartableItem) {
                        if (adapter.isStartableLastInSection(draggedOverItem as StartableItem?)) {
                            val viewLeftSideCenterX = candidateViewLeft
                            val viewLeftSideCenterY = candidateViewTop + (candidateViewBottom - candidateViewTop) / 2
                            val distanceToLeftSideCenter = distanceOfPoints(
                                    viewLeftSideCenterX.toFloat(), viewLeftSideCenterY.toFloat(), fingerCurrentPosX, fingerCurrentPosY)
                            val viewRightSideCenterX = candidateViewRight
                            val viewRightSideCenterY = candidateViewTop + (candidateViewBottom - candidateViewTop) / 2
                            val distanceToRightSideCenter = distanceOfPoints(
                                    viewRightSideCenterX.toFloat(), viewRightSideCenterY.toFloat(), fingerCurrentPosX, fingerCurrentPosY)
                            draggedOverItem!!.setDraggedOver(
                                    if (distanceToLeftSideCenter < distanceToRightSideCenter) BaseItem.LEFT else BaseItem.RIGHT)
                        } else {
                            draggedOverItem!!.setDraggedOver(BaseItem.LEFT)
                        }
                        adapter.notifyItemChanged(candidateItemPosition)
                    } else if (draggedOverItem is SectionItem) {
                        draggedOverItem!!.setDraggedOver(BaseItem.RIGHT)
                        adapter.notifyItemChanged(candidateItemPosition)
                    }
                } else if (draggedItem is SectionItem) {
                    if (draggedOverItem is StartableItem) {
                        draggedOverItem!!.setDraggedOver(BaseItem.RIGHT)
                        adapter.notifyItemChanged(candidateItemPosition)
                    } else if (draggedOverItem is SectionItem) {
                        draggedOverItem!!.setDraggedOver(BaseItem.LEFT)
                        adapter.notifyItemChanged(candidateItemPosition)
                    }
                }
            }
        } else {
            cleanDraggedOverItem()
        }
    }

    private fun cleanDraggedOverItem() {
        if (draggedOverItem != null) {
            draggedOverItem!!.setDraggedOver(BaseItem.NONE)
            adapter.notifyItemChanged(adapter.getItemPosition(draggedOverItem))
            draggedOverItem = null
        }
    }

    private fun distanceOfPoints(p1x: Float, p1y: Float, p2x: Float, p2y: Float): Double {
        val a = p1x - p2x
        val b = p1y - p2y
        return Math.sqrt((a * a + b * b).toDouble())
    }

    private fun scrollWhileDragging(gestureRecognizer: DragGestureRecognizer) {
        val event = gestureRecognizer.motionEvent
        val fingerCurrentPosY = event!!.rawY

        val fingerDownPos = gestureRecognizer.fingerDownPos
        val direction = if (Math.signum(fingerCurrentPosY - fingerDownPos.y) > 0) 1 else -1
        if (direction == -1) {
            val firstItemPos = layoutManager.findFirstCompletelyVisibleItemPosition()
            if (firstItemPos != 0) {
                recyclerView.getLocationOnScreen(recyclerViewLocation)
                val recyclerViewTop = recyclerViewLocation[1]
                val scrollLimit = (fingerCurrentPosY - recyclerViewTop) / recyclerView.height
                if (scrollLimit < 0.20f) {
                    recyclerView.smoothScrollToPosition(0)
                } else {
                    recyclerView.stopScroll()
                }
            }
        } else {
            val lastItemPos = layoutManager.findLastCompletelyVisibleItemPosition()
            if (lastItemPos != adapter.itemCount - 1) {
                recyclerView.getLocationOnScreen(recyclerViewLocation)
                val recyclerViewBottom = recyclerViewLocation[1] + recyclerView.height
                val scrollLimit = (recyclerViewBottom - fingerCurrentPosY) / recyclerView.height
                if (scrollLimit < 0.20f) {
                    recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
                } else {
                    recyclerView.stopScroll()
                }
            }
        }
    }

    private fun savePageToModel() {
        val (id, name) = applistPagePageFragment.getPageItem()
        val items = adapter.allItems
        val keepAppsSorted = settingsUtils.isKeepAppsSortedAlphabetically
        GlobalScope.launch{
            val pageData = ViewModelUtils.viewToModel(id, name, items)
            applistModel.setPage(id, pageData)
            if (keepAppsSorted) {
                applistModel.sortStartablesInPage(id)
            }
        }
    }

}


