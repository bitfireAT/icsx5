/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.databinding.TitleColorBinding
import at.bitfire.icsdroid.db.LocalCalendar

class TitleColorFragment: Fragment() {

    private val model by activityViewModels<TitleColorModel>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val binding = TitleColorBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = model

        binding.color.setOnClickListener {
            val intent = Intent(requireActivity(), ColorPickerActivity::class.java)
            model.color.value?.let {
                intent.putExtra(ColorPickerActivity.EXTRA_COLOR, it)
            }
            startActivityForResult(intent, 0)
        }
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        result?.let {
            model.color.value = it.getIntExtra(ColorPickerActivity.EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR)
        }
    }

    class TitleColorModel: ViewModel() {
        var url = MutableLiveData<String>()

        var originalTitle: String? = null
        val title = MutableLiveData<String>()

        var originalColor: Int? = null
        val color = MutableLiveData<Int>()

        fun dirty() =
                originalTitle != title.value ||
                originalColor != color.value
    }

}
