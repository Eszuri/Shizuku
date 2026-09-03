package moe.shizuku.manager.home

import rikka.recyclerview.IdBasedRecyclerViewAdapter
import rikka.recyclerview.IndexCreatorPool

class HomeAdapter(private val homeModel: HomeViewModel) :
    IdBasedRecyclerViewAdapter(ArrayList()) {

    init {
        updateData()
        setHasStableIds(true)
    }

    companion object {
        private const val ID_STATUS = 0L
        private const val ID_START_WADB = 1L
        private const val ID_DATA_OBB = 2L
    }

    override fun onCreateCreatorPool(): IndexCreatorPool {
        return IndexCreatorPool()
    }

    fun updateData() {
        val status = homeModel.serviceStatus.value?.data ?: return

        clear()
        addItem(ServerStatusViewHolder.CREATOR, status, ID_STATUS)
        addItem(StartWirelessAdbViewHolder.CREATOR, status, ID_START_WADB)
        addItem(DataObbViewHolder.CREATOR, status, ID_DATA_OBB)

        notifyDataSetChanged()
    }
}
