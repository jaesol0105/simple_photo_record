package com.beinny.android.dailylook

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MainActivity : AppCompatActivity(),DailyListFragment.Callbacks {
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

        //fab = findViewById(R.id.fab)
        //fab.setOnClickListener { view ->
        //    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null).show()
        //}

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dailyListFragment, R.id.backUpFragment
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        //if (currentFragment == null) {
        //    val fragment = DailyListFragment.newInstance()
        //    supportFragmentManager
        //        .beginTransaction()
        //        .add(R.id.fragment_container,fragment)
        //        .commit()
        //}
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.fragmentContainerView)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onSelected(dailyId: UUID) {
        val fragment = DailyFragment.newInstance(dailyId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerView,fragment)
            .addToBackStack(null) //인자=백스택의이름
            .commit()
    }
}