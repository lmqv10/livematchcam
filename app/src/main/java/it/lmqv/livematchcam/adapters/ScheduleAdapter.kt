package it.lmqv.livematchcam.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.SpinnerScheduleItemBinding
import it.lmqv.livematchcam.extensions.toFormattedDate
import it.lmqv.livematchcam.services.firebase.Schedule

class ScheduleAdapter(
    private val context: Context,
    private val schedules: List<Schedule>,
    private val onClick: (Schedule) -> Unit,
) : BaseAdapter() {

    override fun getCount(): Int = schedules.size

    override fun getItem(position: Int): Schedule = schedules[position]

    override fun getItemId(position: Int): Long = position.toLong()

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val binding: SpinnerScheduleItemBinding = if (convertView == null) {
            SpinnerScheduleItemBinding.inflate(LayoutInflater.from(context), parent, false)
        } else {
            SpinnerScheduleItemBinding.bind(convertView)
        }

        val item = getItem(position)

        if (item.homeLogo.isEmpty()) {
            binding.ivAwayLogo.setImageResource(R.drawable.shield_add)
        } else {
            binding.ivHomeLogo.load(item.homeLogo) {
                placeholder(R.drawable.refresh)
                error(R.drawable.shield_add)
                allowHardware(false)
            }
        }

        if (item.guestLogo.isEmpty()) {
            binding.ivAwayLogo.setImageResource(R.drawable.shield_add)
        } else {
            binding.ivAwayLogo.load(item.guestLogo) {
                placeholder(R.drawable.refresh)
                error(R.drawable.shield_add)
                allowHardware(false)
            }
        }

        binding.tvHomeTeamName.text = item.homeTeam
        binding.tvAwayTeamName.text = item.guestTeam

        binding.tvMatchInfo.text = item.matchDate.toFormattedDate()

        binding.root.setOnClickListener {
            onClick(item)
        }

        return binding.root
    }
}
