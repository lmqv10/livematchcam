package it.lmqv.livematchcam.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.XmlRes
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import it.lmqv.livematchcam.databinding.DialogFragmentBinding


class PreferencesDialogFragment : DialogFragment() {

    private var _binding: DialogFragmentBinding? = null
    val binding get() = _binding!!

    companion object {
        private const val ARG_PREFERENCE_XML = "preference_xml"
        private const val ARG_PREFERENCE_ROOT_KEY = "preference_root_key"

        fun newInstance(
            @XmlRes preferenceXml: Int,
            rootKey: String? = null
        ): PreferencesDialogFragment {
            return PreferencesDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PREFERENCE_XML, preferenceXml)
                    putString(ARG_PREFERENCE_ROOT_KEY, rootKey)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferenceXml = arguments?.getInt(ARG_PREFERENCE_XML) ?: return
        val rootKey = arguments?.getString(ARG_PREFERENCE_ROOT_KEY)

        val preferenceFragment = GenericPreferenceFragment.newInstance(preferenceXml, rootKey)

        childFragmentManager
            .beginTransaction()
            .replace(binding.container.id, preferenceFragment)
            .commit()

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.50).toInt(),
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class GenericPreferenceFragment : PreferenceFragmentCompat() {

        companion object {
            private const val ARG_XML_RES = "xml_res"
            private const val ARG_ROOT_KEY = "root_key"

            fun newInstance(@XmlRes xmlRes: Int, rootKey: String? = null): GenericPreferenceFragment {
                return GenericPreferenceFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_XML_RES, xmlRes)
                        putString(ARG_ROOT_KEY, rootKey)
                    }
                }
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val xmlRes = arguments?.getInt(ARG_XML_RES) ?: return
            val key = arguments?.getString(ARG_ROOT_KEY) ?: rootKey

            setPreferencesFromResource(xmlRes, key)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference is PreferenceScreen) {
                val xmlRes = arguments?.getInt(ARG_XML_RES) ?: return false
                val fragment = newInstance(xmlRes, preference.key)

                (parentFragment as? PreferencesDialogFragment)?.let { dialogFragment ->
                    dialogFragment.childFragmentManager.beginTransaction()
                        .replace(dialogFragment.binding.container.id, fragment)
                        .addToBackStack(null)
                        .commit()
                }

                return true
            }
            return super.onPreferenceTreeClick(preference)
        }
    }
}