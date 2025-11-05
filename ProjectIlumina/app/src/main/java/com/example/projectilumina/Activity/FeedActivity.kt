package com.example.projectilumina.Activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.projectilumina.databinding.ActivityFeedBinding


class FeedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun setupNavigationButtons() {
        binding.appBarHome.textActivity.text = "Mapa"
        binding.appBarHome.iconNotificacao.setOnClickListener {
            val intent = Intent(this, NotificacaoActivity::class.java)
            startActivity(intent)
        }
        binding.appBarHome.iconMenu.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        binding.endBar.iconDenuncia.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }

    }
}