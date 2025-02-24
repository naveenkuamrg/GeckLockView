package com.thirdparty.webviewdemo

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.ContentBlocking.AntiTracking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import androidx.core.net.toUri
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    private lateinit var geckoView: GeckoView
    private val session = GeckoSession()
    private var sRuntime: GeckoRuntime? = null
    private lateinit var urlEditText: EditText

    private lateinit var trackersCount: TextView
    private var trackersBlockedList: List<ContentBlocking.BlockEvent> = mutableListOf()


    companion object {
       val INITIAL_URL = "https://www.mozilla.org"
        val SEARCH_URI_BASE = "https://duckduckgo.com/?q="
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpView()
        setupToolbar()
        setupUrlEditText()
        setupDelegates()

    }

    override fun onDestroy() {
        super.onDestroy()
        session.close()
        sRuntime?.shutdown()
    }

    private fun setUpView() {
        geckoView = findViewById(R.id.geckoview)
        urlEditText = findViewById<EditText>(R.id.search_edit_text).apply {
            setText(INITIAL_URL)
        }
        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this)
            sRuntime
        }
        trackersCount = findViewById<TextView>(R.id.trackers_count)
        session.open(sRuntime!!)
        geckoView.setSession(session)
        session.loadUri(INITIAL_URL)
        session.settings.useTrackingProtection = true
        session.contentBlockingDelegate = createBlockingDelegate()
        trackersCount.setOnClickListener {
            setupTrackersCounter()

        }

    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
    }

    private fun setupUrlEditText() {

        urlEditText.setOnEditorActionListener(object : OnEditorActionListener {
            override fun onEditorAction(
                v: TextView?,
                actionId: Int,
                event: KeyEvent?
            ): Boolean {
                onCommit(v?.text.toString())
                return true
            }

        } )
    }


    fun onCommit(text: String) {
        if ((text.contains(".") ||
                    text.contains(":")) &&
            !text.contains(" ")) {
            session.loadUri(text)
        } else {
            session.loadUri(SEARCH_URI_BASE + text)
        }
        clearTrackersCount()
        geckoView.requestFocus()
    }

    private fun setupDelegates() {
        session.settings.useTrackingProtection = true
        session.contentBlockingDelegate = createBlockingDelegate()

    }
    private fun createBlockingDelegate(): ContentBlocking.Delegate {
        return object : ContentBlocking.Delegate {
            override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
                trackersBlockedList = trackersBlockedList + event
                trackersCount.text = "${trackersBlockedList.size}"
            }
        }
    }

    private fun ContentBlocking.BlockEvent.categoryToString(): String {
        val stringResource = when (antiTrackingCategory) {
            AntiTracking.NONE -> "none"
            AntiTracking.ANALYTIC -> "ANALYTIC"
            AntiTracking.AD -> "ADD"
            AntiTracking.TEST -> "TEST"
            AntiTracking.SOCIAL -> "SOCIAL"
            AntiTracking.CONTENT -> "CONTENT"
            else -> "none"
        }
        return stringResource
    }

    private fun getFriendlyTrackersUrls(): List<Spanned> {
        return trackersBlockedList.map { blockEvent ->
            val host = blockEvent.uri.toUri().host
            val category = blockEvent.categoryToString()
            Html.fromHtml(
                "<b><font color='#D55C7C'>[$category]</font></b> <br/> $host",
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        }
    }
    private fun setupTrackersCounter() {
        trackersCount = findViewById(R.id.trackers_count)
        trackersCount.setOnClickListener {
            if (trackersBlockedList.isNotEmpty()) {
                val trackerUrls = getFriendlyTrackersUrls() // List<Spanned>
                val adapter = SpannedArrayAdapter(this, android.R.layout.simple_list_item_1, trackerUrls)

                AlertDialog.Builder(this)
                    .setTitle("Blocked Trackers")
                    .setAdapter(adapter) { dialog, which ->
//                        session.loadUri(trackersBlockedList[which].uri.toString())
//                        urlEditText.setText(trackersBlockedList[which].uri.toString())
                    }
                    .setNegativeButton("Close") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun clearTrackersCount() {
        trackersBlockedList = emptyList()
        trackersCount.text = "0"
    }



}

class SpannedArrayAdapter(context: Context, resource: Int, objects: List<Spanned>) :
    ArrayAdapter<Spanned>(context, resource, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = getItem(position)
        return view
    }
}