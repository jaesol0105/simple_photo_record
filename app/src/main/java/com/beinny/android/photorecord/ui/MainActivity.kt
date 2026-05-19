package com.beinny.android.photorecord.ui

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.beinny.android.photorecord.R
import com.beinny.android.photorecord.databinding.LayoutToolbarSelectionBinding
import com.beinny.android.photorecord.ui.common.applyCheckAnimation
import com.beinny.android.photorecord.ui.record.RecordFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), RecordFragment.Callbacks {
    private lateinit var toolbar: Toolbar
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration

    private var selectionBinding: LayoutToolbarSelectionBinding? = null
    private var isSelectAllChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.recordFragment, R.id.dataMgntFragment),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.fragment_container_view)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /** RecordFragment 롱클릭 콜백 - 툴바 선택 뷰 추가/제거 */
    override fun onLongClick(longclick: Boolean, count: Int, total: Int) {
        if (longclick) {
            if (selectionBinding == null) {
                val sb = LayoutToolbarSelectionBinding.inflate(layoutInflater, toolbar, false)
                selectionBinding = sb
                sb.ivSelectAllCheckmark.visibility = View.VISIBLE
                isSelectAllChecked = false
                sb.llSelectAll.setOnClickListener {
                    currentRecordFragment()?.adapterCallback?.run {
                        if (isSelectAllChecked) clearAll() else selectAll()
                    }
                }
                toolbar.addView(sb.root)
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            }
            val sb = selectionBinding!!
            val newChecked = count > 0 && count == total
            applyCheckAnimation(
                sb.ivSelectAllCheckmark,
                sb.viewSelectAllFrame,
                isSelectAllChecked,
                newChecked,
                R.drawable.ic_checkbox_checked_dark
            )
            isSelectAllChecked = newChecked
            sb.tvToolbarSelectedCount.text = "$count${getString(R.string.record_selected_count)}"
            toolbar.title = ""
        } else {
            selectionBinding?.let { toolbar.removeView(it.root) }
            selectionBinding = null
            isSelectAllChecked = false
            toolbar.title = getString(R.string.app_name)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun currentRecordFragment(): RecordFragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NavHostFragment
        return navHostFragment?.childFragmentManager?.primaryNavigationFragment as? RecordFragment
    }
}
