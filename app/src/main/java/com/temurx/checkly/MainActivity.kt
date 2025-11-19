package com.temurx.checkly

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.FirebaseApp
import com.temurx.checkly.databinding.ActivityMainBinding
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseApp.initializeApp(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get NavController from NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Connect BottomNavigationView with NavController
        binding.bottomNav.setupWithNavController(navController)

        // Listen for destination changes to hide/show BottomNavigationView
        // and update selected menu item
        navController.addOnDestinationChangedListener { _, destination, _ ->

            // Hide bottom nav on login
            binding.bottomNav.visibility = if (destination.id == R.id.logInScreen) View.GONE else View.VISIBLE

            // Update BottomNavigationView selection
            when (destination.id) {
                R.id.homeFragment,
                R.id.taskDetails -> binding.bottomNav.menu.findItem(R.id.homeFragment).isChecked = true
                R.id.profileFragment -> binding.bottomNav.menu.findItem(R.id.profileFragment).isChecked = true
            }
        }

    }
}