package moe.shizuku.manager.home

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.HomeDataObbBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.file.DataObbExplorerActivity
import moe.shizuku.manager.model.ServiceStatus
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class DataObbViewHolder(private val binding: HomeDataObbBinding, private val root: View) :
    BaseViewHolder<ServiceStatus>(root),
    View.OnClickListener {

    companion object {
        val CREATOR = Creator<ServiceStatus> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HomeDataObbBinding.inflate(inflater, outer.root, true)
            DataObbViewHolder(inner, outer.root)
        }
    }

    init {
        root.setOnClickListener(this)
    }

    private inline val summary get() = binding.text2

    override fun onBind() {
        val context = itemView.context
        binding.text1.text = context.getString(R.string.home_data_obb_title)
        if (!data.isRunning) {
            root.isEnabled = false
            root.alpha = 0.65f
            summary.text =
                context.getString(R.string.home_status_service_not_running, context.getString(R.string.app_name))
        } else {
            root.isEnabled = true
            root.alpha = 1.0f
            summary.text = context.getString(R.string.home_data_obb_description)
        }
    }

    override fun onClick(v: View) {
        v.context.startActivity(Intent(v.context, DataObbExplorerActivity::class.java))
    }
}
