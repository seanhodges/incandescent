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

        viewModel.getAllScenes().observe(this, Observer<List<Scene>> { roomsWithDevices ->
            val item = ListEntry.Builder(sceneList!!)
                    .title("Test")
                    .type("")
                    .build()
            sceneList.addView(item)
        })


    }

}
