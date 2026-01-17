package it.lmqv.livematchcam.fragments.sports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.adapters.CardAdapter
import it.lmqv.livematchcam.databinding.FragmentSportInfoBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.factories.sports.SportsFactory
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SportInfoFragment : Fragment() {

    companion object {
        fun newInstance() = SportInfoFragment()
    }

    private var _binding: FragmentSportInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var sportCollectJob : Job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var cardItems = SportsFactory.getSports()

        this.sportCollectJob = viewLifecycleOwner.lifecycleScope.launch {
            MatchRepository.sport.collectLatest { sport ->
                try {
                    //Logd("SportInfoFragment::sport.collectLatest:: ${sport}")
                    cardItems.forEach { item -> item.isSelected = item.sport == sport }

                    val adapter = CardAdapter(cardItems) { selectedItem ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            Logd("SportInfoFragment::selectedItem:: ${selectedItem.sport}")
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