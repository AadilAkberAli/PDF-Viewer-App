package com.example.pdfviewer

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class CustomFileAdapter(private val context: Context, arrayList: ArrayList<File>) : BaseAdapter(), Filterable{
    private lateinit var name: TextView
    private lateinit var fileImage: ImageView
    private var filterArrayList: ArrayList<File>
    private var allItems : ArrayList<File>
    init {
        filterArrayList = arrayList
        allItems = arrayList
    }
    override fun getCount(): Int {
        return filterArrayList.size
    }
    override fun getItem(position: Int): Any {
        return position
    }

    fun getItemAtParticularPosition(position: Int): File {
        return filterArrayList.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView = convertView
        convertView = LayoutInflater.from(context).inflate(R.layout.filename_list, parent, false)
        name = convertView.findViewById(R.id.fileName)
        fileImage = convertView.findViewById(R.id.fileImage)
        name.text = filterArrayList[position].name
        if(name.text.endsWith(".pdf"))
        {
            fileImage.setImageResource(R.drawable.pdf)
        }
        else
        {
            fileImage.setImageResource(R.drawable.folder)
        }
        return convertView
    }

    fun filterList(query: String)
    {
        filter.filter(query)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                filterArrayList = allItems
                val query = constraint?.toString()?.toLowerCase()
                val filteredItems = if (query.isNullOrEmpty()) {
                    ArrayList(filterArrayList.toList())
                } else {
                    filterArrayList.filter { it.name.toLowerCase().contains(query) }
                }

                results.values = filteredItems
                results.count = filteredItems.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                Log.e("Result values", results?.values.toString())
                filterArrayList = results?.values as ArrayList<File>
                notifyDataSetChanged()
            }
        }
    }
}