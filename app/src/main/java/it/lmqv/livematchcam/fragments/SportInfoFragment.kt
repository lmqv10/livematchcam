package it.lmqv.livematchcam.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.adapters.CardAdapter
import it.lmqv.livematchcam.adapters.CardItem
import it.lmqv.livematchcam.databinding.FragmentSportInfoBinding
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.factories.Sports
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SportInfoFragment : Fragment() {

    companion object {
        fun newInstance() = SportInfoFragment()
    }

    private lateinit var sportCollectJob : Job

    private val cardItems = listOf(
        //CardItem(sport = Sports.SOCCER, description = R.string.sport_soccer, icon = R.drawable.sport_soccer),
        CardItem(sport = Sports.VOLLEY, description = R.string.sport_volley, icon = R.drawable.sport_volley)
    )

    private var _binding: FragmentSportInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.sportCollectJob = viewLifecycleOwner.lifecycleScope.launch {
            MatchRepository.sport.collectLatest { sport ->
                try {
                    //Logd("SportInfoFragment::sport.collectLatest:: ${sport}")
                    cardItems.forEach { item -> item.isSelected = item.sport == sport }

                    val adapter = CardAdapter(cardItems) { selectedItem ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            //Logd("SportInfoFragment::selectedItem:: ${selectedItem.sport}")
                            MatchRepository.setSport(selectedItem.sport)
                        }
                    }
                    binding.sportsGrid.adapter = adapter
                } catch (e: Exception) {
                    e.printStackTrace()
                    Loge("SportInfoFragment::Exception:: ${e.message.toString()}")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //Logd("SportInfoFragment::sportCollectJob::Cancel")
        this.sportCollectJob.cancel()
        _binding = null
    }
}
