package uk.co.seanhodges.incandescent.client.selection

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.Observer

import uk.co.seanhodges.incandescent.client.R
import uk.co.seanhodges.incandescent.client.storage.SceneWithActions

class SceneFragment : Fragment() {

    companion object {
        fun newInstance() = SceneFragment()
    }

    private lateinit var viewModel: SceneViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scenes, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(SceneViewModel::class.java)
        val sceneList: LinearLayout? = view?.findViewById(R.id.sceneList)

        viewModel.getAllScenes().observe(this, Observer<List<SceneWithActions>> { scenesWithActions ->
            for (sceneWithActions in scenesWithActions) {
                val item = ListEntry.Builder(sceneList!!)
                        .title(sceneWithActions.scene?.title ?: "")
                        .type("scene")
                        .build()
                sceneList.addView(item)
            }

            //TODO: include the [Add] button
        })


    }

}
