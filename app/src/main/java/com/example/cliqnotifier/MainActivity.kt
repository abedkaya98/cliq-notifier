package com.example.cliqnotifier

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cliqnotifier.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Toast.makeText(this, "التطبيق يعمل بنجاح وبدون كراش! 🚀", Toast.LENGTH_LONG).show()

            binding.btnSave.setOnClickListener {
                Toast.makeText(this, "تم الضغط على الحفظ", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
