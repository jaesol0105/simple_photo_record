package com.beinny.android.photorecord.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.beinny.android.photorecord.R
import com.beinny.android.photorecord.ui.recorddetail.RecordDetailFragment
import com.beinny.android.photorecord.ui.record.RecordFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.util.*

class MainActivity : AppCompatActivity(), RecordFragment.Callbacks {
    private lateinit var toolbar : Toolbar
    private lateinit var fab : FloatingActionButton
    private lateinit var navView : NavigationView
    private lateinit var drawerLayout : DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration

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
            setOf(
                R.id.recordFragment, R.id.dataMgntFragment
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        /*
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = DailyListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container,fragment)
                .commit()
        }
        */
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.fragment_container_view)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /** [RecordFragment에서 호출될 콜백 함수.] */
    override fun onSelected(recordId: UUID) {
        Log.d("adapteronclick","2")
        val fragment = RecordDetailFragment.newInstance(recordId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container_view,fragment)
            .addToBackStack(null) // 백스택을 추가하여 백버튼 동작시 RecordFragment로 복귀. 인자=백스택의이름
            .commit()
    }

    override fun onLongClick(longclick :Boolean, count:Int) {
        if (longclick) {
            toolbar.title = count.toString() + "개 선택됨"
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        } else {
            toolbar.title = getString(R.string.app_name)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }
}