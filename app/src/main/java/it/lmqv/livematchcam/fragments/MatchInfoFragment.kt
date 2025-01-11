package it.lmqv.livematchcam.fragments

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSportsBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.factories.Sports
import it.lmqv.livematchcam.factories.SportsFactory
import it.lmqv.livematchcam.viewmodels.MatchViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SportsFragment : Fragment() {

    companion object {
        fun newInstance() = SportsFragment()
    }

    private val youtubeViewModel: YoutubeViewModel by activityViewModels()
    //private val googleViewModel: GoogleViewModel by activityViewModels()
    //private val streamersViewModel: StreamersViewModel by activityViewModels()

    //private val homeTeamViewModel: HomeScoreBoardViewModel by activityViewModels()
    //private val awayTeamViewModel: AwayScoreBoardViewModel by activityViewModels()
    private val matchViewModel: MatchViewModel by viewModels()

    //private val firebaseDataManager = FirebaseDataManager.getInstance()

    private val cardItems = listOf(
        CardItem(sport = Sports.SOCCER, description = "Soccer", icon = R.drawable.sport_soccer),
        CardItem(sport = Sports.VOLLEY, description = "Volley", icon = R.drawable.sport_volley)
    )

    private var sportsFactory = SportsFactory

    private var _binding: FragmentSportsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ////Logd("SportsFragment::matchViewModelID: ${matchViewModel.instanceId}")
        /*homeTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
        }
        awayTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
        }

        homeTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            if (color != null) {
                binding.homeColor.setShirtByColor(color)
            }
        }
        awayTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            if (color != null) {
                binding.awayColor.setShirtByColor(color)
            }
        }*/

        matchViewModel.homeTeam.observe(viewLifecycleOwner) { homeTeam ->
            //Logd("SportsFragment::homeTeam: $homeTeam")
            binding.homeTeam.text = homeTeam
        }
        matchViewModel.guestTeam.observe(viewLifecycleOwner) { guestTeam ->
            //Logd("SportsFragment::guestTeam: $guestTeam")
            binding.guestTeam.text = guestTeam
        }
        matchViewModel.homeColorHex.observe(viewLifecycleOwner) { homeColorHex ->
            //Logd("SportsFragment::homeColorHex: $homeColorHex")
            binding.homeColor.setShirtByColor(Color.parseColor(homeColorHex))
        }
        matchViewModel.guestColorHex.observe(viewLifecycleOwner) { guestColorHex ->
            //Logd("SportsFragment::homeColorHex: $guestColorHex")
            binding.guestColor.setShirtByColor(Color.parseColor(guestColorHex))
        }

        /*matchViewModel.match.observe(viewLifecycleOwner) { match ->
            binding.homeTeam.text = match.homeTeam
            binding.homeTeam.text = match.homeTeam
            binding.awayTeam.text = match.guestTeam
            binding.homeColor.setShirtByColor(Color.parseColor(match.homeColorHex))
            binding.awayColor.setShirtByColor(Color.parseColor(match.guestColorHex))
        }*/

        binding.homeColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                matchViewModel.setHomeColorHex(color)
            }
        }

        binding.guestColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                matchViewModel.setGuestColorHex(color)
            }
        }

        binding.homeTeam.setOnClickListener {
            val teamName = binding.homeTeam.text.toString()

            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
                matchViewModel.setHomeTeam(updatedTeamName)
                requireActivity().hideSystemUI()
            }
        }

        binding.guestTeam.setOnClickListener {
            val teamName = binding.guestTeam.text.toString()

            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
                matchViewModel.setGuestTeam(updatedTeamName)
                requireActivity().hideSystemUI()
            }
        }

        launchOnStarted {
            youtubeViewModel.sport.collectLatest {
                sportsFactory.set(it)
                cardItems.forEach { item -> item.isSelected = item.sport == it }

                val adapter = CardAdapter(cardItems) { selectedItem ->
                    lifecycleScope.launch {
                        youtubeViewModel.setSport(selectedItem.sport)
                    }
                }
                binding.sportsGrid.adapter = adapter
            }
        }

        /*
        launchOnStarted {
            combine(
                googleViewModel.account,
                googleViewModel.firebaseAccountKey,
                streamersViewModel.currentKey
            ) { account, firebaseAccountKey, currentKey -> Triple(account, firebaseAccountKey, currentKey) }
            .distinctUntilChanged()
            .collect { (account, firebaseAccountKey, currentKey) ->
                val accountName = account?.name
                Logd("SportFragment: $accountName $firebaseAccountKey $currentKey")
                if (!accountName.isNullOrEmpty() &&
                    !firebaseAccountKey.isNullOrEmpty() &&
                    !currentKey.isNullOrEmpty()) {
                    //Logd("MATCH INITIALIZE $firebaseAccountKey $currentKey")
                    //matchViewModel.initialize(firebaseAccountKey)

                    /-*
                    firebaseDataManager
                        .initialize(firebaseAccountKey)
                        .authenticateAccount(accountName, { account ->
                            //Logd("RealtimeDB Account Name: ${account.name}")
                            //Logd("RealtimeDB Admin: ${account.admin}")
                            var selectedMatch = account.matches.firstNotNullOf {
                                if (currentKey == it.key) it.value else Match()
                            }
                            //homeTeamViewModel.setName(selectedMatch.homeTeam)
                            //awayTeamViewModel.setName(selectedMatch.guestTeam)

                            //homeTeamViewModel.setLogo(Color.parseColor(selectedMatch.homeColorHex))
                            //awayTeamViewModel.setLogo(Color.parseColor(selectedMatch.guestColorHex))
                        },{
                            //toast("Failed to fetch account.")
                        })
                     *-/
                }
            }
        }*/
    }

    /*override fun onPause() {
        super.onPause()
        //Logd("MatchInfo::OnPause")
        //matchViewModel.detach()
    }

    override fun onResume() {
        super.onResume()
        //Logd("MatchInfo::OnResume")
    }
    */

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        //Logd("MatchInfo::onDestroyView")
    }
}

data class CardItem(
    val sport: Sports,
    val description: String,
    var isSelected: Boolean = false,
    @DrawableRes val icon: Int
)

class CardAdapter(
    private val items: List<CardItem>,
    private val onItemSelected: (CardItem) -> Unit
) : BaseAdapter() {

    private var selectedPosition: Int = -1

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = items[position].sport.ordinal.toLong()

    init {
        val selectedCardItem = items.firstOrNull { x -> x.isSelected }
        selectedPosition = if (selectedCardItem != null) items.indexOf(selectedCardItem) else -1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(parent?.context).inflate(R.layout.card_sport_item, parent, false)

        val cardItem = items[position]
        val imageView = view.findViewById<ImageView>(R.id.sport_icon)
        val cardView = view.findViewById<CardView>(R.id.sport_card)

        imageView.setImageResource(cardItem.icon)
        view.isSelected = position == selectedPosition

        if (view.isSelected) {
            parent?.context?.let {
                cardView.setCardBackgroundColor(ContextCompat.getColor(it, R.color.secondary))
                imageView.setColorFilter(Color.WHITE)
            }
        } else {
            cardView.setCardBackgroundColor(Color.WHITE)
            imageView.clearColorFilter()
        }

        view.setOnClickListener { _ ->
            if (selectedPosition != position) {
                if (selectedPosition >= 0) {
                    items[selectedPosition].isSelected = false
                }
                selectedPosition = position
                cardItem.isSelected = true
                notifyDataSetChanged()
                onItemSelected(cardItem)
            }
        }
        return view
    }
}
