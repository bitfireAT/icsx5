package at.bitfire.icsdroid.ui

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * A [RecyclerView.ViewHolder] that is aware of its [LifecycleOwner]. Also adapts directly the given [ViewBinding].
 * @since 20221202
 * @param binding The [ViewBinding] to give to the ViewHolder.
 */
abstract class LifecycleViewHolder <B: ViewBinding> (binding: B): RecyclerView.ViewHolder(binding.root) {
    val lifecycleOwner by lazy {
        binding.root.context as? LifecycleOwner
    }
}
