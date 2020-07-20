package com.example.colorize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.colorize.R
import com.example.colorize.imageInterface.EditImageFragmentListener
import com.example.colorize.imageInterface.FilterListFragmentListener
import com.zomato.photofilters.utils.ThumbnailItem
import kotlinx.android.synthetic.main.thumbnail_list_item.view.*

class ThumbnailAdapter(private val context: Context,
                       private val thumbnailItemList: List<ThumbnailItem>,
                       private val listener: FilterListFragmentListener): RecyclerView.Adapter<ThumbnailAdapter.MyViewHolder>(){


    private var selectedIndex = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val itemView = LayoutInflater.from(context).inflate(R.layout.thumbnail_list_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return thumbnailItemList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val thumbnailItem = thumbnailItemList[position]
        holder.thumbnail.setImageBitmap(thumbnailItem.image)
        holder.thumbnail.setOnClickListener {

            listener.onFilterSelected(thumbnailItem.filter)
            selectedIndex = position
            notifyDataSetChanged()
        }

        holder.filterName.text = thumbnailItem.filterName

        if (selectedIndex == position) {

            holder.filterName.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
        } else {
            holder.filterName.setTextColor(ContextCompat.getColor(context, R.color.white))

        }
    }


    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        var thumbnail: ImageView
        var filterName: TextView

        init {
            thumbnail = itemView.thumbnail
            filterName = itemView.filter_name
        }

    }
}