package io.ta.waktushalat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TiFragment : Fragment() {

    private var columnCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val dt = arguments?.get("data") as ArrayList<String>?

        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter =
                    dt?.let {
                        MyTiRecyclerViewAdapter(
                            arrayOf(
                                getString(R.string.fajr),
                                getString(R.string.sunrise),
                                getString(R.string.dhuhr),
                                getString(R.string.asr),
                                getString(R.string.maghrib),
                                getString(R.string.isha)
                            ),
                            it
                        )
                    }
            }
        }
        return view
    }

    companion object {
        const val ARG_COLUMN_COUNT = "column-count"
    }
}