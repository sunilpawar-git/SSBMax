package com.example.ssbmax

import android.os.Bundle
import android.view.Menu
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.ssbmax.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_study_materials, R.id.nav_practice_tests,
                R.id.nav_progress, R.id.nav_tips, R.id.nav_oir_test, R.id.nav_ppdt_test,
                R.id.nav_psychology_test, R.id.nav_gto_test, R.id.nav_io_test
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Setup PIQ Form click listener
        setupPIQFormClickListener()
        
        // Setup custom menu item click listeners
        setupCustomMenuClickListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    private fun setupPIQFormClickListener() {
        val headerView = binding.navView.getHeaderView(0)
        val piqFormLink = headerView.findViewById<TextView>(R.id.piq_form_link)
        
        piqFormLink?.setOnClickListener {
            // Handle PIQ Form click
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("PIQ Form")
                .setMessage("Personal Information Questionnaire (PIQ) form will be opened. This form contains detailed questions about your background, family, education, and personal details.")
                .setPositiveButton("Open Form") { _, _ ->
                    // Navigate to PIQ form or open web link
                    Snackbar.make(binding.root, "PIQ Form opened", Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    private fun setupCustomMenuClickListeners() {
        val headerView = binding.navView.getHeaderView(0)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // OIR Test
        headerView.findViewById<LinearLayout>(R.id.nav_oir_test_layout)?.setOnClickListener {
            navController.navigate(R.id.nav_oir_test)
            binding.drawerLayout.closeDrawers()
        }
        
        // PPDT Test
        headerView.findViewById<LinearLayout>(R.id.nav_ppdt_test_layout)?.setOnClickListener {
            navController.navigate(R.id.nav_ppdt_test)
            binding.drawerLayout.closeDrawers()
        }
        
        // Psychology Test
        headerView.findViewById<LinearLayout>(R.id.nav_psychology_test_layout)?.setOnClickListener {
            navController.navigate(R.id.nav_psychology_test)
            binding.drawerLayout.closeDrawers()
        }
        
        // GTO Test
        headerView.findViewById<LinearLayout>(R.id.nav_gto_test_layout)?.setOnClickListener {
            navController.navigate(R.id.nav_gto_test)
            binding.drawerLayout.closeDrawers()
        }
        
        // IO Test
        headerView.findViewById<LinearLayout>(R.id.nav_io_test_layout)?.setOnClickListener {
            navController.navigate(R.id.nav_io_test)
            binding.drawerLayout.closeDrawers()
        }
        
        // Original Menu Items
        // Dashboard
        headerView.findViewById<LinearLayout>(R.id.nav_home_layout)?.setOnClickListener {
            navController.navigate(R.id.nav_home)
            binding.drawerLayout.closeDrawers()
        }
        
        // Study Materials
        headerView.findViewById<LinearLayout>(R.id.nav_study_materials_layout)?.setOnClickListener {
            navController.navigate(R.id.nav_study_materials)
            binding.drawerLayout.closeDrawers()
        }
        
        // Practice Tests
        headerView.findViewById<LinearLayout>(R.id.nav_practice_tests_layout)?.setOnClickListener {
            navController.navigate(R.id.nav_practice_tests)
            binding.drawerLayout.closeDrawers()
        }
        
        // Progress Tracker
        headerView.findViewById<LinearLayout>(R.id.nav_progress_layout)?.setOnClickListener {
            navController.navigate(R.id.nav_progress)
            binding.drawerLayout.closeDrawers()
        }
        
        // Tips & Tricks
        headerView.findViewById<LinearLayout>(R.id.nav_tips_layout)?.setOnClickListener {
            navController.navigate(R.id.nav_tips)
            binding.drawerLayout.closeDrawers()
        }
    }
}