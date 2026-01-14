package it.lmqv.livematchcam.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.dialogs.LogosRecentsDialog
import it.lmqv.livematchcam.databinding.ViewTeamControlBinding
import it.lmqv.livematchcam.extensions.setFillAndBorder
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.factories.Sports

class TeamControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var _binding: ViewTeamControlBinding? = null
    private val binding get() = _binding!!

    private lateinit var sport: Sports

    var onPrimaryColorsChanged: ((Int) -> Unit)? = null
    var onSecondaryColorsChanged: ((Int) -> Unit)? = null
    var onLogoURLChanged: ((String) -> Unit)? = null
    //var onTeamNameChanged: ((String) -> Unit)? = null
    var onEditTeamName: ((String, Sports) -> Unit)? = null

    init {
        orientation = VERTICAL
        _binding = ViewTeamControlBinding.inflate(LayoutInflater.from(context), this, true)

        context.theme
            .obtainStyledAttributes(attrs, R.styleable.TeamControlView, 0, 0)
            .apply {
                try {
                    binding.textTeamName.setTextColor(getColor(R.styleable.TeamControlView_textColor, Color.BLACK))
//                    binding.textTeamName.text = getString(R.styleable.TeamControlView_teamName) ?: ""
//                    binding.imageLogo.tag = getString(R.styleable.TeamControlView_logoURL) ?: ""
//                    binding.primaryColor.setFillAndBorder(getColor(R.styleable.TeamControlView_primaryColor, Color.BLACK))
//                    binding.secondaryColor.setFillAndBorder(getColor(R.styleable.TeamControlView_secondaryColor, Color.BLACK))
                } finally {
                    recycle()
                }
            }

        binding.textTeamName.isSelected = true
        binding.textTeamName.setOnClickListener {
            val sourceTeamName = binding.textTeamName.text.toString()
            onEditTeamName?.invoke(sourceTeamName, sport)
        }

        binding.imageLogo.setOnClickListener {
            val sourceLogoUrl = binding.imageLogo.tag.toString()
            var dialog = LogosRecentsDialog(context, sourceLogoUrl) { updatedLogoUrl ->
                onLogoURLChanged?.invoke(updatedLogoUrl)
            }
            dialog.show()
        }

        binding.primaryColor.setOnClickListener {
            context.showColorPickerDialog { color ->
                onPrimaryColorsChanged?.invoke(color)
                binding.primaryColor.setFillAndBorder(color)
            }
        }
        binding.secondaryColor.setOnClickListener {
            context.showColorPickerDialog { color ->
                onSecondaryColorsChanged?.invoke(color)
                binding.secondaryColor.setFillAndBorder(color)
            }
        }
    }

    fun setLogoUrl(sourceLogoUrl: String) {
        if (binding.imageLogo.tag != sourceLogoUrl) {
            binding.imageLogo.tag = sourceLogoUrl

            if (sourceLogoUrl.isEmpty()) {
                binding.imageLogo.setImageResource(R.drawable.shield_add)
            } else {
                binding.imageLogo.load(sourceLogoUrl) {
                    placeholder(R.drawable.refresh)
                    error(R.drawable.shield_add)
                    allowHardware(false)
                }
            }
        }
    }

    fun setTeamName(name: String, sport: Sports) {
        this.sport = sport
        if (binding.textTeamName.text != name) {
            binding.textTeamName.text = name
        }
    }

    fun setPrimaryColor(color: Int) {
        binding.primaryColor.setFillAndBorder(fillColor = color)
    }

    /*fun setSecondaryColor(color: Int) {
        binding.secondaryColor.setFillAndBorder(fillColor = color)
    }*/
}
