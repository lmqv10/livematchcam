package it.lmqv.livematchcam.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.RemoteScoreActivity
import it.lmqv.livematchcam.YouTubeActivity
import it.lmqv.livematchcam.databinding.FragmentMatchInfoBinding
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.factories.Sports
import it.lmqv.livematchcam.viewmodels.GoogleViewModel
import it.lmqv.livematchcam.viewmodels.MatchViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MatchInfoFragment : Fragment() {

    companion object {
        fun newInstance() = MatchInfoFragment()
    }

    private val googleViewModel: GoogleViewModel by viewModels()
    private val matchViewModel: MatchViewModel by viewModels()

    private val cardItems = listOf(
        CardItem(sport = Sports.SOCCER, description = "Soccer", icon = R.drawable.sport_soccer),
        CardItem(sport = Sports.VOLLEY, description = "Volley", icon = R.drawable.sport_volley)
    )

    private var _binding: FragmentMatchInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Logd("SportsFragment::matchViewModelID: ${matchViewModel.instanceId}")

        matchViewModel.isRealtimeDatabaseAvailable.observe(viewLifecycleOwner) { isAvailable ->
            if (isAvailable) {
                binding.matchInfo.visibility = View.VISIBLE
            } else  {
                binding.matchInfo.visibility = View.GONE
            }
        }

        matchViewModel.homeTeam.observe(viewLifecycleOwner) { homeTeam ->
            binding.homeTeam.text = homeTeam
        }
        matchViewModel.guestTeam.observe(viewLifecycleOwner) { guestTeam ->
            binding.guestTeam.text = guestTeam
        }

        /*matchViewModel.homeColorHex.observe(viewLifecycleOwner) { homeColorHex ->
            binding.homeColor.setShirtByColor(Color.parseColor(homeColorHex))
        }
        matchViewModel.guestColorHex.observe(viewLifecycleOwner) { guestColorHex ->
            binding.guestColor.setShirtByColor(Color.parseColor(guestColorHex))
        }*/
        launchOnStarted {
            combine(
                matchViewModel.homeColorHex,
                matchViewModel.homeLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                //binding.homeColor.isClickable = logoURL.isNullOrEmpty()
                if (!logoURL.isNullOrEmpty()) {
                    binding.homeColor.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                    }
                } else {
                    binding.homeColor.setShirtByColor(Color.parseColor(colorHex))
                }
            }
        }

        launchOnStarted {
            combine(
                matchViewModel.guestColorHex,
                matchViewModel.guestLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                //binding.guestColor.isClickable = logoURL.isNullOrEmpty()
                if (!logoURL.isNullOrEmpty()) {
                    binding.guestColor.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                    }
                } else {
                    binding.guestColor.setShirtByColor(Color.parseColor(colorHex))
                }
            }
        }


        matchViewModel.type.observe(viewLifecycleOwner) { type ->
            cardItems.forEach { item -> item.isSelected = item.sport.name == type }

            val adapter = CardAdapter(cardItems) { selectedItem ->
                lifecycleScope.launch {
                    matchViewModel.setType(selectedItem.sport.name)
                }
            }
            binding.sportsGrid.adapter = adapter
        }

        /*lifecycleScope.launch {
            combine(
                googleViewModel.account,
                googleViewModel.firebaseAccountKey)
            { account, accountKey -> Pair(account, accountKey) }
                .collect { (account, accountKey) ->
                    val isLogged = account != null;
                    var accountName = account?.name
                    val isConnected = !accountName.isNullOrEmpty() && !accountKey.isNullOrEmpty()
                }
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

        binding.remoteScore.setOnClickListener {
            startActivity(Intent(requireActivity(), RemoteScoreActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
