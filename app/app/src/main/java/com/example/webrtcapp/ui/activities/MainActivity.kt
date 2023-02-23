package com.example.webrtcapp.ui.activities

import android.Manifest.permission.CAMERA
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.webrtcapp.databinding.ActivityMainBinding
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.enterBtn.setOnClickListener{
            PermissionX.init(this)
                .permissions(
                    CAMERA
                ).request{ allGranted,_,_ ->
                    if(allGranted){
                        startActivity(
                            Intent(this, CallActivity::class.java)
                                .putExtra("username",binding.username.text.toString())
                        )
                    }else{
                        Toast.makeText(this,"You need to accept!!",Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}